package com.example.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TaskGridBlock extends Block {
    private List<String> headers = new ArrayList<>();
    private List<TaskGridRow> rows = new ArrayList<>();

    public TaskGridBlock() {
        super(TYPE_TASK_GRID);
        // Add default headers
        headers.add("Done");
        headers.add("Task Description");
        headers.add("Category");
        // Add a default blank row
        rows.add(new TaskGridRow());
    }

    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }

    public List<TaskGridRow> getRows() { return rows; }
    public void setRows(List<TaskGridRow> rows) { this.rows = rows; }

    public static class TaskGridRow {
        private boolean checked;
        private List<String> cells = new ArrayList<>();

        public TaskGridRow() {
            this.checked = false;
            // 2 cells by default matching the 2 non-checkbox headers
            this.cells.add("");
            this.cells.add("");
        }

        public TaskGridRow(boolean checked, List<String> cells) {
            this.checked = checked;
            this.cells = cells;
        }

        public boolean isChecked() { return checked; }
        public void setChecked(boolean checked) { this.checked = checked; }

        public List<String> getCells() { return cells; }
        public void setCells(List<String> cells) { this.cells = cells; }
    }

    @Override
    public String serializeContent() {
        try {
            JSONObject obj = new JSONObject();
            
            JSONArray jsonHeaders = new JSONArray();
            for (String h : headers) {
                jsonHeaders.put(h);
            }
            obj.put("headers", jsonHeaders);

            JSONArray jsonRows = new JSONArray();
            for (TaskGridRow r : rows) {
                JSONObject rObj = new JSONObject();
                rObj.put("checked", r.isChecked());
                JSONArray cellsArr = new JSONArray();
                for (String c : r.getCells()) {
                    cellsArr.put(c);
                }
                rObj.put("cells", cellsArr);
                jsonRows.put(rObj);
            }
            obj.put("rows", jsonRows);

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
            
            headers.clear();
            JSONArray jsonHeaders = obj.optJSONArray("headers");
            if (jsonHeaders != null) {
                for (int i = 0; i < jsonHeaders.length(); i++) {
                    headers.add(jsonHeaders.getString(i));
                }
            } else {
                headers.add("Done");
                headers.add("Task Description");
                headers.add("Category");
            }

            rows.clear();
            JSONArray jsonRows = obj.optJSONArray("rows");
            if (jsonRows != null) {
                for (int i = 0; i < jsonRows.length(); i++) {
                    JSONObject rObj = jsonRows.getJSONObject(i);
                    boolean checked = rObj.optBoolean("checked", false);
                    JSONArray cellsArr = rObj.optJSONArray("cells");
                    List<String> cells = new ArrayList<>();
                    if (cellsArr != null) {
                        for (int j = 0; j < cellsArr.length(); j++) {
                            cells.add(cellsArr.optString(j, ""));
                        }
                    }
                    rows.add(new TaskGridRow(checked, cells));
                }
            }
            if (rows.isEmpty()) {
                rows.add(new TaskGridRow());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
