package jp.osdn.gokigen.aira01a.playback;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

/**
 *  イメージを表示する ... ImageViewerSampleから持ってくる
 *
 */
public class ScalableImageView extends AppCompatImageView
{

    private static enum GestureMode {
        None,
        Move,
        Zoom,
    }

    private Context mContext;
    private GestureDetector mDoubleTapDetector;
    private GestureMode mGestureMode;

    /** The affine transformation matrix. */
    private Matrix mMatrix;

    /** The horizontal moving factor after scaling. */
    private float mMoveX;
    /** The vertical moving factor after scaling. */
    private float mMoveY;
    /** The X-coordinate origin for calculating the amount of movement. */
    private float mMovingBaseX;
    /** The Y-coordinate origin for calculating the amount of movement. */
    private float mMovingBaseY;

    /** The scaling factor. */
    private float mScale;
    /** The minimum value of scaling factor. */
    private float mScaleMin;
    /** The maximum value of scaling factor. */
    private float mScaleMax;
    /** The distance from the center for determining the amount of scaling. */
    private float mScalingBaseDistance;
    /** The center X-coordinate to determine the amount of scaling. */
    private float mScalingCenterX;
    /** The center Y-coordinate to determine the amount of scaling. */
    private float mScalingCenterY;

    /** The width of the view. */
    private int mViewWidth;
    /** The height of the view. */
    private int mViewHeight;
    /** The width of the image. */
    private int mImageWidth;
    /** The height of the image. */
    private int mImageHeight;


    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        reset();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        reset();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        reset();
    }


    /**
     * Constructs a new CapturedImageView.
     *
     */
    public ScalableImageView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    /**
     * Constructs a new CapturedImageView.
     *
     */
    public ScalableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    /**
     * Constructs a new CapturedImageView.
     *
     */
    public ScalableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    /**
     * Initializes this instance.
     */
    private void init() {
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        this.setScaleType(ScaleType.MATRIX);
        mMatrix = new Matrix();
        mViewWidth = 0;
        mViewHeight = 0;
        mImageWidth = 0;
        mImageHeight = 0;
        mGestureMode = GestureMode.None;
        mMoveX = 0;
        mMoveY = 0;
        mScale = 1.f;
        mScaleMin = 1.f;
        mScaleMax = 4.f;

        // Setups touch gesture.
        mDoubleTapDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mScale != 1.0f) {
                    // Zooms at tapped point.
                    updateScaleWithBasePoint(1.0f, e.getX(), e.getY());
                } else {
                    // Zooms out.
                    fitScreen();
                }
                updateMatrix();
                return true;
            }
        });
    }

    /**
     * Resets current scaling.
     */
    private void reset() {
        Drawable drawable = this.getDrawable();
        if (drawable != null) {
            mImageWidth = drawable.getIntrinsicWidth();
            mImageHeight = drawable.getIntrinsicHeight();
            fitScreen();
            updateMatrix();
        }
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        mViewWidth = r - l;
        mViewHeight = b - t;
        if (this.getDrawable() != null) {
            fitScreen();
        }
        updateMatrix();
        return super.setFrame(l, t, r, b);
    }

    /**
     * Returns a scaled X offset.
     *
     * @param scale A scaling factor.
     * @param moveX A horizontal moving factor.
     * @return A scaled X offset.
     */
    private float computeOffsetX(float scale, float moveX) {
        // Offsets in order to center the image.
        float scaledWidth = scale * mImageWidth;
        float offsetX = (mViewWidth - scaledWidth) / 2;
        // Moves specified offset.
        offsetX += moveX;
        return offsetX;
    }

    /**
     * Returns a scaled Y offset.
     *
     * @param scale A scaling factor.
     * @param moveY A vertical moving factor.
     * @return A scaled Y offset.
     */
    private float computeOffsetY(float scale, float moveY) {
        // Offsets in order to center the image.
        float scaledHeight = scale * mImageHeight;
        float offsetY = (mViewHeight - scaledHeight) / 2;
        // Moves specified offset.
        offsetY += moveY;
        return offsetY;
    }

    /**
     * Updates affine transformation matrix to display the image.
     */
    private void updateMatrix() {
        // Creates new matrix.
        mMatrix.reset();
        mMatrix.postScale(mScale, mScale);
        mMatrix.postTranslate(computeOffsetX(mScale, mMoveX), computeOffsetY(mScale, mMoveY));

        // Updates the matrix.
        this.setImageMatrix(mMatrix);
        this.invalidate();
    }

    /**
     * Calculates zoom scale. (for the image size to fit screen size).
     */
    private void fitScreen() {
        if ((mImageWidth == 0) || (mImageHeight == 0) || (mViewWidth == 0) || (mViewHeight == 0)) {
            return;
        }

        // Clears the moving factors.
        updateMove(0, 0);

        // Gets scaling ratio.
        float scaleX = (float)mViewWidth / mImageWidth;
        float scaleY = (float)mViewHeight / mImageHeight;

        // Updates the scaling factor so that the image will not be larger than the screen size.
        mScale = Math.min(scaleX, scaleY);
        mScaleMin = mScale;
        // 4 times of original image size or 4 times of the screen size.
        mScaleMax = Math.max(4.f, mScale * 4);
    }

    /**
     * Updates the moving factors.
     *
     * @param moveX A horizontal moving factor.
     * @param moveY A vertical moving factor.
     */
    protected void updateMove(float moveX, float moveY) {
        mMoveX = moveX;
        mMoveY = moveY;

        // Gets scaled size.
        float scaledWidth = mImageWidth * mScale;
        float scaledHeight = mImageHeight * mScale;

        // Clips the moving factors.
        if (scaledWidth <= mViewWidth) {
            mMoveX = 0;
        } else {
            float minMoveX = -(scaledWidth - mViewWidth) / 2;
            float maxMoveX = +(scaledWidth - mViewWidth) / 2;
            mMoveX = Math.min(Math.max(mMoveX, minMoveX), maxMoveX);
        }
        if (scaledHeight <= mViewHeight) {
            mMoveY = 0;
        } else {
            float minMoveY = -(scaledHeight - mViewHeight) / 2;
            float maxMoveY = +(scaledHeight - mViewHeight) / 2;
            mMoveY = Math.min(Math.max(mMoveY, minMoveY), maxMoveY);
        }
    }

    /**
     * Updates the scaling factor. The specified point doesn't change in appearance.
     *
     * @param newScale The new scaling factor.
     * @param baseX The center position of scaling.
     * @param baseY The center position of scaling.
     */
    protected void updateScaleWithBasePoint(float newScale, float baseX, float baseY) {
        float lastScale = mScale;
        float lastOffsetX = computeOffsetX(mScale, mMoveX);
        float lastOffsetY = computeOffsetY(mScale, mMoveY);

        // Updates the scale with clipping.
        mScale = Math.min(Math.max(newScale, mScaleMin), mScaleMax);
        mScalingCenterX = baseX;
        mScalingCenterY = baseY;

        // Gets scaling base point on the image world.
        float scalingCenterXOnImage = (mScalingCenterX - lastOffsetX) / lastScale;
        float scalingCenterYOnImage = (mScalingCenterY - lastOffsetY) / lastScale;
        // Gets scaling base point on the scaled image world.
        float scalingCenterXOnScaledImage = scalingCenterXOnImage * mScale;
        float scalingCenterYOnScaledImage = scalingCenterYOnImage * mScale;
        // Gets scaling base point on the view world.
        float scalingCenterXOnView = computeOffsetX(mScale, 0) + scalingCenterXOnScaledImage;
        float scalingCenterYOnView = computeOffsetY(mScale, 0) + scalingCenterYOnScaledImage;

        // Updates moving.
        updateMove(mScalingCenterX - scalingCenterXOnView, mScalingCenterY - scalingCenterYOnView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDoubleTapDetector.onTouchEvent(event)) {
            return true;
        }

        int action = event.getAction() & MotionEvent.ACTION_MASK;
        int touchCount = event.getPointerCount();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mScale > mScaleMin) {
                    // Starts to move the image and takes in the start point.
                    mGestureMode = GestureMode.Move;
                    mMovingBaseX = event.getX();
                    mMovingBaseY = event.getY();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (touchCount >= 2) {
                    // Starts zooming and takes in the center point.
                    mGestureMode = GestureMode.Zoom;
                    float distance = (float)Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
                    mScalingBaseDistance = distance;
                    mScalingCenterX = (event.getX(0) + event.getX(1)) / 2;
                    mScalingCenterY = (event.getY(0) + event.getY(1)) / 2;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mGestureMode == GestureMode.Move) {
                    // Moves the image and updates the start point.
                    float moveX = event.getX() - mMovingBaseX;
                    float moveY = event.getY() - mMovingBaseY;
                    mMovingBaseX = event.getX();
                    mMovingBaseY = event.getY();
                    updateMove(mMoveX + moveX, mMoveY + moveY);
                    updateMatrix();
                } else if ((mGestureMode == GestureMode.Zoom) && (touchCount >= 2)) {
                    // Zooms the image and updates the distance from the center point.
                    float distance = (float)Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
                    float scale = distance / mScalingBaseDistance;
                    mScalingBaseDistance = distance;
                    updateScaleWithBasePoint(mScale * scale, mScalingCenterX, mScalingCenterY);
                    updateMatrix();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                // Finishes all gestures.
                mGestureMode = GestureMode.None;
                break;
        }
        return true;
    }

    // The content in view can scroll to horizontal.
    public boolean canHorizontalScroll()
    {
        if (mScale == mScaleMin) {
            return false;
        }
        if (mGestureMode == GestureMode.None) {
            return false;
        }

        // TODO: Please improve UX.
        // If the view rectangle is touching to the edge of the image, the view cannot be scrolled.

        return true;
    }
}
