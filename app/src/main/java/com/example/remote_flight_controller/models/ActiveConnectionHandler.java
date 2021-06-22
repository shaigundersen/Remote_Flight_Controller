package com.example.remote_flight_controller.models;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class ActiveConnectionHandler {
    private static Socket socketFG;
    private static PrintWriter out;
    private final BlockingQueue<Runnable> jobQueue = new LinkedBlockingQueue<Runnable>();
    private boolean keepAlive;
    private Thread worker;
    private PropertyChangeSupport changes = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changes.addPropertyChangeListener(l);
    }
    public void removePropertyChangeListener(PropertyChangeListener l) {
        changes.removePropertyChangeListener(l);
    }
    public ActiveConnectionHandler(final String serverIp,final int serverPort) {
        this.keepAlive = true;
        worker = new Thread(()->{
            try {
                socketFG = new Socket(serverIp, serverPort);
                // if socket successfully opened notify any listener (ViewModel)
                changes.firePropertyChange("connected",false,true);
                out = new PrintWriter(socketFG.getOutputStream(),true);
            } catch (IOException e) {
                System.out.println("socket Error");
            }
        });
        worker.start();
        new Thread(()->{
            try {
                // wait for the socket opening thread to finish
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(socketFG != null && socketFG.isConnected()){
                // Connected! now do jobs sequentially
                while(keepAlive){
                    try {
                        System.out.println("waiting for a job..");
                        jobQueue.take().run();
                    } catch (InterruptedException ignored){}
                }
            }
        }).start();
    }

    public void disconnectFromFG() throws InterruptedException {
        // have the thread do a work to finish while loop, that way we don't need to join it
        jobQueue.put(() -> {
            keepAlive = false;
            try {
                socketFG.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void setAileron(final double value) throws InterruptedException {
        jobQueue.put(() -> {
            out.print("set /controls/flight/aileron "+value+"\r\n");
            out.flush();
        });
    }
    public void setElevator(double value) throws InterruptedException {
        jobQueue.put(() -> {
            out.print("set /controls/flight/elevator "+value+"\r\n");
            out.flush();
        });
    }

    public void setThrottle(double value) throws InterruptedException {
        jobQueue.put(() -> {
            out.print("set /controls/engines/current-engine/throttle "+value+"\r\n");
            out.flush();
        });
    }

    public void setRudder(double value) throws InterruptedException {
        jobQueue.put(() -> {
            out.print("set /controls/flight/rudder "+value+"\r\n");
            out.flush();
        });
    }

}
