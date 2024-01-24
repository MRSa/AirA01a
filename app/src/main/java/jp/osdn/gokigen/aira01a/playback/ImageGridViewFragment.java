package jp.osdn.gokigen.aira01a.playback;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraFileInfo;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.co.olympus.camerakit.OLYCamera.ProgressEvent;
import jp.osdn.gokigen.aira01a.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.preference.PreferenceManager;
import android.os.Bundle;

import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.google.android.material.snackbar.Snackbar;

public class ImageGridViewFragment extends Fragment  implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener, MultiFileDownloadConfirmationDialog.DownloadConfirmationCallback, MultiFileDeleteConfirmationDialog.DeleteConfirmationCallback, ImageDownloader.Callback
{
    private final String TAG = this.toString();
    private final String MOVIE_SUFFIX = ".mov";
    //private final String JPEG_SUFFIX = ".jpg";
    private final String RAW_SUFFIX = ".ORF";

    private GridView gridView;
    private boolean gridViewIsScrolling;

    private OLYCamera camera = null;
    private final OlyCameraContentListHolder contentListHolder = new OlyCameraContentListHolder();

    private ExecutorService executor;
    private LruCache<String, Bitmap> imageCache;

    public void setInterface(OLYCamera camera)
    {
        this.camera = camera;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "ImageGridViewFragment::onCreate()");

        executor = Executors.newFixedThreadPool(1);
        imageCache = new LruCache<>(120);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.v(TAG, "ImageGridViewFragment::onCreateView()");
        View view = inflater.inflate(R.layout.fragment_image_grid_view, container, false);

