package com.aricohen.easyzmanim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LocationPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 1;
    private static final int TYPE_CITY = 2;

    public interface OnCityClick {
        void onCityClick(LocationItem.City city);
    }

    private final List<LocationItem> items = new ArrayList<>();
    private final OnCityClick onCityClick;

    public LocationPickerAdapter(OnCityClick onCityClick) {
        this.onCityClick = onCityClick;
    }

    public void setItems(List<LocationItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isHeader() ? TYPE_HEADER : TYPE_CITY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inf.inflate(R.layout.item_location_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inf.inflate(R.layout.item_location_city, parent, false);
            return new CityVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LocationItem item = items.get(position);

        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).headerText.setText(((LocationItem.Header) item).title);
        } else {
            LocationItem.City city = (LocationItem.City) item;
            CityVH vh = (CityVH) holder;
            vh.cityText.setText(city.name);

            vh.itemView.setOnClickListener(v -> {
                if (onCityClick != null) onCityClick.onCityClick(city);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView headerText;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }

    static class CityVH extends RecyclerView.ViewHolder {
        final TextView cityText;
        CityVH(@NonNull View itemView) {
            super(itemView);
            cityText = itemView.findViewById(R.id.cityText);
        }
    }
}
