package com.example.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.database.DatabaseHelper;
import com.example.model.Block;
import com.example.model.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {
    private final DatabaseHelper dbHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Page>> pages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Page> activePage = new MutableLiveData<>();
    private final MutableLiveData<List<Block>> activePageBlocks = new MutableLiveData<>(new ArrayList<>());

    public MainViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new DatabaseHelper(application);
        loadPages();
    }

    public LiveData<List<Page>> getPages() { return pages; }
    public LiveData<Page> getActivePage() { return activePage; }
    public LiveData<List<Block>> getActivePageBlocks() { return activePageBlocks; }

    public void loadPages() {
        executor.execute(() -> {
            List<Page> list = dbHelper.getAllPages();
            pages.postValue(list);
            
            Page current = activePage.getValue();
            if (current != null) {
                loadPage(current.getId());
            } else if (!list.isEmpty()) {
                loadPage(list.get(0).getId());
            }
        });
    }

    public void createNewPage(String title) {
        executor.execute(() -> {
            long newId = dbHelper.createPage(title);
            List<Page> list = dbHelper.getAllPages();
            pages.postValue(list);
            loadPage(newId);
        });
    }

    public void loadPage(long pageId) {
        executor.execute(() -> {
            Page page = dbHelper.getPageById(pageId);
            if (page != null) {
                activePage.postValue(page);
                List<Block> blocks = dbHelper.getBlocksForPage(pageId);
                activePageBlocks.postValue(blocks);
            }
        });
    }

    public void updatePageTitle(long pageId, String title) {
        executor.execute(() -> {
            dbHelper.updatePageTitle(pageId, title);
            List<Page> list = dbHelper.getAllPages();
            pages.postValue(list);
            Page active = activePage.getValue();
            if (active != null && active.getId() == pageId) {
                active.setTitle(title);
                activePage.postValue(active);
            }
        });
    }

    public void deletePage(long pageId) {
        executor.execute(() -> {
            dbHelper.deletePage(pageId);
            List<Page> list = dbHelper.getAllPages();
            pages.postValue(list);
            
            Page active = activePage.getValue();
            if (active != null && active.getId() == pageId) {
                if (!list.isEmpty()) {
                    loadPage(list.get(0).getId());
                } else {
                    activePage.postValue(null);
                    activePageBlocks.postValue(new ArrayList<>());
                }
            }
        });
    }

    public void addBlockToActivePage(Block block) {
        Page current = activePage.getValue();
        if (current == null) return;
        block.setPageId(current.getId());
        
        executor.execute(() -> {
            dbHelper.createBlock(block);
            List<Block> blocks = dbHelper.getBlocksForPage(current.getId());
            activePageBlocks.postValue(blocks);
        });
    }

    public void updateBlock(Block block) {
        executor.execute(() -> {
            dbHelper.updateBlock(block);
            List<Block> currentList = activePageBlocks.getValue();
            if (currentList != null) {
                for (int i = 0; i < currentList.size(); i++) {
                    if (currentList.get(i).getId() == block.getId()) {
                        currentList.set(i, block);
                        break;
                    }
                }
                activePageBlocks.postValue(new ArrayList<>(currentList));
            }
        });
    }

    public void deleteBlock(Block block) {
        executor.execute(() -> {
            dbHelper.deleteBlock(block.getId());
            Page current = activePage.getValue();
            if (current != null) {
                List<Block> blocks = dbHelper.getBlocksForPage(current.getId());
                activePageBlocks.postValue(blocks);
            }
        });
    }

    public void reorderBlocks(List<Block> reorderedList) {
        activePageBlocks.postValue(new ArrayList<>(reorderedList));
        
        executor.execute(() -> {
            dbHelper.saveAllBlocksOrder(reorderedList);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
