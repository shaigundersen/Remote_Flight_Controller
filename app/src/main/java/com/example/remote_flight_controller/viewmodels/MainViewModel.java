package com.example.remote_flight_controller.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.remote_flight_controller.models.ActiveConnectionHandler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;


public class MainViewModel extends ViewModel implements PropertyChangeListener {

    private MutableLiveData<String> ip = new MutableLiveData<>("");

    private MutableLiveData<String> port = new MutableLiveData<>("");
    private MutableLiveData<String> feedbackPort = new MutableLiveData<>("");
    private MutableLiveData<String> feedbackIp = new MutableLiveData<>("");
    private boolean connected = false;
    private ActiveConnectionHandler activeConnectionHandler;

    public MutableLiveData<String> getFeedbackPort() {
        return feedbackPort;
    }

    public MutableLiveData<String> getFeedbackIp() {
        return feedbackIp;
    }
    public MutableLiveData<String> getIp() {
        return ip;
    }

    public MutableLiveData<String> getPort() {
        return port;
    }

    public void connectToFG(){
        // validation check before connection and inform user on error
        if(!this.validateInput()){
            return;
        }
        feedbackPort.setValue("");
        feedbackIp.setValue("");

        //creates a new Active obj that handle all connection with FG
        this.activeConnectionHandler = new ActiveConnectionHandler(ip.getValue(), Integer.parseInt(port.getValue()));
        // add as a listener to know when thread successfully connected to FG
        this.activeConnectionHandler.addPropertyChangeListener(this);
    }

    public void disconnectFromFG(){
        if(this.activeConnectionHandler == null)
            return;
        try {
            this.activeConnectionHandler.disconnectFromFG();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setAileron(double value){
        if(this.activeConnectionHandler == null)
            return;
        try {
            this.activeConnectionHandler.setAileron(value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setElevator(double value) {
        if(this.activeConnectionHandler == null)
            return;
        try {
            this.activeConnectionHandler.setElevator(value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void setThrottle(double value) {
        if(this.activeConnectionHandler == null)
            return;
        try {
            this.activeConnectionHandler.setThrottle(value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void setRudder(double value) {
        if(this.activeConnectionHandler == null)
            return;
        try {
            this.activeConnectionHandler.setRudder(value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        // listens on the event of successful connection to FG server
        if("connected".equals(propertyName)){
            this.connected = true;
        }
    }
    // check if ip/port are of valid type
    // notify user if not by changing the relevant field's color
    private boolean validateInput(){
        // fresh start
        feedbackPort.setValue("");
        feedbackIp.setValue("");
        boolean flag = true;
        // check if user entered some value
        if(ip.getValue().equals("")){
            feedbackIp.setValue("invalid ip");
            flag = false;
        }
        // check if ip is of form XXX.XXX.XXX.XXX
        if(!ip.getValue().matches("((0|[1-9][0-9]?|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}(0|[1-9][0-9]?|1[0-9][0-9]|2[0-4][0-9]|25[0-5])")){
            feedbackIp.setValue("invalid ip");
            flag = false;
        }
        if(port.getValue().equals("")){
            feedbackPort.setValue("invalid port");
            flag = false;
        }
        int intPort=0;
        // check if port is digits only
        try{
            intPort=Integer.parseInt(port.getValue());
        }catch (NumberFormatException e){
            feedbackPort.setValue("invalid port");
            flag = false;
        }
        // check if port is valid value
        if(!(1<=intPort && intPort<=65535)){
            feedbackPort.setValue("invalid port");
            flag = false;
        }
        return flag;
    }

    public boolean getConnected() {
        return this.connected;
    }
}