        GridViewOnItemClickListener listener = new GridViewOnItemClickListener();
        gridView = view.findViewById(R.id.gridView1);
        gridView.setAdapter(new GridViewAdapter(inflater));
        gridView.setOnItemClickListener(listener);
        gridView.setOnItemLongClickListener(listener);
        gridView.setOnScrollListener(new GridViewOnScrollListener());

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.image_grid_view, menu);

        String title = getString(R.string.app_name);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null) {
                bar.setTitle(title);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_refresh)
        {
            refresh(true);
            return true;
        } else if (id == R.id.action_info) {
            String cameraVersion;
            try
            {
                Map<String, Object> hardwareInformation = camera.inquireHardwareInformation();
                cameraVersion = (String) hardwareInformation.get(OLYCamera.HARDWARE_INFORMATION_CAMERA_FIRMWARE_VERSION_KEY);
            }
            catch (OLYCameraKitException e)
            {
                cameraVersion = "Unknown";
            }
            FragmentActivity activity = getActivity();
            if (activity != null)
            {
                Snackbar.make(getActivity().findViewById(R.id.fragment1), "Camera " + cameraVersion + " / " + "CameraKit " + OLYCamera.getVersion(), Snackbar.LENGTH_SHORT).show();
                //Toast.makeText(getActivity(), "Camera " + cameraVersion + " / " + "CameraKit " + OLYCamera.getVersion(), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.action_select_all) {
            try
            {
                // 全選択・全選択の解除
                contentListHolder.setResetAllSelection();

                // 画面表示の更新
                gridView.invalidateViews();
                //GridViewAdapter adapter = (GridViewAdapter) gridView.getAdapter();
                //adapter.notifyDataSetChanged();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *   一括転送・一括削除ボタンの有効・無効の切替
     *
     */
    private void changeBatchButtonStatus()
    {
        try
        {
            boolean isEnabled = (contentListHolder.getSelectedContentCount() > 0);

            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null)
            {
                ImageButton downloadButton = activity.findViewById(R.id.download_batch);
                downloadButton.setEnabled(isEnabled);

                ImageButton deleteButton = activity.findViewById(R.id.delete_batch);
                deleteButton.setEnabled(isEnabled);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume() Start");
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null)
        {
            try
            {
                ImageButton refreshButton = activity.findViewById(R.id.button_list_refresh);
                ImageButton downloadButton = activity.findViewById(R.id.download_batch);
                ImageButton deleteButton = activity.findViewById(R.id.delete_batch);
                RadioButton dateButton = activity.findViewById(R.id.radio_date);
                RadioButton pathButton = activity.findViewById(R.id.radio_path);
                Spinner categorySpinner = activity.findViewById(R.id.category_spinner);
                boolean dateChecked = dateButton.isChecked();
                refreshButton.setOnClickListener(this);
                downloadButton.setOnClickListener(this);
                deleteButton.setOnClickListener(this);
                deleteButton.setEnabled(false);
                dateButton.setChecked(dateChecked);
                dateButton.setOnCheckedChangeListener(this);
                pathButton.setChecked(!dateChecked);
                pathButton.setOnCheckedChangeListener(this);
                categorySpinner.setOnItemSelectedListener(this);

                ActionBar bar = activity.getSupportActionBar();
                if (bar != null) {
                    // アクションバーの表示をするかどうか
                    boolean isShowActionBar = false;
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    if (preferences != null) {
                        isShowActionBar = preferences.getBoolean("use_playback_menu", false);
                    }
                    if (isShowActionBar) {
                        bar.show();  // ActionBarの表示を出す
                        //refreshButton.setVisibility(View.INVISIBLE);  // リフレッシュボタンを消す
                    } else {
                        bar.hide();   // ActionBarの表示を消す
                        //refreshButton.setVisibility(View.VISIBLE);  // リフレッシュボタンを表示する
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////
        refresh(true);
        ////////////////////////////////////////////////////////////////////////////////////

        Log.v(TAG, "onResume() End");
    }

    private void refresh(final boolean forceRefresh)
    {
        try
        {
            if (camera != null)
            {
                // 再生モードかどうかを確認して、再生モードでなかった場合には再生モードに切り替える。
                OLYCamera.RunMode runMode = camera.getRunMode();
                if (runMode != OLYCamera.RunMode.Playback) {
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                Log.v(TAG, "changeRunMode(OLYCamera.RunMode.Playback) : Start");
                                camera.changeRunMode(OLYCamera.RunMode.Playback);
                                Log.v(TAG, "changeRunMode(OLYCamera.RunMode.Playback) : End");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            refreshImpl(forceRefresh);
                        }
                    };
                    thread.start();
                } else {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            refreshImpl(forceRefresh);
                        }
                    });
                    thread.start();
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showHideProgressBar(true);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void showHideProgressBar(final boolean isVisible)
    {
        try
        {
            Activity activity = getActivity();
            if (activity != null)
            {
                ProgressBar bar = getActivity().findViewById(R.id.progress_bar);
                if (bar != null)
                {
                    bar.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
                    bar.invalidate();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void onPause()
    {
        Log.v(TAG, "onPause() Start");
        if (!executor.isShutdown())
        {
            executor.shutdownNow();
        }
        try
        {
            //  アクションバーは隠した状態に戻しておく
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null)
            {
                ActionBar bar = activity.getSupportActionBar();
                if (bar != null)
                {
                    bar.hide();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.gc();
        super.onPause();
        Log.v(TAG, "onPause() End");
    }

    @Override
    public void onStop()
    {
        if (camera == null)
        {
            super.onStop();
            return;
        }
        super.onStop();
    }

    private void refreshImpl(boolean forceRefresh)
    {
        Log.v(TAG, "refreshImpl() start");
        if ((contentListHolder.getContentList().size() == 0) || (forceRefresh))
        {
            camera.downloadContentList(new OLYCamera.DownloadContentListCallback() {
                @Override
                public void onCompleted(List<OLYCameraFileInfo> list) {
                    contentListHolder.setContent(list);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showHideProgressBar(false);
                            try {
                                FragmentActivity activity = getActivity();
                                if (activity != null) {
                                    Spinner categorySpinner = activity.findViewById(R.id.category_spinner);
                                    RadioButton dateButton = activity.findViewById(R.id.radio_date);
                                    boolean dateChecked = dateButton.isChecked();

                                    // パス一覧 / 日付一覧
                                    List<String> strList = (dateChecked) ? contentListHolder.getDateList() : contentListHolder.getPathList();

                                    // 先頭に ALLを追加
                                    //strList.add("ALL");
                                    strList.add(0, "ALL");
                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, strList);
                                    categorySpinner.setAdapter(adapter);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            gridView.invalidateViews();
                        }
                    });
                    Log.v(TAG, "refreshImpl() end");
                }

                @Override
                public void onErrorOccurred(Exception e)
                {
                    final String message = e.getMessage();
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            presentMessage("Load failed", message);
                        }
                    });
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showHideProgressBar(false);
                    gridView.invalidateViews();
                }
            });
        }
    }

    // AdapterView.OnItemSelectedListener
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        Log.v(TAG, "onItemSelected()");
        try
        {
            FragmentActivity activity = getActivity();
            if (activity != null)
            {
                Spinner spinner = activity.findViewById(R.id.category_spinner);
                String label = (String) spinner.getSelectedItem();
                Log.v(TAG, ":::::SELECTED LABEL  : " + label);

                RadioButton checkDate = activity.findViewById(R.id.radio_date);
                contentListHolder.setCondition(checkDate.isChecked(), label);

                // アイテムの選択を落とす
                contentListHolder.setAllSelection(false);
            }
            refresh(false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //  AdapterView.OnItemSelectedListener
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.v(TAG, "onNothingSelected()");
    }

    //  CompoundButton.OnCheckedChangeListener
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        Log.v(TAG, "onCheckedChanged : " + isChecked);
        try
        {
            if (!isChecked)
            {
                // do nothing!
                return;
            }
            boolean dateChecked = (buttonView.getId() == R.id.radio_date);
            FragmentActivity activity = getActivity();
            if (activity != null)
            {
                Spinner categorySpinner = activity.findViewById(R.id.category_spinner);

                // パス一覧 / 日付一覧
                List<String> strList = (dateChecked) ? contentListHolder.getDateList() : contentListHolder.getPathList();

                // 先頭に ALLを追加
                strList.add(0, "ALL");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, strList);
                categorySpinner.setAdapter(adapter);
                categorySpinner.invalidate();

                contentListHolder.setCondition(dateChecked, "ALL");

                // アイテムの選択を落とす
                contentListHolder.setAllSelection(false);

                // 画面更新。
                refresh(false);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // View.OnClickListener
    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.button_list_refresh)
        {
            refresh(true);
        }
        else if (id == R.id.download_batch)
        {
            // 複数ファイルのダウンロードを指示する
            Log.v(TAG, "PUSH DOWNLOAD!");
            showMultiFileDownloadConfirmationDialog();
        }
        else if (id == R.id.delete_batch)
        {
            // 複数ファイルの削除を指示する
            Log.v(TAG, "PUSH DELETE!");
            showMultiFileDeleteConfirmationDialog();
        }
    }

    /**
     *    選択した画像ファイルの一括削除
     *
     */
    private void showMultiFileDeleteConfirmationDialog()
    {
        try
        {
            int nofPictures = contentListHolder.getSelectedContentCount();
            if (nofPictures > 0)
            {
                //FragmentManager manager = getFragmentManager();
                FragmentManager manager = getParentFragmentManager();
                if (manager != null)
                {
                    MultiFileDeleteConfirmationDialog dialog = MultiFileDeleteConfirmationDialog.newInstance(this, nofPictures);
                    dialog.show(manager, "deleteSelection");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *    選択した画像ファイルの一括ダウンロード
     *
     */
    private void showMultiFileDownloadConfirmationDialog()
    {
        try {
            int nofPictures = contentListHolder.getSelectedContentCount();
            if (nofPictures > 0)
            {
                //FragmentManager manager = getFragmentManager();
                FragmentManager manager = getParentFragmentManager();
                if (manager != null)
                {
                    MultiFileDownloadConfirmationDialog dialog = MultiFileDownloadConfirmationDialog.newInstance(this, nofPictures);
                    dialog.show(manager, "selection");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     *   画像ファイルの一括ダウンロード処理
     *
     */
    @Override
    public void confirmSelection(int selectedButtonId, boolean withRaw)
    {
        Log.v(TAG, "confirmSelection : " + selectedButtonId + " (" + withRaw + ")");

        float downloadSize;
        switch (selectedButtonId)
        {
            case R.id.radio_download__1024x768:
                downloadSize = OLYCamera.IMAGE_RESIZE_1024;
                break;
            case R.id.radio_download__1600x1200:
                downloadSize = OLYCamera.IMAGE_RESIZE_1600;
                break;
            case R.id.radio_download__1920x1440:
                downloadSize = OLYCamera.IMAGE_RESIZE_1920;
                break;
            case R.id.radio_download__2048x1536:
                downloadSize = OLYCamera.IMAGE_RESIZE_2048;
                break;

            case R.id.radio_download__original_size:
            default:
                downloadSize = OLYCamera.IMAGE_RESIZE_NONE;
                break;
        }

        List<OLYCameraContentInfoEx> contentList = contentListHolder.getSelectedContentList();
        for (OLYCameraContentInfoEx item : contentList)
        {
            Log.v(TAG, "  " + item.getFileInfo().getFilename());
        }

        try
        {
            ImageDownloader downloader = new ImageDownloader(getActivity(), camera);
            downloader.startDownloadMulti(contentList, downloadSize, withRaw, this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void finishedDownloadMulti(boolean isAbort, int successCount, int failureCount)
    {
        Log.v(TAG, "finishedDownloadMulti() ok : " + successCount + "   NG : " + failureCount + " [abort : " + isAbort + "]");

        // ここでダウンロード結果を通知する
        String message;
        if (successCount == 0)
        {
            // 一枚も取得できなかった場合
            message = getString(R.string.message_download_finished_but_failed);
        }
        else if ((isAbort)||(failureCount != 0))
        {
            // 失敗があった場合
            message = getString(R.string.message_download_finished_pics_with_failure) + " " + successCount;
            message = message + " " + getString(R.string.message_download_failure_count) + " " + failureCount;
        }
        else
        {
            // 成功した場合
            message = getString(R.string.message_download_finished_pics) + " " + successCount;
        }

        final FragmentActivity activity = getActivity();
        if (activity != null)
        {
            final String messageToShow = message;
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    View view = activity.findViewById(R.id.fragment1);
                    Snackbar.make(view, messageToShow, Snackbar.LENGTH_SHORT).show();
                    //Toast.makeText(getContext(), messageToShow, Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 画面を更新する...
        //refresh(true);
        try
        {
            // 更新は、選択を解除するだけにする
            contentListHolder.setAllSelection(false);
            GridViewAdapter adapter = (GridViewAdapter) gridView.getAdapter();
            adapter.notifyDataSetChanged();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void confirmDeleteSelection(int selectedButtonId, final boolean withRaw, final boolean onlyRaw)
    {
        try
        {
            Log.v(TAG, " confirmDeleteSelection (with RAW : " + withRaw + ")");

            final Activity activity = getActivity();
            final ProgressDialog deleteProgressDialog =  new ProgressDialog(activity);
            deleteProgressDialog.setTitle(activity.getString(R.string.dialog_delete_title)); //+ " (" + loopCount  + " / " + maxCount + ")");
            deleteProgressDialog.setMessage(activity.getString(R.string.dialog_delete_message)); //+ " " + filename);
            deleteProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            deleteProgressDialog.setCancelable(false);
            deleteProgressDialog.show();

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try
                    {
                        try
                        {
                            // 再生保守モードに切り替える
                            camera.changeRunMode(OLYCamera.RunMode.Playmaintenance);
                        }
                        catch (Throwable t)
                        {
                            t.printStackTrace();
                        }

                        List<OLYCameraContentInfoEx> contentList = contentListHolder.getSelectedContentList();
                        int itemCount = contentList.size();
                        int processCount = 0;
                        for (OLYCameraContentInfoEx item : contentList)
                        {
                            OLYCameraFileInfo fileInfo = item.getFileInfo();
                            Log.v(TAG, "  " + fileInfo.getDirectoryPath() + "/" + fileInfo.getFilename() + " with RAW : " + withRaw + "  only RAW :" + onlyRaw);

                            // JPEGファイルを削除する
                            if (!onlyRaw)
                            {
                                String fileName = fileInfo.getDirectoryPath() + "/" + fileInfo.getFilename();
                                Log.v(TAG, " DELETE FILE NAME : " + fileName);
                                try
                                {
                                    camera.eraseContent(fileName);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }

                            if ((item.hasRaw())&&(withRaw || onlyRaw))
                            {
                                // RAWファイルも削除する
                                String fileName = fileInfo.getDirectoryPath() + "/" + fileInfo.getFilename().substring(0, fileInfo.getFilename().indexOf(".")) + RAW_SUFFIX;
                                Log.v(TAG, " DELETE (RAW) FILE NAME : " + fileName);
                                try
                                {
                                    camera.eraseContent(fileName);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            processCount++;

                            deleteProgressDialog.setProgress(processCount / itemCount * 100);
                        }
                        // すべての選択を落とす
                        contentListHolder.setAllSelection(false);

                        try
                        {
                            // 再生モードに戻す
                            camera.changeRunMode(OLYCamera.RunMode.Playback);
                        }
                        catch (Throwable t)
                        {
                            t.printStackTrace();
                        }

                        // 画面表示を更新する
                        refreshImpl(true);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            if (deleteProgressDialog != null)
                            {
                                deleteProgressDialog.dismiss();
                            }
                        }
                    });
                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static class GridCellViewHolder
    {
		ImageView imageView;
		ImageView iconView;
		ImageView checkView;
	}
	
	private class GridViewAdapter extends BaseAdapter
    {
		private final LayoutInflater inflater;

		GridViewAdapter(LayoutInflater inflater)
		{
			this.inflater = inflater;
		}

		private List<?> getItemList()
        {
            return (contentListHolder.getContentList());
		}
		
		@Override
		public int getCount()
        {
			if (getItemList() == null)
			{
				return 0;
			}
			return getItemList().size();
		}

		@Override
		public Object getItem(int position)
        {
			if (getItemList() == null)
			{
				return null;
			}
			return getItemList().get(position);
		}

		@Override
		public long getItemId(int position)
        {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
        {
            try {
                GridCellViewHolder viewHolder;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.view_grid_cell, parent, false);

                    viewHolder = new GridCellViewHolder();
                    viewHolder.imageView = convertView.findViewById(R.id.imageViewY);
                    viewHolder.iconView = convertView.findViewById(R.id.imageViewZ);
                    viewHolder.checkView = convertView.findViewById(R.id.imageViewX);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (GridCellViewHolder) convertView.getTag();
                }

                OLYCameraContentInfoEx infoEx = (OLYCameraContentInfoEx) getItem(position);
                OLYCameraFileInfo item = (infoEx != null) ? infoEx.getFileInfo() : null;
                if (item == null)
                {
                    viewHolder.imageView.setImageResource(R.drawable.ic_satellite_grey_24dp);
                    viewHolder.iconView.setImageDrawable(null);
                    viewHolder.checkView.setImageDrawable(null);
                    return convertView;
                }

                String path = new File(item.getDirectoryPath(), item.getFilename()).getPath();
                Bitmap thumbnail = imageCache.get(path);
                if (thumbnail == null)
                {
                    viewHolder.imageView.setImageResource(R.drawable.ic_satellite_grey_24dp);
                    viewHolder.iconView.setImageDrawable(null);
                    viewHolder.checkView.setImageDrawable(null);
                    if (!gridViewIsScrolling)
                    {
                        if (executor.isShutdown())
                        {
                            executor = Executors.newFixedThreadPool(1);
                        }
                        executor.execute(new ThumbnailLoader(viewHolder, path, infoEx.hasRaw()));
                    }
                } else {
                    viewHolder.imageView.setImageBitmap(thumbnail);
                    if (path.toLowerCase().endsWith(MOVIE_SUFFIX)) {
                        viewHolder.iconView.setImageResource(R.drawable.icn_movie);
                    } else if (infoEx.hasRaw()) {
                        viewHolder.iconView.setImageResource(R.drawable.ic_raw_black_1x);
                    } else {
                        viewHolder.iconView.setImageDrawable(null);
                    }
                    if (infoEx.isChecked())
                    {
                        viewHolder.checkView.setImageResource(R.drawable.ic_check_green_24dp);
                    }
                    else
                    {
                        viewHolder.checkView.setImageDrawable(null);
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
			return convertView;
		}
	}
	
	private class GridViewOnItemClickListener implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener
    {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
			try
            {
				ImagePagerViewFragment fragment = new ImagePagerViewFragment();    // Use an advanced viewer.
				fragment.setCamera(camera);
				fragment.setContentList(contentListHolder.getContentList());
				fragment.setContentIndex(position);
				Activity activity = getActivity();
				if (activity != null)
				{
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    transaction.replace(getId(), fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

        @Override
        public boolean onItemLongClick(final AdapterView<?> parent, View view, int position, long id)
        {
            try
            {
                OLYCameraContentInfoEx infoEx = contentListHolder.getContentList().get(position);
                if (infoEx != null)
                {
                    infoEx.toggleChecked();
                }
                view.invalidate();
                changeBatchButtonStatus();
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            GridViewAdapter adapter = (GridViewAdapter) parent.getAdapter();
                            adapter.notifyDataSetChanged();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
                return (true);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return (false);
        }
    }
	
	private class GridViewOnScrollListener implements AbsListView.OnScrollListener
    {
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
        {
			// No operation.
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState)
        {
			if (scrollState == SCROLL_STATE_IDLE) {
				gridViewIsScrolling = false;
				gridView.invalidateViews();
			} else if ((scrollState == SCROLL_STATE_FLING) || (scrollState == SCROLL_STATE_TOUCH_SCROLL)) {
				gridViewIsScrolling = true;
				if (!executor.isShutdown()) {
					executor.shutdownNow();
				}
			}
		}
	}

	private class ThumbnailLoader implements Runnable
    {
		private GridCellViewHolder viewHolder;
		private String path;
        private final boolean hasRaw;
		
		ThumbnailLoader(GridCellViewHolder viewHolder, String path, boolean hasRaw)
        {
			this.viewHolder = viewHolder;
			this.path = path;
            this.hasRaw = hasRaw;
		}
		
		@Override
		public void run()
        {
			class Box {
				boolean isDownloading = true;
			}
			final Box box = new Box();
			
			camera.downloadContentThumbnail(path, new OLYCamera.DownloadImageCallback() {
				@Override
				public void onProgress(ProgressEvent e) {
				}
				
				@Override
				public void onCompleted(byte[] data, Map<String, Object> metadata) {
					final Bitmap thumbnail = createRotatedBitmap(data, metadata);
					imageCache.put(path, thumbnail);
					runOnUiThread(new Runnable() {
						@Override
						public void run()
                        {
							viewHolder.imageView.setImageBitmap(thumbnail);
							if (path.toLowerCase().endsWith(MOVIE_SUFFIX))
							{
								viewHolder.iconView.setImageResource(R.drawable.icn_movie);
							}
							else if (hasRaw)
                            {
                                viewHolder.iconView.setImageResource(R.drawable.ic_raw_black_1x);
                            }
                            else
                            {
								viewHolder.iconView.setImageDrawable(null);
							}
                            viewHolder.checkView.setImageDrawable(null);
						}
					});
					box.isDownloading = false;  
				}
				
				@Override
				public void onErrorOccurred(Exception e) {
					box.isDownloading = false;
				}
			});

			// Waits to realize the serial download.
			while (box.isDownloading) {
				Thread.yield();
			}
		}
	}
	
	
	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------
	
	private void presentMessage(String title, String message)
    {
        try
        {
            Context context = getActivity();
            if (context == null) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(title).setMessage(message);
            builder.show();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	
	private void runOnUiThread(Runnable action)
    {
        try
        {
            Activity activity = getActivity();
            if (activity == null) return;

            activity.runOnUiThread(action);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	
	
	private Bitmap createRotatedBitmap(byte[] data, Map<String, Object> metadata)
    {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if (bitmap == null)
		{
			return (null);
		}
		
		int degrees = getRotationDegrees(data, metadata);
		if (degrees != 0)
		{
			Matrix m = new Matrix();
			m.postRotate(degrees);
			try
            {
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
			}
			catch (Throwable e)
            {
				e.printStackTrace();
			}
		}
		return bitmap;
	}
	
	private int getRotationDegrees(byte[] data, Map<String, Object> metadata)
    {
		int degrees = 0;
		int orientation = ExifInterface.ORIENTATION_UNDEFINED;
		
		if (metadata != null && metadata.containsKey("Orientation"))
		{
			String orientationStr = (String)metadata.get("Orientation");
			if (orientationStr != null)
			{
				orientation = Integer.parseInt(orientationStr);
			}
		} else {
			// Gets image orientation to display a picture.
			try {
				File tempFile = File.createTempFile("temp", null);
				{
					FileOutputStream outStream = new FileOutputStream(tempFile.getAbsolutePath());
					outStream.write(data);
					outStream.close();
				}
				
				ExifInterface exifInterface = new ExifInterface(tempFile.getAbsolutePath());
				orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

				boolean ret = tempFile.delete();
				if (!ret)
                {
                    Log.v(TAG, "tempFile.delete() : failure");
                }
			}
			catch (Exception e)
            {
				e.printStackTrace();
			}
		}

		switch (orientation)
		{
			case ExifInterface.ORIENTATION_ROTATE_90:
				degrees = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degrees = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degrees = 270;
				break;
			case ExifInterface.ORIENTATION_NORMAL:
			default:
				break;
		}
		return degrees;
	}
}
