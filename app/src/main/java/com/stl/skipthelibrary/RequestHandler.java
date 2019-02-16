package com.stl.skipthelibrary;

import java.util.ArrayList;

/**
 * Created by Luke Slevinsky on 2019-02-15.
 */
public class RequestHandler {
    private State state;
    private ArrayList<String> pendingRequestors; // User name
    private String acceptedRequestor; // User name

    public RequestHandler(State state) {
        this(state,new ArrayList<String>(), null);

    }

    public RequestHandler(State state, ArrayList<String> pendingRequestors, String acceptedRequestor) {
        this.state = state;
        this.pendingRequestors = pendingRequestors;
        this.acceptedRequestor = acceptedRequestor;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public ArrayList<String> getPendingRequestors() {
        return pendingRequestors;
    }

    public void setPendingRequestors(ArrayList<String> pendingRequestors) {
        this.pendingRequestors = pendingRequestors;
    }

    public String getAcceptedRequestor() {
        return acceptedRequestor;
    }

    public void setAcceptedRequestor(String acceptedRequestor) {
        this.acceptedRequestor = acceptedRequestor;
    }

}