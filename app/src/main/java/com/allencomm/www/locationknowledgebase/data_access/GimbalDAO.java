package com.allencomm.www.locationknowledgebase.data_access;

/**
 * Created by josiahb on 6/6/2017.
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.allencomm.www.locationknowledgebase.events.GimbalEvent;
import com.allencomm.www.locationknowledgebase.events.GimbalEvent.TYPE;

public class GimbalDAO {
    private static final String ACTION_GIMBALSERVICE_NEWEVENT = "gimbalservice_newevent";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_TYPE = "type";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DATE = "date";

    public static final String PREFERENCE_SHOWOPTIN = "showoptin";

    public static boolean showOptIn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_SHOWOPTIN, true);
    }

    public static void setOptInShown(Context context) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PREFERENCE_SHOWOPTIN, false);
        editor.commit();
    }

    public static List<GimbalEvent> getEvents(Context context) {
        List<GimbalEvent> events = new ArrayList<GimbalEvent>();

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String jsonString = prefs.getString(KEY_EVENTS, null);

            if (jsonString != null) {
                JSONArray jsonArray = new JSONArray(jsonString);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    GimbalEvent event = new GimbalEvent();
                    event.setType(TYPE.valueOf(jsonObject.getString(KEY_TYPE)));
                    event.setTitle(jsonObject.getString(KEY_TITLE));
                    event.setDate(new Date(jsonObject.getLong(KEY_DATE)));
                    events.add(event);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return events;
    }

    public static void setEvents(Context context, List<GimbalEvent> events) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            JSONArray jsonArray = new JSONArray();

            for (GimbalEvent event : events) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(KEY_TYPE, event.getType().name());
                jsonObject.put(KEY_TITLE, event.getTitle());
                jsonObject.put(KEY_DATE, event.getDate().getTime());
                jsonArray.put(jsonObject);
            }

            String jsonString = jsonArray.toString();
            Editor editor = prefs.edit();
            editor.putString(KEY_EVENTS, jsonString);
            editor.commit();

            Intent intent = new Intent();
            intent.setAction(ACTION_GIMBALSERVICE_NEWEVENT);
            context.sendBroadcast(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
