package jp.osdn.gokigen.aira01a.connection.ble;

class OlyCameraSetArrayItem
{
    private final String dataId;
    private String btName;
    private String btPassCode;
    private String information;

    OlyCameraSetArrayItem(String dataId, String name, String passCode, String information)
    {
        this.dataId = dataId;
        this.btName = name;
        this.btPassCode = passCode;
        this.information = information;
    }

    String getDataId()
    {
        return (dataId);
    }

    String getBtName()
    {
        return (btName);
    }

    String getBtPassCode()
    {
        return (btPassCode);
    }

    String getInformation()
    {
        return (information);
    }
}
