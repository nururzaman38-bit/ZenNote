package com.example.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.model.Block;
import com.example.model.Page;
import com.example.model.ParagraphBlock;
import com.example.model.TaskGridBlock;
import com.example.model.LinkBookmarkBlock;
import com.example.model.PasswordVaultBlock;
import com.example.model.MediaAttachmentBlock;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "zennote.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_PAGES = "pages";
    private static final String TABLE_BLOCKS = "blocks";

    // Common column names
    private static final String KEY_ID = "id";

    // Pages Table Columns
    private static final String KEY_PAGE_TITLE = "title";
    private static final String KEY_PAGE_CREATED_AT = "created_at";

    // Blocks Table Columns
    private static final String KEY_BLOCK_PAGE_ID = "page_id";
    private static final String KEY_BLOCK_TYPE = "type";
    private static final String KEY_BLOCK_POSITION = "position";
    private static final String KEY_BLOCK_CONTENT = "content";

    // Table Create Statements
    private static final String CREATE_TABLE_PAGES = "CREATE TABLE " + TABLE_PAGES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_PAGE_TITLE + " TEXT,"
            + KEY_PAGE_CREATED_AT + " INTEGER"
            + ")";

    private static final String CREATE_TABLE_BLOCKS = "CREATE TABLE " + TABLE_BLOCKS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_BLOCK_PAGE_ID + " INTEGER,"
            + KEY_BLOCK_TYPE + " TEXT,"
            + KEY_BLOCK_POSITION + " INTEGER,"
            + KEY_BLOCK_CONTENT + " TEXT,"
            + "FOREIGN KEY(" + KEY_BLOCK_PAGE_ID + ") REFERENCES " + TABLE_PAGES + "(" + KEY_ID + ") ON DELETE CASCADE"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PAGES);
        db.execSQL(CREATE_TABLE_BLOCKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLOCKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGES);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Pages CRUD
    public long createPage(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PAGE_TITLE, title);
        values.put(KEY_PAGE_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_PAGES, null, values);
    }

    public List<Page> getAllPages() {
        List<Page> pages = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PAGES + " ORDER BY " + KEY_PAGE_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c.moveToFirst()) {
            do {
                Page page = new Page();
                page.setId(c.getLong(c.getColumnIndexOrThrow(KEY_ID)));
                page.setTitle(c.getString(c.getColumnIndexOrThrow(KEY_PAGE_TITLE)));
                page.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(KEY_PAGE_CREATED_AT)));
                pages.add(page);
            } while (c.moveToNext());
        }
        c.close();
        return pages;
    }

    public Page getPageById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_PAGES, null, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        Page page = null;
        if (c != null && c.moveToFirst()) {
            page = new Page(
                c.getLong(c.getColumnIndexOrThrow(KEY_ID)),
                c.getString(c.getColumnIndexOrThrow(KEY_PAGE_TITLE)),
                c.getLong(c.getColumnIndexOrThrow(KEY_PAGE_CREATED_AT))
            );
            c.close();
        }
        return page;
    }

    public void updatePageTitle(long pageId, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PAGE_TITLE, title);
        db.update(TABLE_PAGES, values, KEY_ID + " = ?", new String[]{String.valueOf(pageId)});
    }

    public void deletePage(long pageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PAGES, KEY_ID + " = ?", new String[]{String.valueOf(pageId)});
    }

    // Blocks CRUD
    public long createBlock(Block block) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Find maximum position to insert at the end
        int nextPosition = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(" + KEY_BLOCK_POSITION + ") FROM " + TABLE_BLOCKS + " WHERE " + KEY_BLOCK_PAGE_ID + " = " + block.getPageId(), null);
        if (cursor.moveToFirst()) {
            nextPosition = cursor.getInt(0) + 1;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(KEY_BLOCK_PAGE_ID, block.getPageId());
        values.put(KEY_BLOCK_TYPE, block.getType());
        values.put(KEY_BLOCK_POSITION, nextPosition);
        values.put(KEY_BLOCK_CONTENT, block.serializeContent());

        long id = db.insert(TABLE_BLOCKS, null, values);
        block.setId(id);
        block.setPosition(nextPosition);
        return id;
    }

    public List<Block> getBlocksForPage(long pageId) {
        List<Block> blocks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_BLOCKS + " WHERE " + KEY_BLOCK_PAGE_ID + " = ? ORDER BY " + KEY_BLOCK_POSITION + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, new String[]{String.valueOf(pageId)});

        if (c.moveToFirst()) {
            do {
                String type = c.getString(c.getColumnIndexOrThrow(KEY_BLOCK_TYPE));
                Block block = createPolymorphicBlock(type);
                block.setId(c.getLong(c.getColumnIndexOrThrow(KEY_ID)));
                block.setPageId(c.getLong(c.getColumnIndexOrThrow(KEY_BLOCK_PAGE_ID)));
                block.setPosition(c.getInt(c.getColumnIndexOrThrow(KEY_BLOCK_POSITION)));
                
                String contentJson = c.getString(c.getColumnIndexOrThrow(KEY_BLOCK_CONTENT));
                block.deserializeContent(contentJson);
                
                blocks.add(block);
            } while (c.moveToNext());
        }
        c.close();
        return blocks;
    }

    public void updateBlock(Block block) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BLOCK_CONTENT, block.serializeContent());
        values.put(KEY_BLOCK_POSITION, block.getPosition());
        db.update(TABLE_BLOCKS, values, KEY_ID + " = ?", new String[]{String.valueOf(block.getId())});
    }

    public void deleteBlock(long blockId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BLOCKS, KEY_ID + " = ?", new String[]{String.valueOf(blockId)});
    }

    public void saveAllBlocksOrder(List<Block> blocks) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                block.setPosition(i);
                ContentValues values = new ContentValues();
                values.put(KEY_BLOCK_POSITION, i);
                db.update(TABLE_BLOCKS, values, KEY_ID + " = ?", new String[]{String.valueOf(block.getId())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private Block createPolymorphicBlock(String type) {
        switch (type) {
            case Block.TYPE_PARAGRAPH:
                return new ParagraphBlock();
            case Block.TYPE_TASK_GRID:
                return new TaskGridBlock();
            case Block.TYPE_LINK:
                return new LinkBookmarkBlock();
            case Block.TYPE_PASSWORD:
                return new PasswordVaultBlock();
            case Block.TYPE_MEDIA:
                return new MediaAttachmentBlock();
            default:
                return new ParagraphBlock();
        }
    }
}
