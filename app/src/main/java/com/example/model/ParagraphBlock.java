package com.example.model;

import org.json.JSONObject;

public class ParagraphBlock extends Block {
    private String text = "";

    public ParagraphBlock() {
        super(TYPE_PARAGRAPH);
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    @Override
    public String serializeContent() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("text", text);
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
            this.text = obj.optString("text", "");
        } catch (Exception e) {
            this.text = "";
        }
    }
}
