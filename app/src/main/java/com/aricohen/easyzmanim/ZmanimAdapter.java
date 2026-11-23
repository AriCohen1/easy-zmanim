package com.aricohen.easyzmanim;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ZmanimAdapter extends RecyclerView.Adapter<ZmanimAdapter.VH> {

    private final List<ZmanRow> items = new ArrayList<>();
    private int highlightIndex = -1;

    public void setItems(List<ZmanRow> newItems, int highlightIndex) {
        items.clear();
        items.addAll(newItems);
        this.highlightIndex = highlightIndex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_zman, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ZmanRow row = items.get(position);

        holder.labelText.setText(row.label);
        holder.timeText.setText(row.time);

        boolean highlighted = (position == highlightIndex);

        // Simple highlight that looks good everywhere: slightly larger + bold + higher elevation
        holder.timeText.setTypeface(null, highlighted ? Typeface.BOLD : Typeface.NORMAL);
        holder.labelText.setTypeface(null, highlighted ? Typeface.BOLD : Typeface.NORMAL);

        holder.card.setCardElevation(highlighted ? 8f : 2f);
        holder.card.setAlpha(highlighted ? 1.0f : 0.92f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final CardView card;
        final TextView labelText;
        final TextView timeText;

        VH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            labelText = itemView.findViewById(R.id.labelText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}
