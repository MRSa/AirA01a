package jp.osdn.gokigen.aira01a.playback;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jp.co.olympus.camerakit.OLYCameraFileInfo;

class OlyCameraContentListHolder
{
    private final String TAG = this.toString();
    private static final String MOVIE_SUFFIX = ".mov";
    private static final String JPEG_SUFFIX = ".jpg";
    private static final String RAW_SUFFIX = ".orf";

    private List<OLYCameraContentInfoEx> contentList  = new ArrayList<>();

    private String label = "ALL";
    private boolean isDateFilter = true;

    OlyCameraContentListHolder()
    {

    }

    void setCondition(boolean isDateChecked, String label)
    {
        isDateFilter = isDateChecked;
        this.label = label;
    }

    void setContent(List<OLYCameraFileInfo> list)
    {
        contentList.clear();

        // Sort contents in chronological order (or alphabetical order).
        Collections.sort(list, new Comparator<OLYCameraFileInfo>() {
            @Override
            public int compare(OLYCameraFileInfo lhs, OLYCameraFileInfo rhs) {
                long diff = rhs.getDatetime().getTime() - lhs.getDatetime().getTime();
                if (diff == 0)
                {
                    diff = rhs.getFilename().compareTo(lhs.getFilename());
                }
                return (int)Math.min(Math.max(-1, diff), 1);
            }
        });
        HashMap<String, OLYCameraContentInfoEx> rawItems = new HashMap<>();
        for (OLYCameraFileInfo item : list)
        {
            String path = item.getFilename().toLowerCase(Locale.getDefault());
            if ((path.endsWith(JPEG_SUFFIX))||(path.endsWith(MOVIE_SUFFIX)))
            {
                contentList.add(new OLYCameraContentInfoEx(item, false));
            }
            else if (path.endsWith(RAW_SUFFIX))
            {
                rawItems.put(path, new OLYCameraContentInfoEx(item, true));
            }
        }

        for (OLYCameraContentInfoEx item : contentList)
        {
            String path = item.getFileInfo().getFilename().toLowerCase(Locale.getDefault());
            if (path.endsWith(JPEG_SUFFIX))
            {
                String target = path.replace(JPEG_SUFFIX, RAW_SUFFIX);
                OLYCameraContentInfoEx raw = rawItems.get(target);
                if (raw != null)
                {
                    item.setHasRaw();
                    Log.v(TAG, "DETECT RAW FILE: " + target);
                }
            }
        }
    }

