package com.example.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.BlocksAdapter;
import com.example.model.Block;

import java.util.Collections;
import java.util.List;

public class BlockTouchHelperCallback extends ItemTouchHelper.Callback {

    private final BlocksAdapter adapter;
    private final DragDropListener listener;

    public interface DragDropListener {
        void onBlocksReordered(List<Block> reorderedList);
    }

    public BlockTouchHelperCallback(BlocksAdapter adapter, DragDropListener listener) {
        this.adapter = adapter;
        this.listener = listener;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        // Disabled so users can scroll/select text inside edit fields naturally.
        // Dragging is initiated via the drag handle touch listener instead.
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        List<Block> blocks = adapter.getBlocks();
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(blocks, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(blocks, i, i - 1);
            }
        }
        adapter.notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // No-op
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        listener.onBlocksReordered(adapter.getBlocks());
    }
}
