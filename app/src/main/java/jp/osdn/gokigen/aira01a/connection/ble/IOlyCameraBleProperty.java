package jp.osdn.gokigen.aira01a.connection.ble;

/**
 *  Bluetooth のプロパティにアクセスするインタフェース
 *
 */
public interface IOlyCameraBleProperty
{
    int MAX_STORE_PROPERTIES = 12;  // Olympus Airは、最大12個登録可能

    String OLYCAMERA_BLUETOOTH_SETTINGS = "olympus_air_bt";
    String OLYCAMERA_BLUETOOTH_POWER_ON = "ble_power_on";

    String NAME_KEY = "AirBtName";
    String CODE_KEY = "AirBtCode";
    String DATE_KEY = "AirBtId";
}
