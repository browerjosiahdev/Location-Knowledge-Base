package com.allencomm.www.locationknowledgebase.events;

/**
 * Created by josiahb on 6/6/2017.
 */

import java.io.Serializable;
import java.util.Date;

public class GimbalEvent implements Serializable {
    public static enum TYPE {
        PLACE_ENTER, PLACE_ENTER_DELAY, PLACE_EXIT, COMMUNICATION_PRESENTED, COMMUNICATION_ENTER, COMMUNICATION_EXIT,
        COMMUNICATION_INSTANT_PUSH, COMMUNICATION_TIME_PUSH, APP_INSTANCE_ID_RESET, COMMUNICATION, NOTIFICATION_CLICKED
    }

    private TYPE type;
    private String title;
    private Date date;

    public GimbalEvent() {

    }

    public GimbalEvent(TYPE type, String title, Date date) {
        this.type = type;
        this.title = title;
        this.date = date;
    }

    public TYPE getType() { return type; }
    public void setType(TYPE type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
}
