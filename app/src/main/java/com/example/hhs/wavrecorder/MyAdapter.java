package com.example.hhs.wavrecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.List;

public class MyAdapter extends BaseAdapter {
    Context context;
    List<String> myList;
    LayoutInflater mInflater;
    private int selectPosition = 0;

    public MyAdapter(Context context,List<String> mList){
        this.context = context;
        this.myList = mList;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setSelectPosition(int position) {
        this.selectPosition = position;
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

        ViewHolder viewHolder = null;
        if(convertView == null){
            convertView = mInflater.inflate(R.layout.adapter_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView)convertView.findViewById(R.id.id_name);
            viewHolder.select = (RadioButton)convertView.findViewById(R.id.id_select);
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