    /**
     * 　アイテムの選択状態を変更する
     *
     */
    void setAllSelection(boolean isSelect)
    {
        try
        {
            for (OLYCameraContentInfoEx item : contentList)
            {
                item.setChecked(isSelect);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   すべて選択されているか？
     *
     */
    private boolean isAllSelected()
    {
        return (contentList.size() == getSelectedContentCount());
    }

    /**
     *   「選択アイテム数」を応答する
     *
     */
    int getSelectedContentCount()
    {
        int selected = 0;
        try
        {
            for (OLYCameraContentInfoEx item : contentList)
            {
                if (item.isChecked())
                {
                    selected++;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (selected);
    }

    List<OLYCameraContentInfoEx> getSelectedContentList()
    {
        if (contentList == null)
        {
            return (new ArrayList<>());
        }

        ArrayList<OLYCameraContentInfoEx> targetList = new ArrayList<>();
        for (OLYCameraContentInfoEx item : contentList)
        {
            if (item.isChecked())
            {
                targetList.add(item);
            }
        }
        Collections.sort(targetList, new Comparator<OLYCameraContentInfoEx>() {
            @Override
            public int compare(OLYCameraContentInfoEx lhs, OLYCameraContentInfoEx rhs)
            {
                long diff = rhs.getFileInfo().getDatetime().getTime() - lhs.getFileInfo().getDatetime().getTime();
                if (diff == 0)
                {
                    diff = rhs.getFileInfo().getFilename().compareTo(lhs.getFileInfo().getFilename());
                }
                return (int)Math.min(Math.max(-1, diff), 1);
            }
        });
        return (targetList);
    }

    List<OLYCameraContentInfoEx> getContentList()
    {
        try
        {
            if (label.equals("ALL"))
            {
                return (contentList);
            }
            return ((isDateFilter) ? filterDate(label) : filterPath(label));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (new ArrayList<>());
    }

    private List<OLYCameraContentInfoEx> filterDate(String dateLabel)
    {
        //Log.v(TAG, "filterDate() : " + dateLabel);
        if (contentList == null)
        {
            return (new ArrayList<>());
        }

        ArrayList<OLYCameraContentInfoEx> targetList = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        for (OLYCameraContentInfoEx content : contentList)
        {
            String capturedDate = format.format(content.getFileInfo().getDatetime());
            if (dateLabel.equals(capturedDate))
            {
                targetList.add(content);
            }
        }
        Collections.sort(targetList, new Comparator<OLYCameraContentInfoEx>()
        {
            @Override
            public int compare(OLYCameraContentInfoEx lhs, OLYCameraContentInfoEx rhs)
            {
                long diff = rhs.getFileInfo().getDatetime().getTime() - lhs.getFileInfo().getDatetime().getTime();
                if (diff == 0)
                {
                    diff = rhs.getFileInfo().getFilename().compareTo(lhs.getFileInfo().getFilename());
                }
                return (int)Math.min(Math.max(-1, diff), 1);
            }
        });
        //Collections.sort(targetList);
        return (targetList);
    }

    private List<OLYCameraContentInfoEx> filterPath(String path)
    {
        //Log.v(TAG, "filterPath() : " + path);
        if (contentList == null)
        {
            return (new ArrayList<>());
        }

        ArrayList<OLYCameraContentInfoEx> targetList = new ArrayList<>();
        for (OLYCameraContentInfoEx content : contentList)
        {
            if (path.equals(content.getFileInfo().getDirectoryPath()))
            {
                targetList.add(content);
            }
        }
        Collections.sort(targetList, new Comparator<OLYCameraContentInfoEx>() {
            @Override
            public int compare(OLYCameraContentInfoEx lhs, OLYCameraContentInfoEx rhs)
            {
                long diff = rhs.getFileInfo().getDatetime().getTime() - lhs.getFileInfo().getDatetime().getTime();
                if (diff == 0)
                {
                    diff = rhs.getFileInfo().getFilename().compareTo(lhs.getFileInfo().getFilename());
                }
                return (int)Math.min(Math.max(-1, diff), 1);
            }
        });
        //Collections.sort(targetList);
        //Log.v(TAG, "getContentsListAtPath() " + targetList.size());
        return (targetList);
    }


    List<String> getDateList()
    {
        if (contentList == null)
        {
            return (new ArrayList<>());
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        HashMap<String, String> map = new HashMap<>();
        for (OLYCameraContentInfoEx content : contentList)
        {
            map.put(format.format(content.getFileInfo().getDatetime()), content.getFileInfo().getFilename());
        }
        ArrayList<String> dateList = new ArrayList<>(map.keySet());
        Collections.sort(dateList, Collections.reverseOrder());
        return (dateList);
    }

    List<String>  getPathList()
    {
        if (contentList == null)
        {
            return (new ArrayList<>());
        }
        HashMap<String, String> map = new HashMap<>();
        for (OLYCameraContentInfoEx content : contentList)
        {
            map.put(content.getFileInfo().getDirectoryPath(), content.getFileInfo().getFilename());
        }
        ArrayList<String> pathList = new ArrayList<>(map.keySet());
        Collections.sort(pathList, Collections.reverseOrder());
        return (pathList);
    }

    /**
     * 　アイテムの選択状態をすべて設定する (フィルターも加味する)
     *
     */
    void setResetAllSelection()
    {
        if (label.equals("ALL"))
        {
            setAllSelection(!isAllSelected());
        }
        else if (isDateFilter)
        {
            setResetAllSelectionWithDate(label);
        }
        else
        {
            setResetAllSelectionWithPath(label);
        }
    }

    /**
     * 　アイテムの全選択・全選択解除を指定されているパスに関して設定する
     *
     */
    private void setResetAllSelectionWithPath(String path)
    {
        int itemCount = 0;
        int checkedCount = 0;
        for (OLYCameraContentInfoEx content : contentList)
        {
            if (path.equals(content.getFileInfo().getDirectoryPath()))
            {
                itemCount++;
                if (content.isChecked())
                {
                    checkedCount++;
                }
            }
        }

        boolean isChecked = (itemCount != checkedCount);
        for (OLYCameraContentInfoEx content : contentList)
        {
            if (path.equals(content.getFileInfo().getDirectoryPath()))
            {
                content.setChecked(isChecked);
            }
        }
    }

    /**
     * 　アイテムの全選択・全選択解除を指定されている日付に関して設定する
     *
     */
    private void setResetAllSelectionWithDate(String dateLabel)
    {
        int itemCount = 0;
        int checkedCount = 0;
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        for (OLYCameraContentInfoEx content : contentList)
        {
            String capturedDate = format.format(content.getFileInfo().getDatetime());
            if (dateLabel.equals(capturedDate))
            {
                itemCount++;
                if (content.isChecked())
                {
                    checkedCount++;
                }
            }
        }

        boolean isChecked = (itemCount != checkedCount);
        for (OLYCameraContentInfoEx content : contentList)
        {
            String capturedDate = format.format(content.getFileInfo().getDatetime());
            if (dateLabel.equals(capturedDate))
            {
                content.setChecked(isChecked);
            }
        }
    }

}
