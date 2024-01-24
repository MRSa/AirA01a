package jp.osdn.gokigen.aira01a.myprops;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;

class MyCameraPropertyLoadArrayAdapter extends ArrayAdapter<MyCameraPropertySetItems>
{
    private LayoutInflater inflater;
    private final int textViewResourceId;
    private List<MyCameraPropertySetItems> listItems;

    MyCameraPropertyLoadArrayAdapter(Context context, int resource, List<MyCameraPropertySetItems> objects)
    {
        super(context, resource, objects);
        textViewResourceId = resource;
        listItems = objects;

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     *
     */
    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        View view;
        if(convertView != null)
        {
            view = convertView;
        }
        else
        {
            view = inflater.inflate(textViewResourceId, parent, false);
        }
        MyCameraPropertySetItems item = listItems.get(position);
        try
        {
            TextView idView = view.findViewWithTag("id");
            idView.setText(item.getItemId());

            TextView titleView = view.findViewWithTag("title");
            titleView.setText(item.getItemName());

            TextView infoView = view.findViewWithTag("info");
            infoView.setText(item.getItemInfo());

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (view);
    }
}
