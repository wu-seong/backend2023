package org.example;

import org.json.JSONObject;

public class JSONMessageWrapper {
    public JSONMessageWrapper(User user, JSONObject jsonObject) {
        this.user = user;
        this.jsonObject = jsonObject;
    }

    private User user;

    public User getUser() {
        return user;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    private JSONObject jsonObject;
}
