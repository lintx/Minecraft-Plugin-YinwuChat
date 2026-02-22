package org.lintx.plugins.yinwuchat.velocity.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.lintx.plugins.yinwuchat.Util.Gson;

/**
 * WebSocket 输入消息基类
 */
public class InputBase {
    
    public static InputBase getObject(String json) {
        try {
            JsonElement jsonTree = JsonParser.parseString(json);
            if (jsonTree.isJsonObject()) {
                JsonObject object = jsonTree.getAsJsonObject();
                JsonElement actionElement = object.get("action");
                if (actionElement != null) {
                    String action = actionElement.getAsString();
                    if (action.equalsIgnoreCase("check_token")) {
                        return new InputCheckToken(object.get("token").getAsString());
                    } else if (action.equalsIgnoreCase("send_message")) {
                        return new InputMessage(object.get("message").getAsString());
                    } else if (action.equalsIgnoreCase("command")) {
                        return new InputCommand(object.get("command").getAsString());
                    } else if (action.equalsIgnoreCase("private_message")) {
                        return new InputPrivateMessage(object.get("to").getAsString(), object.get("message").getAsString());
                    } else if (action.equalsIgnoreCase("bind_account")) {
                        return new InputBindAccount(object.get("account").getAsString());
                    } else if (action.equalsIgnoreCase("read_cursor")) {
                        String chat = object.has("chat") ? object.get("chat").getAsString() : "";
                        long messageId = object.has("message_id") ? object.get("message_id").getAsLong() : 0L;
                        return new InputReadCursor(chat, messageId);
                    }
                } else {
                    JsonElement postTypeElement = object.get("post_type");
                    if (postTypeElement != null) {
                        try {
                            InputAQQBot inputModel = Gson.gson().fromJson(json, new TypeToken<InputAQQBot>(){}.getType());
                            return inputModel;
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
