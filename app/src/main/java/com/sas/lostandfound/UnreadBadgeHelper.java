package com.sas.lostandfound;

import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnreadBadgeHelper {

    public interface UnreadCountCallback {
        void onCountRetrieved(long count);
    }

    public static void fetchUnreadCount(String universityId, UnreadCountCallback callback) {
        if (universityId == null || universityId.isEmpty()) {
            if (callback != null) callback.onCountRetrieved(0);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("p_user_id", universityId);

        SupabaseDatabaseHelper.rpc("get_user_conversations", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    List<Conversation> list = new com.google.gson.Gson().fromJson(result, new TypeToken<List<Conversation>>(){}.getType());
                    long totalUnread = 0;
                    if (list != null) {
                        for (Conversation c : list) {
                            totalUnread += c.getUnreadCount();
                        }
                    }
                    if (callback != null) {
                        callback.onCountRetrieved(totalUnread);
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onCountRetrieved(0);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (callback != null) callback.onCountRetrieved(0);
            }
        });
    }

    public static void sendBadgeUpdateBroadcast(android.content.Context context) {
        if (context == null) return;
        android.content.Intent broadcastIntent = new android.content.Intent("com.sas.lostandfound.UPDATE_BADGES");
        broadcastIntent.setPackage(context.getPackageName());
        context.sendBroadcast(broadcastIntent);
    }
}
