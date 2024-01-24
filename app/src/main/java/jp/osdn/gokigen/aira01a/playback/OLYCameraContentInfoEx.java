package jp.osdn.gokigen.aira01a.playback;

import jp.co.olympus.camerakit.OLYCameraFileInfo;

public class OLYCameraContentInfoEx
{
    private final OLYCameraFileInfo fileInfo;
    private boolean hasRaw;
    private boolean checked = false;
    OLYCameraContentInfoEx(OLYCameraFileInfo fileInfo, boolean hasRaw)
    {
        this.fileInfo = fileInfo;
        this.hasRaw = hasRaw;
    }

    void setChecked(boolean isChecked)
    {
        checked = isChecked;
    }

    void toggleChecked()
    {
        checked = !checked;
    }

    boolean isChecked()
    {
        return (checked);
    }

    void setHasRaw()
    {
        hasRaw = true;
    }

    public OLYCameraFileInfo getFileInfo()
    {
        return (fileInfo);
    }
    public boolean hasRaw()
    {
        return (hasRaw);
    }
}
