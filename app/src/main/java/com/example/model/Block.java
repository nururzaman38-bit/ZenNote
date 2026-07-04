package com.example.model;

public abstract class Block {
    public static final String TYPE_PARAGRAPH = "PARAGRAPH";
    public static final String TYPE_TASK_GRID = "TASK_GRID";
    public static final String TYPE_LINK = "LINK";
    public static final String TYPE_PASSWORD = "PASSWORD";
    public static final String TYPE_MEDIA = "MEDIA";

    private long id;
    private long pageId;
    private String type;
    private int position;

    public Block(String type) {
        this.type = type;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPageId() { return pageId; }
    public void setPageId(long pageId) { this.pageId = pageId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public abstract String serializeContent();
    public abstract void deserializeContent(String json);
}
