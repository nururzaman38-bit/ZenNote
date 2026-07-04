package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Page;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class DrawerPagesAdapter extends RecyclerView.Adapter<DrawerPagesAdapter.PageViewHolder> {

    private final List<Page> pagesList = new ArrayList<>();
    private long activePageId = -1;
    private final PageClickListener listener;

    public interface PageClickListener {
        void onPageClick(Page page);
        void onPageDelete(Page page);
    }

    public DrawerPagesAdapter(PageClickListener listener) {
        this.listener = listener;
    }

    public void setPages(List<Page> pages, long activeId) {
        this.pagesList.clear();
        this.pagesList.addAll(pages);
        this.activePageId = activeId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        Page page = pagesList.get(position);
        holder.bind(page, page.getId() == activePageId);
    }

    @Override
    public int getItemCount() {
        return pagesList.size();
    }

    class PageViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardPage;
        final TextView tvTitle;
        final ImageButton btnDelete;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPage = itemView.findViewById(R.id.card_page_item);
            tvTitle = itemView.findViewById(R.id.tv_page_title);
            btnDelete = itemView.findViewById(R.id.btn_delete_page);
        }

        void bind(Page page, boolean isActive) {
            tvTitle.setText(page.getTitle());
            
            if (isActive) {
                cardPage.setCardBackgroundColor(itemView.getContext().getColor(R.color.primary));
                tvTitle.setTextColor(itemView.getContext().getColor(R.color.white));
                btnDelete.setColorFilter(itemView.getContext().getColor(R.color.white));
            } else {
                cardPage.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
                tvTitle.setTextColor(itemView.getContext().getColor(R.color.on_surface));
                btnDelete.setColorFilter(itemView.getContext().getColor(R.color.accent_error));
            }

            itemView.setOnClickListener(v -> listener.onPageClick(page));
            btnDelete.setOnClickListener(v -> listener.onPageDelete(page));
        }
    }
}
