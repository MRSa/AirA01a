package jp.osdn.gokigen.aira01a.liveview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.location.Location;

interface IStoreImage
{
    void doStore(final Bitmap target, final Location location, final boolean isShare);
    void setActivity(Activity activity);
}
