package jp.osdn.gokigen.aira01a.myprops;

class MyCameraPropertySetItems
{
    private final String itemId;
    private String itemName;
    private String itemInfo;
    private int iconResource ;

    MyCameraPropertySetItems(int iconResource, String itemId, String itemName, String itemInfo)
    {
        this.iconResource = iconResource;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemInfo = itemInfo;
    }

    String getItemId()
    {
        return itemId;
    }

    String getItemName()
    {
        return itemName;
    }

    String getItemInfo()
    {
        return itemInfo;
    }

    int getIconResource()
    {
        return iconResource;
    }
}
