package deveoper.lin.local.picturebrowse.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import deveoper.lin.local.picturebrowse.R;
import deveoper.lin.local.picturebrowse.entity.GirdViewEntity;
import deveoper.lin.local.picturebrowse.util.ImageLoader;

/**
 * Created by lin on 2015/10/27.
 */
public class GirdViewAdapter extends BaseListAdapter<GirdViewEntity> {

    public static final int THREAD_COUNT = 3;

    public GirdViewAdapter(ArrayList<GirdViewEntity> mList, Context context) {
        super(mList, context);
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_gird_view_adapter, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        GirdViewEntity entity = getItem(position);
        ImageLoader.getInstance(THREAD_COUNT, ImageLoader.Type.LIFO).loadImage(entity.getAbsolutePath(), viewHolder.imageView);
        viewHolder.imageBtn.setBackgroundResource(entity.isSelected() ?
                R.drawable.icon_select : R.drawable.icon_un_select);
        if (entity.isSelected()) {
            viewHolder.imageView.setColorFilter(mContext.getResources().getColor(R.color.transparent_one));
        } else {
            viewHolder.imageView.clearColorFilter();
        }
        viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelectStatus(position);
            }
        });

        return convertView;
    }

    private void updateSelectStatus(int position) {
        if (isNotEmptyOrNull()) {
            boolean status = getItem(position).isSelected();
            getData().get(position).setIsSelected(!status);
            notifyDataSetChanged();
        }
    }


    static class ViewHolder {
        @InjectView(R.id.image_view)
        ImageView imageView;
        @InjectView(R.id.image_btn)
        ImageButton imageBtn;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
