package com.allencomm.www.locationknowledgebase.services;

import android.app.Notification;
import android.app.Service;

/**
 * Created by josiahb on 6/6/2017.
 */

import java.util.LinkedList;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.os.IBinder;

import com.gimbal.android.Communication;
import com.gimbal.android.CommunicationListener;
import com.gimbal.android.CommunicationManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Push;
import com.gimbal.android.Visit;
import com.allencomm.www.locationknowledgebase.Home;
import com.allencomm.www.locationknowledgebase.data_access.GimbalDAO;
import com.allencomm.www.locationknowledgebase.events.GimbalEvent;
import com.allencomm.www.locationknowledgebase.events.GimbalEvent.TYPE;

public class GimbalService extends Service {
    public static final String ACTION_GIMBALSERVICE_STARTED = "gimbalservice_started";

    private static final int MAX_NUM_EVENTS = 100;
    private LinkedList<GimbalEvent> events;
    private PlaceEventListener placeEventListener;
    private CommunicationListener communicationListener;

    public void onCreate() {
        events = new LinkedList<GimbalEvent>(GimbalDAO.getEvents(getApplicationContext()));

        Gimbal.setApiKey(this.getApplication(), "8d45ca992bfa355256e3e383a6187ea3");

        placeEventListener = new PlaceEventListener() {
            @Override
            public void onVisitStart(Visit visit) {
                addEvent(new GimbalEvent(TYPE.PLACE_ENTER, visit.getPlace().getName(), new Date(visit.getArrivalTimeInMillis())));
            }

            @Override
            public void onVisitStartWithDelay(Visit visit, int delayTimeInSeconds) {
                if (delayTimeInSeconds > 0) {
                    addEvent(new GimbalEvent(TYPE.PLACE_ENTER_DELAY, visit.getPlace().getName(), new Date(System.currentTimeMillis())));
                }
            }

            @Override
            public void onVisitEnd(Visit visit) {
                addEvent(new GimbalEvent(TYPE.PLACE_EXIT, visit.getPlace().getName(), new Date(visit.getDepartureTimeInMillis())));
            }
        };
        PlaceManager.getInstance().addListener(placeEventListener);

        communicationListener = new CommunicationListener() {
            @Override
            public Notification.Builder prepareCommunicationForDisplay(Communication communication, Visit visit, int notificationId) {
                addEvent(new GimbalEvent(TYPE.COMMUNICATION_PRESENTED, communication.getTitle() + ": CONTENT_DELIVERED", new Date()));

                return null;
            }

            @Override
            public Notification.Builder prepareCommunicationForDisplay(Communication communication, Push push, int notificationId) {
                addEvent(new GimbalEvent(TYPE.COMMUNICATION_INSTANT_PUSH, communication.getTitle() + ": CONTENT_DELIVERED", new Date()));

                return null;
            }

            @Override
            public void onNotificationClicked(List<Communication> communications) {
                for (Communication communication : communications) {
                    if (communication != null) {
                        addEvent(new GimbalEvent(TYPE.NOTIFICATION_CLICKED, communication.getTitle() + ": CONTENT_CLICKED", new Date()));

                        Intent intent = new Intent(getApplicationContext(), Home.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            }
        };
        CommunicationManager.getInstance().addListener(communicationListener);
    }

    private void addEvent(GimbalEvent event) {
        while (events.size() >= MAX_NUM_EVENTS) {
            events.removeLast();
        }
        events.add(0, event);
        GimbalDAO.setEvents(getApplicationContext(), events);
    }

    public void notifyServiceStarted() {
        Intent intent = new Intent(ACTION_GIMBALSERVICE_STARTED);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        PlaceManager.getInstance().removeListener(placeEventListener);
        CommunicationManager.getInstance().removeListener(communicationListener);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
