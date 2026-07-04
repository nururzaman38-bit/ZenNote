package com.example;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.BlocksAdapter;
import com.example.adapter.DrawerPagesAdapter;
import com.example.model.Block;
import com.example.model.Page;
import com.example.model.ParagraphBlock;
import com.example.model.TaskGridBlock;
import com.example.model.LinkBookmarkBlock;
import com.example.model.PasswordVaultBlock;
import com.example.model.MediaAttachmentBlock;
import com.example.utils.BlockTouchHelperCallback;
import com.example.viewmodel.MainViewModel;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements BlocksAdapter.BlockInteractionListener {

    private DrawerLayout drawerLayout;
    private MainViewModel viewModel;
    
    private EditText etPageTitle;
    private View emptyState;
    private RecyclerView rvBlocks;
    private BlocksAdapter blocksAdapter;
    private ItemTouchHelper itemTouchHelper;

    private RecyclerView rvDrawerPages;
    private DrawerPagesAdapter drawerAdapter;

    private MediaAttachmentBlock activeMediaBlockForPicking;

    // Media picking launcher
    private final ActivityResultLauncher<Intent> mediaPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && activeMediaBlockForPicking != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Request persistable URI permission to access across device reboots
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        String displayName = getFileNameFromUri(uri);
                        activeMediaBlockForPicking.setUri(uri.toString());
                        activeMediaBlockForPicking.setFileName(displayName);
                        
                        // Update in viewModel
                        viewModel.updateBlock(activeMediaBlockForPicking);
                    }
                }
                activeMediaBlockForPicking = null;
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts
        drawerLayout = findViewById(R.id.drawer_layout);
        etPageTitle = findViewById(R.id.et_page_title);
        emptyState = findViewById(R.id.empty_state);
        rvBlocks = findViewById(R.id.rv_blocks);
        rvDrawerPages = findViewById(R.id.rv_drawer_pages);

        // Set up custom Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Initialize adapters
        blocksAdapter = new BlocksAdapter(this);
        rvBlocks.setLayoutManager(new LinearLayoutManager(this));
        rvBlocks.setAdapter(blocksAdapter);

        // Drag and drop attachment
        BlockTouchHelperCallback callback = new BlockTouchHelperCallback(blocksAdapter, reorderedList -> {
            viewModel.reorderBlocks(reorderedList);
        });
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rvBlocks);

        // Set up Side Drawer Page selection
        drawerAdapter = new DrawerPagesAdapter(new DrawerPagesAdapter.PageClickListener() {
            @Override
            public void onPageClick(Page page) {
                viewModel.loadPage(page.getId());
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            @Override
            public void onPageDelete(Page page) {
                viewModel.deletePage(page.getId());
            }
        });
        rvDrawerPages.setLayoutManager(new LinearLayoutManager(this));
        rvDrawerPages.setAdapter(drawerAdapter);

        // Setup Page Title text watcher for auto-save
        etPageTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                Page active = viewModel.getActivePage().getValue();
                if (active != null && !s.toString().equals(active.getTitle())) {
                    viewModel.updatePageTitle(active.getId(), s.toString());
                }
            }
        });

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Listen for active page changes
        viewModel.getActivePage().observe(this, page -> {
            if (page != null) {
                if (!etPageTitle.getText().toString().equals(page.getTitle())) {
                    etPageTitle.setText(page.getTitle());
                    etPageTitle.setSelection(etPageTitle.getText().length());
                }
            } else {
                etPageTitle.setText("");
            }
        });

        // Listen for blocks list changes
        viewModel.getActivePageBlocks().observe(this, blocks -> {
            if (blocks == null || blocks.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                rvBlocks.setVisibility(View.GONE);
                blocksAdapter.setBlocks(new ArrayList<>());
            } else {
                emptyState.setVisibility(View.GONE);
                rvBlocks.setVisibility(View.VISIBLE);
                blocksAdapter.setBlocks(blocks);
            }
            
            // Refresh pages drawer to reflect any potential updates
            List<Page> currentPages = viewModel.getPages().getValue();
            Page active = viewModel.getActivePage().getValue();
            if (currentPages != null && active != null) {
                drawerAdapter.setPages(currentPages, active.getId());
            }
        });

        // Listen for pages changes
        viewModel.getPages().observe(this, pages -> {
            Page active = viewModel.getActivePage().getValue();
            long activeId = active != null ? active.getId() : -1;
            
            if (pages.isEmpty()) {
                // Generate default starting workspace on first load
                viewModel.createNewPage("Welcome to ZenNote 🌸");
            } else {
                drawerAdapter.setPages(pages, activeId);
                if (activeId == -1 && !pages.isEmpty()) {
                    viewModel.loadPage(pages.get(0).getId());
                }
            }
        });

        // Side drawer create page button
        Button btnCreatePage = findViewById(R.id.btn_create_page);
        btnCreatePage.setOnClickListener(v -> {
            viewModel.createNewPage("Untitled Project Page");
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Created new page", Toast.LENGTH_SHORT).show();
        });

        // Setup Bottom Addition Buttons
        findViewById(R.id.btn_add_paragraph).setOnClickListener(v -> {
            ParagraphBlock block = new ParagraphBlock();
            block.setText("");
            viewModel.addBlockToActivePage(block);
        });

        findViewById(R.id.btn_add_task_grid).setOnClickListener(v -> {
            TaskGridBlock block = new TaskGridBlock();
            viewModel.addBlockToActivePage(block);
        });

        findViewById(R.id.btn_add_link).setOnClickListener(v -> {
            LinkBookmarkBlock block = new LinkBookmarkBlock();
            block.setSiteName("");
            block.setUrl("");
            viewModel.addBlockToActivePage(block);
        });

        findViewById(R.id.btn_add_password).setOnClickListener(v -> {
            PasswordVaultBlock block = new PasswordVaultBlock();
            block.setAccount("");
            block.setUsername("");
            block.setPassword("");
            block.setLocked(true);
            viewModel.addBlockToActivePage(block);
        });

        findViewById(R.id.btn_add_media).setOnClickListener(v -> {
            MediaAttachmentBlock block = new MediaAttachmentBlock();
            block.setUri("");
            block.setFileName("");
            viewModel.addBlockToActivePage(block);
        });
    }

    // --- Block Interaction Listeners ---

    @Override
    public void onBlockUpdated(Block block) {
        viewModel.updateBlock(block);
    }

    @Override
    public void onBlockDeleted(Block block) {
        viewModel.deleteBlock(block);
    }

    @Override
    public void onSelectMedia(MediaAttachmentBlock block) {
        activeMediaBlockForPicking = block;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        mediaPickerLauncher.launch(intent);
    }

    @Override
    public void onAuthenticatePassword(PasswordVaultBlock block, Runnable onSuccess) {
        BiometricManager biometricManager = BiometricManager.from(this);
        int status = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        
        if (status != BiometricManager.BIOMETRIC_SUCCESS) {
            // Devices without hardware biometrics automatically success-reveal as a fallback
            onSuccess.run();
            return;
        }

        Executor mainExecutor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, mainExecutor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                onSuccess.run();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(MainActivity.this, "Authentication failed: " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Password Vault")
                .setSubtitle("Confirm authentication to reveal credential details")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        prompt.authenticate(promptInfo);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    // Helper to get raw file name from selected document Uri
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
