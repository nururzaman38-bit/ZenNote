package com.example.model;

import org.json.JSONObject;

public class MediaAttachmentBlock extends Block {
    private String uri = "";
    private String fileName = "";

    public MediaAttachmentBlock() {
        super(TYPE_MEDIA);
    }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    @Override
    public String serializeContent() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("uri", uri);
            obj.put("fileName", fileName);
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public void deserializeContent(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(json);
            this.uri = obj.optString("uri", "");
            this.fileName = obj.optString("fileName", "");
        } catch (Exception e) {
            this.uri = "";
            this.fileName = "";
        }
    }
}
