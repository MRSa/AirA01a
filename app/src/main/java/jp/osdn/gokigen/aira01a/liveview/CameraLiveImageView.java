package jp.osdn.gokigen.aira01a.liveview;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.exifinterface.media.ExifInterface;
import jp.co.olympus.camerakit.OLYCamera;

/**
 *   CameraLiveImageView :
 *    (OLYMPUS の ImageCaptureSample そのまま)
 *
 */
public class CameraLiveImageView extends View implements CameraLiveViewListenerImpl.IImageDataReceiver, ISelfTimerDrawer
{
    private final String TAG = this.toString();

    public static final int GRID_FRAME_NONE = 0;
    public static final int GRID_FRAME_1 = 1;
    public static final int GRID_FRAME_2 = 2;
    public static final int GRID_FRAME_3 = 3;
    public static final int GRID_FRAME_4 = 4;
    public static final int GRID_FRAME_5 = 5;
    public static final int GRID_FRAME_6 = 6;
    public static final int GRID_FRAME_7 = 7;
    public static final int GRID_FRAME_8 = 8;
    public static final int GRID_FRAME_9 = 9;
    public static final int GRID_FRAME_A = 10;
    public static final int GRID_FRAME_B = 11;
    public static final int GRID_FRAME_C = 12;

    public enum FocusFrameStatus
    {
        Running,
        Focused,
        Failed,
        Unknown
    }

    private ImageView.ScaleType imageScaleType;
    private Bitmap imageBitmap;
    private int imageRotationDegrees;

    private boolean showingFocusFrame;
    private FocusFrameStatus focusFrameStatus;
    private int framingGridStatus;
    private RectF focusFrameRect;
    private Timer focusFrameHideTimer;

    private boolean showLevelGauge = false;
    private String orientation = "";
    private float roll = Float.NaN;
    private float pitch = Float.NaN;

    private float liveViewMagnify = 1.0f;

    private int selfTimerRemainSecond = 0;

    private IStoreImage imageStore = null;

    public CameraLiveImageView(Context context)
    {
        super(context);
        initComponent(context);
    }

