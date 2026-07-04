package com.example.model;

import org.json.JSONObject;

public class LinkBookmarkBlock extends Block {
    private String siteName = "";
    private String url = "";

    public LinkBookmarkBlock() {
        super(TYPE_LINK);
    }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    @Override
    public String serializeContent() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("siteName", siteName);
            obj.put("url", url);
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
            this.siteName = obj.optString("siteName", "");
            this.url = obj.optString("url", "");
        } catch (Exception e) {
            this.siteName = "";
            this.url = "";
        }
    }
}
