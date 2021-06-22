package com.example.remote_flight_controller.views;
@FunctionalInterface
public interface JoystickChanged {
    void onChange(double aileron, double elevator);
}