    public CameraLiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initComponent(context);
    }

    public CameraLiveImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initComponent(context);
    }

    private void initComponent(Context context)
    {
        imageScaleType = ImageView.ScaleType.FIT_CENTER;
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            framingGridStatus = Integer.parseInt(preferences.getString("frame_grid","0"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            framingGridStatus = 0;
        }
        imageStore = new StoreImage(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        imageBitmap = null;
        if (focusFrameHideTimer != null) {
            focusFrameHideTimer.cancel();
            focusFrameHideTimer = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCanvas(canvas);
    }

    public float getIntrinsicContentSizeWidth() {
        if (imageBitmap == null) {
            return 0;
        }
        return imageBitmap.getWidth();
    }

    public float getIntrinsicContentSizeHeight() {
        if (imageBitmap == null) {
            return 0;
        }
        return imageBitmap.getHeight();
    }

    /**
     * Sets a image to view.
     * (CameraLiveViewListenerImpl.IImageDataReceiver の実装)
     *
     * @param data A image of live-view.
     * @param metadata A metadata of the image.
     */
    public void setImageData(byte[] data, Map<String, Object> metadata)
    {
        Bitmap bitmap;
        int rotationDegrees;

        if (data != null && metadata != null)
        {
            // Create a bitmap.
            try
            {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                return;
            }
            // Acquire a rotation degree of image.
            int orientation = ExifInterface.ORIENTATION_UNDEFINED;
            if (metadata.containsKey("Orientation"))
            {
                String orientationString = (String) metadata.get("Orientation");
                if (orientationString != null)
                {
                    orientation = Integer.parseInt(orientationString);
                }
                else
                {
                    orientation = ExifInterface.ORIENTATION_NORMAL;
                }
            }
            switch (orientation)
            {
                case ExifInterface.ORIENTATION_NORMAL:
                    rotationDegrees = 0;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270;
                    break;
                default:
                    rotationDegrees = 0;
                    break;
            }
            imageBitmap = bitmap;
            imageRotationDegrees = rotationDegrees;
        }

        refreshCanvas();
    }

    /**
     * Returns a point which is detected by a motion event.
     *
     * @param event A motion event.
     * @return A point in the view finder. if a point is equal to null, the point is out of the view finder.
     */
    public PointF getPointWithEvent(MotionEvent event) {
        if (event == null || imageBitmap == null) {
            return null;
        }

        PointF pointOnView = new PointF(event.getX(), event.getY());
        PointF pointOnImage = convertPointFromViewArea(pointOnView);
        float imageWidth;
        float imageHeight;
        if (imageRotationDegrees == 0 || imageRotationDegrees == 180) {
            imageWidth = imageBitmap.getWidth();
            imageHeight = imageBitmap.getHeight();
        } else {
            imageWidth = imageBitmap.getHeight();
            imageHeight = imageBitmap.getWidth();
        }
        return (OLYCamera.convertPointOnLiveImageIntoViewfinder(pointOnImage, imageWidth, imageHeight, imageRotationDegrees));
    }

    /**
     * Returns whether a image area contains a specified point.
     *
     * @param point The point to examine.
     * @return true if the image is not null or empty and the point is located within the rectangle; otherwise, false.
     */
    public boolean isContainsPoint(PointF point)
    {
       return ((point != null)&&(new RectF(0, 0, 1, 1)).contains(point.x, point.y));
    }

    /**
     * Hides the forcus frame.
     */
    public void hideFocusFrame() {
        if (focusFrameHideTimer != null) {
            focusFrameHideTimer.cancel();
            focusFrameHideTimer = null;
        }

        showingFocusFrame = false;

        refreshCanvas();
    }

    /**
     * Shows the focus frame.
     *
     * @param rect A rectangle of the focus frame on view area.
     * @param status A status of the focus frame.
     */
    public void showFocusFrame(RectF rect, FocusFrameStatus status) {
        showFocusFrame(rect, status, 0);
    }

    /**
     * Shows the focus frame.
     *
     * @param rect A rectangle of the focus frame on view area.
     * @param status A status of the focus frame.
     * @param duration A duration of the focus frame showing.
     */
    public void showFocusFrame(RectF rect, FocusFrameStatus status, double duration) {
        if (focusFrameHideTimer != null) {
            focusFrameHideTimer.cancel();
            focusFrameHideTimer = null;
        }

        showingFocusFrame = true;
        focusFrameStatus = status;
        focusFrameRect = rect;

        refreshCanvas();

        if (duration > 0) {
            focusFrameHideTimer = new Timer();
            focusFrameHideTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    hideFocusFrame();
                }
            }, (long)(duration * 1000));
        }
    }

    private void refreshCanvas() {
        if(Looper.getMainLooper().getThread() == Thread.currentThread()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    private void drawCanvas(Canvas canvas)
    {
        // Clears the canvas.
        canvas.drawARGB(255, 0, 0, 0);

        if (imageBitmap == null)
        {
            // 表示するビットマップがないときは、すぐに折り返す
            return;
        }

        // Rotates the image.
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        canvas.rotate(imageRotationDegrees, centerX, centerY);

        RectF viewRect = null;
        Rect imageRect;
        //  Calculate the viewport of bitmap.
        if (imageScaleType == ImageView.ScaleType.FIT_CENTER)
        {
            final int srcWidth;
            final int srcHeight;
            if ((imageRotationDegrees == 0) || (imageRotationDegrees == 180))
            {
                srcWidth = imageBitmap.getWidth();
                srcHeight =imageBitmap.getHeight();
            }
            else
            {
                // Replaces width and height.
                srcWidth = imageBitmap.getHeight();
                srcHeight = imageBitmap.getWidth();
            }
            final int dstWidth;
            final int dstHeight;
            {
                int maxWidth = canvas.getWidth();
                int maxHeight = canvas.getHeight();
                float widthRatio = maxWidth / (float)srcWidth;
                float heightRatio = maxHeight / (float)srcHeight;
                float smallRatio = Math.min(widthRatio, heightRatio);

                if (widthRatio < heightRatio)
                {
                    // Fits to maxWidth with keeping aspect ratio.
                    dstWidth = maxWidth;
                    dstHeight = (int)(smallRatio * srcHeight);
                }
                else
                {
                    // Fits to maxHeight with keeping aspect ratio.
                    dstHeight = maxHeight;
                    dstWidth = (int)(smallRatio * srcWidth);
                }
            }
            final float halfWidth = dstWidth * 0.5f;
            final float halfHeight = dstHeight * 0.5f;
            if ((imageRotationDegrees == 0) || (imageRotationDegrees == 180))
            {
                viewRect = new RectF(
                        centerX - halfWidth,
                        centerY - halfHeight,
                        centerX - halfWidth + dstWidth,
                        centerY - halfHeight + dstHeight);
            }
            else
            {
                // Replaces the width and height.
                viewRect = new RectF(
                        centerX - halfHeight,
                        centerY - halfWidth,
                        centerX - halfHeight + dstHeight,
                        centerY - halfWidth + dstWidth);
            }

            // Draws the bitmap.
            imageRect = new Rect(0, 0, imageBitmap.getWidth(), imageBitmap.getHeight());
            canvas.drawBitmap(imageBitmap, imageRect, viewRect, null);

        }
        else
        {
            // Sorry, other scale types are not supported.
            Log.v(TAG, "Sorry, other scale types are not supported. " + imageScaleType);
        }

        // Cancels rotation of the canvas.
        canvas.rotate(-imageRotationDegrees, centerX, centerY);

        float imageWidth;
        float imageHeight;
        if (imageRotationDegrees == 0 || imageRotationDegrees == 180)
        {
            imageWidth = imageBitmap.getWidth();
            imageHeight = imageBitmap.getHeight();
        }
        else
        {
            imageWidth = imageBitmap.getHeight();
            imageHeight = imageBitmap.getWidth();
        }

        if ((focusFrameRect != null)&&(showingFocusFrame))
        {
            //  Calculate the rectangle of focus.
            RectF focusRectOnImage = OLYCamera.convertRectOnViewfinderIntoLiveImage(focusFrameRect, imageWidth, imageHeight, imageRotationDegrees);
            RectF focusRectOnView = convertRectFromImageArea(focusRectOnImage);

            // Draw a rectangle to the canvas.
            Paint focusFramePaint = new Paint();
            focusFramePaint.setStyle(Paint.Style.STROKE);
            switch (focusFrameStatus)
            {
                case Running:
                    focusFramePaint.setColor(Color.WHITE);
                    break;

                case Focused:
                    focusFramePaint.setColor(Color.GREEN);
                    break;

                case Failed:
                    focusFramePaint.setColor(Color.RED);
                    break;
                case Unknown:
                    focusFramePaint.setColor(Color.YELLOW);
                    break;
            }
            float focusFrameStrokeWidth = 2.0f;
            DisplayMetrics dm = getResources().getDisplayMetrics();
            float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, focusFrameStrokeWidth, dm);
            focusFramePaint.setStrokeWidth(strokeWidth);
            canvas.drawRect(focusRectOnView, focusFramePaint);
        }
        if (viewRect == null)
        {
            return;
        }

        // ライブビューの拡大表示マーカー
        if (liveViewMagnify > 1.0f)
        {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(16);
            paint.setAntiAlias(true);
            paint.setShadowLayer(5.0f, 3.0f, 3.0f, Color.BLACK);  // これで文字に影をつける
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            canvas.drawText("x" + liveViewMagnify, viewRect.left + 3.0f, viewRect.bottom - fontMetrics.bottom, paint);
        }

        // セルフタイマーの残り秒数を表示する
        if (selfTimerRemainSecond > 0)
        {
            Paint paint = new Paint();
            if (selfTimerRemainSecond >= 3)
            {
                paint.setColor(Color.WHITE);
            }
            else
            {
                paint.setColor(Color.YELLOW);
            }
            paint.setTextSize(96);
            paint.setAntiAlias(true);
            paint.setShadowLayer(5.0f, 3.0f, 3.0f, Color.BLACK);  // これで文字に影をつける
            String remainText = " " + selfTimerRemainSecond + " ";
            float textWidth = paint.measureText(remainText) / 2.0f;
            //Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            //float textHeight = (fontMetrics.descent - fontMetrics.ascent) / 2.0f;
            canvas.drawText(remainText, viewRect.centerX() - textWidth, viewRect.centerY(), paint);
        }

        // グリッド（撮影補助線）の表示
        if (framingGridStatus == GRID_FRAME_NONE)
        {
            return;
        }
        RectF gridRect;
        if ((imageRotationDegrees == 0) || (imageRotationDegrees == 180))
        {
            gridRect = new RectF(viewRect);
        }
        else
        {
            float height = viewRect.right - viewRect.left;
            float width = viewRect.bottom - viewRect.top;
            float left = (canvas.getWidth() / 2.0f) - (width / 2.0f);
            float top =  (canvas.getHeight() / 2.0f) - (height / 2.0f);
            gridRect = new RectF(left, top, left + width, top + height);
        }
        Paint framePaint = new Paint();
        framePaint.setStyle(Paint.Style.STROKE);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, dm);
        framePaint.setStrokeWidth(strokeWidth);

        switch (framingGridStatus)
        {
            case GRID_FRAME_1:
                framePaint.setColor(Color.argb(130,15,15,15));
                drawFramingGridType0(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_2:
                framePaint.setColor(Color.argb(130,15,15,15));
                drawFramingGridType1(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_3:
                framePaint.setColor(Color.argb(130,15,15,15));
                drawFramingGridType2(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_4:
                framePaint.setColor(Color.argb(130,15,15,15));
                drawFramingGridType3(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_5:
                framePaint.setColor(Color.argb(130,15,15,15));
                drawFramingGridType4(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_6:
                framePaint.setColor(Color.argb(130,15,15,15));
                drawFramingGridType5(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_7:
                framePaint.setColor(Color.argb(130,235,235,235));
                drawFramingGridType0(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_8:
                framePaint.setColor(Color.argb(130,235,235,235));
                drawFramingGridType1(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_9:
                framePaint.setColor(Color.argb(130,235,235,235));
                drawFramingGridType2(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_A:
                framePaint.setColor(Color.argb(130,235,235,235));
                drawFramingGridType3(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_B:
                framePaint.setColor(Color.argb(130,235,235,235));
                drawFramingGridType4(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_C:
                framePaint.setColor(Color.argb(130,235,235,235));
                drawFramingGridType5(canvas, gridRect, framePaint);
                break;
            case GRID_FRAME_NONE:
            default:
                // フレームグリッドは何も表示しない
                break;
        }

        if ((showLevelGauge)&&(!Float.isNaN(roll))&&(!Float.isNaN(pitch)))
        {
            // デジタル水準器の表示を行う
            drawLevelGauge(canvas, roll, pitch);
        }
    }

    /**
     *   4x3 のグリッド表示
     *
     */
    private void drawFramingGridType0(Canvas canvas, RectF rect, Paint paint)
    {
        float w = (rect.right - rect.left) / 4.0f;
        float h = (rect.bottom - rect.top) / 3.0f;

        canvas.drawLine(rect.left + w, rect.top, rect.left + w, rect.bottom, paint);
        canvas.drawLine(rect.left + 2.0f * w, rect.top, rect.left + 2.0f * w, rect.bottom, paint);
        canvas.drawLine(rect.left + 3.0f * w, rect.top, rect.left + 3.0f * w, rect.bottom, paint);
        canvas.drawLine(rect.left, rect.top + h, rect.right, rect.top + h, paint);
        canvas.drawLine(rect.left, rect.top + 2.0f * h, rect.right, rect.top + 2.0f * h, paint);
        canvas.drawRect(rect, paint);
    }


    /**
     *    3x3 のグリッド表示
     *
     */
    private void drawFramingGridType1(Canvas canvas, RectF rect, Paint paint)
    {
        float w = (rect.right - rect.left) / 3.0f;
        float h = (rect.bottom - rect.top) / 3.0f;

        canvas.drawLine(rect.left + w, rect.top, rect.left + w, rect.bottom, paint);
        canvas.drawLine(rect.left + 2.0f * w, rect.top, rect.left + 2.0f * w, rect.bottom, paint);
        canvas.drawLine(rect.left, rect.top + h, rect.right, rect.top + h, paint);
        canvas.drawLine(rect.left, rect.top + 2.0f * h, rect.right, rect.top + 2.0f * h, paint);
        canvas.drawRect(rect, paint);
    }

    /**
     *   4x4 のグリッド表示
     *
     */
    private void drawFramingGridType2(Canvas canvas, RectF rect, Paint paint)
    {
        float w = (rect.right - rect.left) / 4.0f;
        float h = (rect.bottom - rect.top) / 4.0f;

        canvas.drawLine(rect.left + w, rect.top, rect.left + w, rect.bottom, paint);
        canvas.drawLine(rect.left + 2.0f * w, rect.top, rect.left + 2.0f * w, rect.bottom, paint);
        canvas.drawLine(rect.left + 3.0f * w, rect.top, rect.left + 3.0f * w, rect.bottom, paint);
        canvas.drawLine(rect.left, rect.top + h, rect.right, rect.top + h, paint);
        canvas.drawLine(rect.left, rect.top + 2.0f * h, rect.right, rect.top + 2.0f * h, paint);
        canvas.drawLine(rect.left, rect.top + 3.0f * h, rect.right, rect.top + 3.0f * h, paint);
        canvas.drawRect(rect, paint);
    }

    /**
     *   4x4 のグリッドと対角線の表示
     *
     */
    private void drawFramingGridType3(Canvas canvas, RectF rect, Paint paint)
    {
        float w = (rect.right - rect.left) / 4.0f;
        float h = (rect.bottom - rect.top) / 4.0f;

        canvas.drawLine(rect.left + w, rect.top, rect.left + w, rect.bottom, paint);
        canvas.drawLine(rect.left + 2.0f * w, rect.top, rect.left + 2.0f * w, rect.bottom, paint);
        canvas.drawLine(rect.left + 3.0f * w, rect.top, rect.left + 3.0f * w, rect.bottom, paint);
        canvas.drawLine(rect.left, rect.top + h, rect.right, rect.top + h, paint);
        canvas.drawLine(rect.left, rect.top + 2.0f * h, rect.right, rect.top + 2.0f * h, paint);
        canvas.drawLine(rect.left, rect.top + 3.0f * h, rect.right, rect.top + 3.0f * h, paint);

        canvas.drawLine(rect.left, rect.top, rect.right, rect.bottom, paint);
        canvas.drawLine(rect.left, rect.bottom, rect.right, rect.top, paint);

        canvas.drawRect(rect, paint);
    }
    /**
     *   3x3 のグリッドと対角線の表示
     *
     */
    private void drawFramingGridType4(Canvas canvas, RectF rect, Paint paint)
    {
        float cX = (rect.right + rect.left) / 2.0f;
        float cY = (rect.bottom + rect.top) / 2.0f;

        canvas.drawLine(rect.left, cY, rect.right, cY, paint);
        canvas.drawLine(cX, rect.bottom, cX, rect.top, paint);

        canvas.drawRect(rect, paint);
    }

    /**
     *   真ん中四角と中心線の表示
     *
     */
    private void drawFramingGridType5(Canvas canvas, RectF rect, Paint paint)
    {
        float cX = (rect.right + rect.left) / 2.0f;
        float cY = (rect.bottom + rect.top) / 2.0f;
        float w = (rect.right - rect.left) / 4.0f;
        float h = (rect.bottom - rect.top) / 4.0f;

        canvas.drawRect(rect.left + w, rect.top + h, rect.right - w, rect.bottom - h, paint);

        canvas.drawLine(rect.left, cY, rect.left + w, cY, paint);
        canvas.drawLine(rect.right - w, cY, rect.right, cY, paint);

        canvas.drawLine(cX, rect.top, cX, rect.top + h, paint);
        canvas.drawLine(cX, rect.bottom - h, cX, rect.bottom, paint);
    }

    /**
     * Converts a point on image area to a point on view area.
     *
     * @param point A point on image area. (e.g. a live preview image)
     * @return A point on view area. (e.g. a touch panel view)
     */
    private PointF convertPointFromImageArea(PointF point) {
        if (imageBitmap == null) {
            return new PointF();
        }

        float viewPointX = point.x;
        float viewPointY = point.y;
        float imageSizeWidth;
        float imageSizeHeight;
        if (imageRotationDegrees == 0 || imageRotationDegrees == 180) {
            imageSizeWidth = imageBitmap.getWidth();
            imageSizeHeight = imageBitmap.getHeight();
        } else {
            imageSizeWidth = imageBitmap.getHeight();
            imageSizeHeight = imageBitmap.getWidth();
        }
        float viewSizeWidth = this.getWidth();
        float viewSizeHeight = this.getHeight();
        float ratioX = viewSizeWidth / imageSizeWidth;
        float ratioY = viewSizeHeight / imageSizeHeight;
        float scale;

        switch (imageScaleType) {
            case FIT_XY:
                viewPointX *= ratioX;
                viewPointY *= ratioY;
                break;
            case FIT_CENTER:	// go to next label.
            case CENTER_INSIDE:
                scale = Math.min(ratioX, ratioY);
                viewPointX *= scale;
                viewPointY *= scale;
                viewPointX += (viewSizeWidth  - imageSizeWidth  * scale) / 2.0f;
                viewPointY += (viewSizeHeight - imageSizeHeight * scale) / 2.0f;
                break;
            case CENTER_CROP:
                scale = Math.max(ratioX, ratioY);
                viewPointX *= scale;
                viewPointY *= scale;
                viewPointX += (viewSizeWidth  - imageSizeWidth  * scale) / 2.0f;
                viewPointY += (viewSizeHeight - imageSizeHeight * scale) / 2.0f;
                break;
            case CENTER:
                viewPointX += viewSizeWidth / 2.0  - imageSizeWidth  / 2.0f;
                viewPointY += viewSizeHeight / 2.0 - imageSizeHeight / 2.0f;
                break;
            default:
                break;
        }

        return new PointF(viewPointX, viewPointY);
    }

    /**
     * Converts a point on view area to a point on image area.
     *
     * @param point A point on view area. (e.g. a touch panel view)
     * @return A point on image area. (e.g. a live preview image)
     */
    private PointF convertPointFromViewArea(PointF point)
    {
        if (imageBitmap == null)
        {
            return new PointF();
        }

        float imagePointX = point.x;
        float imagePointY = point.y;
        float imageSizeWidth;
        float imageSizeHeight;
        if (imageRotationDegrees == 0 || imageRotationDegrees == 180) {
            imageSizeWidth = imageBitmap.getWidth();
            imageSizeHeight = imageBitmap.getHeight();
        } else {
            imageSizeWidth = imageBitmap.getHeight();
            imageSizeHeight = imageBitmap.getWidth();
        }
        float viewSizeWidth = this.getWidth();
        float viewSizeHeight = this.getHeight();
        float ratioX = viewSizeWidth / imageSizeWidth;
        float ratioY = viewSizeHeight / imageSizeHeight;
        float scale;

        switch (imageScaleType) {
            case FIT_XY:
                imagePointX /= ratioX;
                imagePointY /= ratioY;
                break;
            case FIT_CENTER:	// go to next label.
            case CENTER_INSIDE:
                scale = Math.min(ratioX, ratioY);
                imagePointX -= (viewSizeWidth  - imageSizeWidth  * scale) / 2.0f;
                imagePointY -= (viewSizeHeight - imageSizeHeight * scale) / 2.0f;
                imagePointX /= scale;
                imagePointY /= scale;
                break;
            case CENTER_CROP:
                scale = Math.max(ratioX, ratioY);
                imagePointX -= (viewSizeWidth  - imageSizeWidth  * scale) / 2.0f;
                imagePointY -= (viewSizeHeight - imageSizeHeight * scale) / 2.0f;
                imagePointX /= scale;
                imagePointY /= scale;
                break;
            case CENTER:
                imagePointX -= (viewSizeWidth - imageSizeWidth)  / 2.0f;
                imagePointY -= (viewSizeHeight - imageSizeHeight) / 2.0f;
                break;
            default:
                break;
        }

        return new PointF(imagePointX, imagePointY);
    }

    /**
     * Converts a rectangle on image area to a rectangle on view area.
     *
     * @param rect A rectangle on image area. (e.g. a live preview image)
     * @return A rectangle on view area. (e.g. a touch panel view)
     */
    private RectF convertRectFromImageArea(RectF rect)
    {
        if (imageBitmap == null)
        {
            return new RectF();
        }

        PointF imageTopLeft =  new PointF(rect.left, rect.top);
        PointF imageBottomRight = new PointF(rect.right, rect.bottom);

        PointF viewTopLeft = convertPointFromImageArea(imageTopLeft);
        PointF viewBottomRight = convertPointFromImageArea(imageBottomRight);

        return (new RectF(viewTopLeft.x, viewTopLeft.y, viewBottomRight.x, viewBottomRight.y));
    }

    /**
     * Converts a rectangle on view area to a rectangle on image area.
     *
     * @param rect A rectangle on view area. (e.g. a touch panel view)
     * @return A rectangle on image area. (e.g. a live preview image)
     */
    @SuppressWarnings("unused")
    private RectF convertRectFromViewArea(RectF rect)
    {
        if (imageBitmap == null)
        {
            return new RectF();
        }

        PointF viewTopLeft = new PointF(rect.left, rect.top);
        PointF viewBottomRight = new PointF(rect.right, rect.bottom);

        PointF imageTopLeft = convertPointFromViewArea(viewTopLeft);
        PointF imageBottomRight = convertPointFromViewArea(viewBottomRight);

        return (new RectF(imageTopLeft.x, imageTopLeft.y, imageBottomRight.x, imageBottomRight.y));
    }

    /**
     *   デジタル水準器の使用・未使用設定
     *
     */
    public void updateLevelGauge(boolean hasLevelGauge)
    {
        showLevelGauge = hasLevelGauge;
    }

    /**
     *   デジタル水準器の表示を行う
     *
     */
    private void drawLevelGauge(Canvas canvas, float horizontalValue, float verticalValue)
    {
        // レベルゲージの表示位置
        int height = canvas.getHeight();
        int width = canvas.getWidth();
        int centerX = width / 2;
        int centerY = height / 2;

        float maxBandWidth = width / 3.0f;     // ゲージの最大長 (画面の 1/3 ぐらい)
        float maxBandHeight = height / 3.0f;   // ゲージの最大長 (画面の 1/3 ぐらい)
        int barWidthInitial = 7;               // 表示するゲージの幅（の初期値）
        int barWidth;                          // 実際に表示するゲージの幅

        Paint paint = new Paint();

        // 垂直線
        float verticalSize = verticalValue / 60.0f * maxBandHeight;  // 45度で切り替わるはずだが、一応...
        if (Math.abs(verticalSize) < 1.0f)
        {
            // 線引き限界以下、水平検出とする (この時の線は倍の長さにする)
            verticalSize = 1.0f;
            barWidth = barWidthInitial * 2;
        }
        else
        {
            barWidth = barWidthInitial;
        }
        paint.setStrokeWidth(barWidth);
        paint.setColor(getLevelColor(verticalValue));
        canvas.drawLine((width - barWidth), centerY, (width - barWidth), (centerY + verticalSize), paint);

        // 水平線
        float horizontalSize = horizontalValue / 60.0f * maxBandWidth;  // 45度ぐらいで切り替わるはずだが、一応...
        if (Math.abs(horizontalSize) < 1.0f)
        {
            // 線引き限界以下、水平検出とする (この時の線は倍の長さにする）
            horizontalSize = 1.0f;
            barWidth = barWidthInitial * 2;
        }
        else
        {
            barWidth = barWidthInitial;
        }
        paint.setStrokeWidth(barWidth);
        paint.setColor(getLevelColor(horizontalValue));
        canvas.drawLine(centerX, (height - barWidth), (centerX + horizontalSize),  (height - barWidth), paint);
    }

    /**
     *   デジタル水準器の情報をもらう
     *
     */
    public void updateLevelGauge(String orientation, float roll, float pitch)
    {
        this.orientation = orientation;
        this.roll = roll;
        this.pitch = pitch;
    }

    /**
     *   デジタル水準器の色を決める
     *
     */
    private int getLevelColor(float value)
    {
        final float LEVELGAUGE_THRESHOLD_MIDDLE = 2.0f;
        final float LEVELGAUGE_THRESHOLD_OVER = 15.0f;
        value = Math.abs(value);

        if (value < LEVELGAUGE_THRESHOLD_MIDDLE)
        {
            return (Color.GREEN);
        }
        if (value > LEVELGAUGE_THRESHOLD_OVER)
        {
            return (Color.RED);
        }
        return (Color.YELLOW);
    }

    /**
     *   ライブビュー画像を保管する
     *
     * @param isShare 画像保管後、共有するか？ (trueなら共有する)
     */
    public void storeImage(boolean isShare)
    {
        if ((imageStore != null)&&(imageBitmap != null))
        {
            imageStore.doStore(imageBitmap, null, isShare);
        }
    }

    /**
     *   ライブビューの表示中スケールを表示する
     *
     * @param scale 拡大サイズ
     */
    public void updateLiveViewScale(float scale)
    {
        liveViewMagnify = scale;
    }

    /**
     *   imageStore用...
     *
     */
    public void setActivity(Activity activity)
    {
        if (imageStore != null)
        {
            imageStore.setActivity(activity);
        }
    }

    /**
     *   セルフタイマーの秒数を設定する
     *
     */
    @Override
    public void setTimerCount(int remainSeconds)
    {
        Log.v(TAG, "  setTimerCount: " + remainSeconds);
        selfTimerRemainSecond = remainSeconds;
        try
        {
            refreshCanvas();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
