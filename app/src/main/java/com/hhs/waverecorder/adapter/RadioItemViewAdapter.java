package com.hhs.waverecorder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.hhs.wavrecorder.R;

import java.util.List;

public class RadioItemViewAdapter extends BaseAdapter {
    Context context;
    List<String> myList;
    LayoutInflater mInflater;
    private int selectPosition = 0;

    public RadioItemViewAdapter(Context context, List<String> mList){
        this.context = context;
        this.myList = mList;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setSelectPosition(int position) {
        this.selectPosition = position;
        this.notifyDataSetChanged(); // trigger getView
    }
    public int getSelectPosition() {
        return selectPosition;
    }
    @Override
    public int getCount() {
        return myList.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if(convertView == null){
            convertView = mInflater.inflate(R.layout.adapter_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.name = convertView.findViewById(R.id.id_name);
            viewHolder.select = convertView.findViewById(R.id.id_select);
            convertView.setTag(viewHolder);
        } else{
            viewHolder = (ViewHolder)convertView.getTag();
        }
        viewHolder.name.setText(myList.get(position));
        if(this.selectPosition == position){
            viewHolder.select.setChecked(true);
        } else{
            viewHolder.select.setChecked(false);
        }
        return convertView;
    }
}