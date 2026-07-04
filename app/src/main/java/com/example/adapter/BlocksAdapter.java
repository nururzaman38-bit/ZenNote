package com.example.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Block;
import com.example.model.ParagraphBlock;
import com.example.model.TaskGridBlock;
import com.example.model.LinkBookmarkBlock;
import com.example.model.PasswordVaultBlock;
import com.example.model.MediaAttachmentBlock;
import com.example.utils.EncryptionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlocksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Block> blockList = new ArrayList<>();
    private final BlockInteractionListener listener;

    public interface BlockInteractionListener {
        void onBlockUpdated(Block block);
        void onBlockDeleted(Block block);
        void onSelectMedia(MediaAttachmentBlock block);
        void onAuthenticatePassword(PasswordVaultBlock block, Runnable onSuccess);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public BlocksAdapter(BlockInteractionListener listener) {
        this.listener = listener;
    }

    public void setBlocks(List<Block> blocks) {
        this.blockList.clear();
        this.blockList.addAll(blocks);
        notifyDataSetChanged();
    }

    public List<Block> getBlocks() {
        return blockList;
    }

    @Override
    public int getItemViewType(int position) {
        Block block = blockList.get(position);
        switch (block.getType()) {
            case Block.TYPE_PARAGRAPH:
                return 0;
            case Block.TYPE_TASK_GRID:
                return 1;
            case Block.TYPE_LINK:
                return 2;
            case Block.TYPE_PASSWORD:
                return 3;
            case Block.TYPE_MEDIA:
                return 4;
            default:
                return 0;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case 0:
                return new ParagraphViewHolder(inflater.inflate(R.layout.item_block_paragraph, parent, false));
            case 1:
                return new TaskGridViewHolder(inflater.inflate(R.layout.item_block_task_grid, parent, false));
            case 2:
                return new LinkViewHolder(inflater.inflate(R.layout.item_block_link, parent, false));
            case 3:
                return new PasswordViewHolder(inflater.inflate(R.layout.item_block_password, parent, false));
            case 4:
                return new MediaViewHolder(inflater.inflate(R.layout.item_block_media, parent, false));
            default:
                return new ParagraphViewHolder(inflater.inflate(R.layout.item_block_paragraph, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Block block = blockList.get(position);
        switch (getItemViewType(position)) {
            case 0:
                ((ParagraphViewHolder) holder).bind((ParagraphBlock) block);
                break;
            case 1:
                ((TaskGridViewHolder) holder).bind((TaskGridBlock) block);
                break;
            case 2:
                ((LinkViewHolder) holder).bind((LinkBookmarkBlock) block);
                break;
            case 3:
                ((PasswordViewHolder) holder).bind((PasswordVaultBlock) block);
                break;
            case 4:
                ((MediaViewHolder) holder).bind((MediaAttachmentBlock) block);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return blockList.size();
    }

    // --- VIEW HOLDERS ---

    // 1. Paragraph View Holder
    class ParagraphViewHolder extends RecyclerView.ViewHolder {
        final EditText etText;
        final ImageButton btnDelete;
        final ImageView imgDrag;
        boolean isBinding = false;

        ParagraphViewHolder(@NonNull View itemView) {
            super(itemView);
            etText = itemView.findViewById(R.id.et_paragraph_text);
            btnDelete = itemView.findViewById(R.id.btn_delete_block);
            imgDrag = itemView.findViewById(R.id.img_drag_handle);
        }

        void bind(ParagraphBlock block) {
            isBinding = true;
            etText.setText(block.getText());
            isBinding = false;

            etText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isBinding) return;
                    block.setText(s.toString());
                    listener.onBlockUpdated(block);
                }
            });

            btnDelete.setOnClickListener(v -> listener.onBlockDeleted(block));
            imgDrag.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(this);
                }
                return false;
            });
        }
    }

    // 2. Task Grid View Holder
    class TaskGridViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout llRowsContainer;
        final Button btnAddRow;
        final ImageButton btnDelete;
        final ImageView imgDrag;

        TaskGridViewHolder(@NonNull View itemView) {
            super(itemView);
            llRowsContainer = itemView.findViewById(R.id.ll_grid_rows);
            btnAddRow = itemView.findViewById(R.id.btn_add_grid_row);
            btnDelete = itemView.findViewById(R.id.btn_delete_block);
            imgDrag = itemView.findViewById(R.id.img_drag_handle);
        }

        void bind(TaskGridBlock block) {
            btnDelete.setOnClickListener(v -> listener.onBlockDeleted(block));
            imgDrag.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(this);
                }
                return false;
            });

            btnAddRow.setOnClickListener(v -> {
                block.getRows().add(new TaskGridBlock.TaskGridRow());
                listener.onBlockUpdated(block);
                bind(block); // re-bind to rebuild UI
            });

            rebuildGridRows(block);
        }

        private void rebuildGridRows(TaskGridBlock block) {
            llRowsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

            for (int i = 0; i < block.getRows().size(); i++) {
                final int rowIndex = i;
                final TaskGridBlock.TaskGridRow row = block.getRows().get(rowIndex);
                View rowView = inflater.inflate(R.layout.item_grid_row, llRowsContainer, false);

                TextView tvNum = rowView.findViewById(R.id.tv_row_number);
                CheckBox cbDone = rowView.findViewById(R.id.cb_done);
                EditText etDesc = rowView.findViewById(R.id.et_task_desc);
                EditText etCategory = rowView.findViewById(R.id.et_task_category);
                ImageButton btnDeleteRow = rowView.findViewById(R.id.btn_delete_row);

                // Auto Numbering
                tvNum.setText(String.valueOf(rowIndex + 1));

                // Bind checkbox
                cbDone.setChecked(row.isChecked());
                applyCompletionStyling(rowView, row.isChecked(), etDesc, etCategory);

                cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    row.setChecked(isChecked);
                    applyCompletionStyling(rowView, isChecked, etDesc, etCategory);
                    listener.onBlockUpdated(block);
                });

                // Populate cells
                if (row.getCells().size() > 0) {
                    etDesc.setText(row.getCells().get(0));
                }
                if (row.getCells().size() > 1) {
                    etCategory.setText(row.getCells().get(1));
                }

                // Add text change listeners
                etDesc.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) {
                        while (row.getCells().size() < 1) row.getCells().add("");
                        row.getCells().set(0, s.toString());
                        listener.onBlockUpdated(block);
                    }
                });

                etCategory.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) {
                        while (row.getCells().size() < 2) row.getCells().add("");
                        row.getCells().set(1, s.toString());
                        listener.onBlockUpdated(block);
                    }
                });

                // ENTER KEY triggers auto new row
                View.OnKeyListener enterKeyListener = (v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        // Insert a new blank row directly after the current row
                        block.getRows().add(rowIndex + 1, new TaskGridBlock.TaskGridRow());
                        listener.onBlockUpdated(block);
                        bind(block); // Redraw the component
                        
                        // Focus the newly added description edittext on next frame
                        llRowsContainer.post(() -> {
                            if (rowIndex + 1 < llRowsContainer.getChildCount()) {
                                View nextRow = llRowsContainer.getChildAt(rowIndex + 1);
                                EditText nextDesc = nextRow.findViewById(R.id.et_task_desc);
                                if (nextDesc != null) {
                                    nextDesc.requestFocus();
                                }
                            }
                        });
                        return true;
                    }
                    return false;
                };
                etDesc.setOnKeyListener(enterKeyListener);
                etCategory.setOnKeyListener(enterKeyListener);

                btnDeleteRow.setOnClickListener(v -> {
                    if (block.getRows().size() > 1) {
                        block.getRows().remove(rowIndex);
                    } else {
                        // Reset the single remaining row to empty
                        block.getRows().set(0, new TaskGridBlock.TaskGridRow());
                    }
                    listener.onBlockUpdated(block);
                    bind(block);
                });

                llRowsContainer.addView(rowView);
            }
        }

        private void applyCompletionStyling(View rowView, boolean isChecked, EditText etDesc, EditText etCategory) {
            if (isChecked) {
                // Task completion strike-through
                etDesc.setPaintFlags(etDesc.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                etCategory.setPaintFlags(etCategory.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                // Opacity to 50%
                rowView.setAlpha(0.5f);
            } else {
                // Clear strike-through
                etDesc.setPaintFlags(etDesc.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                etCategory.setPaintFlags(etCategory.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                // Opacity to 100%
                rowView.setAlpha(1.0f);
            }
        }
    }

    // 3. Link Bookmarks View Holder
    class LinkViewHolder extends RecyclerView.ViewHolder {
        final EditText etSiteName;
        final EditText etUrl;
        final ImageButton btnVisit;
        final ImageButton btnDelete;
        final ImageView imgDrag;
        boolean isBinding = false;

        LinkViewHolder(@NonNull View itemView) {
            super(itemView);
            etSiteName = itemView.findViewById(R.id.et_site_name);
            etUrl = itemView.findViewById(R.id.et_url);
            btnVisit = itemView.findViewById(R.id.btn_visit_site);
            btnDelete = itemView.findViewById(R.id.btn_delete_block);
            imgDrag = itemView.findViewById(R.id.img_drag_handle);
        }

        void bind(LinkBookmarkBlock block) {
            isBinding = true;
            etSiteName.setText(block.getSiteName());
            etUrl.setText(block.getUrl());
            isBinding = false;

            etSiteName.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isBinding) return;
                    block.setSiteName(s.toString());
                    listener.onBlockUpdated(block);
                }
            });

            etUrl.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isBinding) return;
                    block.setUrl(s.toString());
                    listener.onBlockUpdated(block);
                }
            });

            btnVisit.setOnClickListener(v -> {
                // Trigger opening browser logic
                String url = block.getUrl();
                if (!url.isEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    itemView.getContext().startActivity(intent);
                }
            });

            btnDelete.setOnClickListener(v -> listener.onBlockDeleted(block));
            imgDrag.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(this);
                }
                return false;
            });
        }
    }

    // 4. Password Vault View Holder
    class PasswordViewHolder extends RecyclerView.ViewHolder {
        final EditText etAccount;
        final EditText etUsername;
        final EditText etPassword;
        final ImageButton btnLockStatus;
        final Button btnReveal;
        final ImageButton btnDelete;
        final ImageView imgDrag;
        boolean isBinding = false;
        boolean revealed = false;

        PasswordViewHolder(@NonNull View itemView) {
            super(itemView);
            etAccount = itemView.findViewById(R.id.et_vault_account);
            etUsername = itemView.findViewById(R.id.et_vault_username);
            etPassword = itemView.findViewById(R.id.et_vault_password);
            btnLockStatus = itemView.findViewById(R.id.btn_lock_status);
            btnReveal = itemView.findViewById(R.id.btn_reveal_password);
            btnDelete = itemView.findViewById(R.id.btn_delete_block);
            imgDrag = itemView.findViewById(R.id.img_drag_handle);
        }

        void bind(PasswordVaultBlock block) {
            isBinding = true;
            etAccount.setText(block.getAccount());
            etUsername.setText(block.getUsername());
            
            // If locked, mask the password always
            if (block.isLocked()) {
                btnLockStatus.setImageResource(R.drawable.ic_lock);
                etPassword.setText("••••••••");
                etPassword.setEnabled(false);
                btnReveal.setText("REVEAL");
                revealed = false;
            } else {
                btnLockStatus.setImageResource(R.drawable.ic_unlock);
                etPassword.setEnabled(true);
                if (revealed) {
                    String decrypted = EncryptionUtils.decrypt(block.getPassword());
                    etPassword.setText(decrypted);
                    btnReveal.setText("HIDE");
                } else {
                    etPassword.setText("••••••••");
                    btnReveal.setText("REVEAL");
                }
            }
            isBinding = false;

            etAccount.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isBinding) return;
                    block.setAccount(s.toString());
                    listener.onBlockUpdated(block);
                }
            });

            etUsername.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isBinding) return;
                    block.setUsername(s.toString());
                    listener.onBlockUpdated(block);
                }
            });

            etPassword.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isBinding || block.isLocked() || !revealed) return;
                    String encrypted = EncryptionUtils.encrypt(s.toString());
                    block.setPassword(encrypted);
                    listener.onBlockUpdated(block);
                }
            });

            btnLockStatus.setOnClickListener(v -> {
                block.setLocked(!block.isLocked());
                if (block.isLocked()) {
                    revealed = false;
                }
                listener.onBlockUpdated(block);
                bind(block);
            });

            btnReveal.setOnClickListener(v -> {
                if (revealed) {
                    revealed = false;
                    bind(block);
                } else {
                    // Requires biometric authentication
                    listener.onAuthenticatePassword(block, () -> {
                        // On success:
                        revealed = true;
                        block.setLocked(false); // unlock if they authenticated successfully
                        listener.onBlockUpdated(block);
                        bind(block);
                    });
                }
            });

            btnDelete.setOnClickListener(v -> listener.onBlockDeleted(block));
            imgDrag.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(this);
                }
                return false;
            });
        }
    }

    // 5. Media Block View Holder
    class MediaViewHolder extends RecyclerView.ViewHolder {
        final View selectContainer;
        final View previewContainer;
        final ImageView imgPreview;
        final TextView tvFileName;
        final Button btnChange;
        final ImageButton btnDelete;
        final ImageView imgDrag;

        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            selectContainer = itemView.findViewById(R.id.ll_select_container);
            previewContainer = itemView.findViewById(R.id.ll_preview_container);
            imgPreview = itemView.findViewById(R.id.img_preview);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            btnChange = itemView.findViewById(R.id.btn_change_media);
            btnDelete = itemView.findViewById(R.id.btn_delete_block);
            imgDrag = itemView.findViewById(R.id.img_drag_handle);
        }

        void bind(MediaAttachmentBlock block) {
            if (block.getUri() == null || block.getUri().isEmpty()) {
                selectContainer.setVisibility(View.VISIBLE);
                previewContainer.setVisibility(View.GONE);
            } else {
                selectContainer.setVisibility(View.GONE);
                previewContainer.setVisibility(View.VISIBLE);
                
                tvFileName.setText(block.getFileName());
                // Set image URI using simple built-in Android URI rendering or fallback to placeholder icon
                try {
                    imgPreview.setImageURI(android.net.Uri.parse(block.getUri()));
                } catch (Exception e) {
                    e.printStackTrace();
                    imgPreview.setImageResource(R.drawable.ic_media);
                }
            }

            selectContainer.setOnClickListener(v -> listener.onSelectMedia(block));
            btnChange.setOnClickListener(v -> listener.onSelectMedia(block));
            btnDelete.setOnClickListener(v -> listener.onBlockDeleted(block));
            imgDrag.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(this);
                }
                return false;
            });
        }
    }
}
