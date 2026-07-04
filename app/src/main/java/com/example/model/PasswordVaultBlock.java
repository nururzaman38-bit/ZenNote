package com.example.model;

import org.json.JSONObject;

public class PasswordVaultBlock extends Block {
    private String account = "";
    private String username = "";
    private String password = ""; // Store AES-encrypted
    private boolean isLocked = true; // Mark as Private/Locked by default

    public PasswordVaultBlock() {
        super(TYPE_PASSWORD);
    }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    @Override
    public String serializeContent() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("account", account);
            obj.put("username", username);
            obj.put("password", password);
            obj.put("isLocked", isLocked);
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
            this.account = obj.optString("account", "");
            this.username = obj.optString("username", "");
            this.password = obj.optString("password", "");
            this.isLocked = obj.optBoolean("isLocked", true);
        } catch (Exception e) {
            this.account = "";
            this.username = "";
            this.password = "";
            this.isLocked = true;
        }
    }
}
