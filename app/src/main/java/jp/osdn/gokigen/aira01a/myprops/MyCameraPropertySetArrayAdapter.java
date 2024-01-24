package jp.osdn.gokigen.aira01a.myprops;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.NonNull;
import jp.osdn.gokigen.aira01a.R;

class MyCameraPropertySetArrayAdapter  extends ArrayAdapter<MyCameraPropertySetItems>
{
    private final String TAG = toString();
    private final Context context;
    private LayoutInflater inflater;
    private final int textViewResourceId;
    private List<MyCameraPropertySetItems> listItems;
    private final ILoadSaveMyCameraPropertyDialogDismiss dialogDismiss;


    MyCameraPropertySetArrayAdapter(Context context, int resource, List<MyCameraPropertySetItems> objects, ILoadSaveMyCameraPropertyDialogDismiss dialogDismiss)
    {
        super(context, resource, objects);
        this.context = context;
        textViewResourceId = resource;
        listItems = objects;
        this.dialogDismiss = dialogDismiss;

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
        try
        {
            final MyCameraPropertySetItems item = listItems.get(position);

            TextView idView = view.findViewWithTag("id");
            idView.setText(item.getItemId());

            final EditText titleView = view.findViewWithTag("title");
            titleView.setText(item.getItemName());

            TextView infoView = view.findViewWithTag("info");
            infoView.setText(item.getItemInfo());

            Button button = view.findViewWithTag("button");
            button.setOnClickListener(new Button.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {

                    String idHeader = item.getItemId();
                    String title = titleView.getText().toString();
                    String itemInfo = item.getItemInfo();

                    Log.v(TAG, "CLICKED : " + idHeader + " " + title + " (" + item.getItemName() + " " + itemInfo + ")" );
                    if (dialogDismiss != null)
                    {
                        dialogDismiss.doDismissWithPropertySave(idHeader, title);
                    }
                    Log.v(TAG, "PROPERTY STORED : " + idHeader + " " + title);

                    // Toastで保管したことを通知する
                    String restoredMessage = context.getString(R.string.saved_my_props) + title;
                    Toast.makeText(context, restoredMessage, Toast.LENGTH_SHORT).show();

                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (view);
    }
}
