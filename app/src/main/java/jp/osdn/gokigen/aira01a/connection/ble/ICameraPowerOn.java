package jp.osdn.gokigen.aira01a.connection.ble;


public interface ICameraPowerOn
{
    // カメラ起動指示
    void wakeup(PowerOnCameraCallback callback);

    // 実行終了時のコールバックのインタフェース
    interface PowerOnCameraCallback
    {
        void wakeupExecuted(boolean isExecute);
    }
}
