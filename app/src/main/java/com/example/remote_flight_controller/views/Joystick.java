package com.example.remote_flight_controller.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.remote_flight_controller.R;


public class Joystick extends View {
    public JoystickChanged notifyService;

    private int outerCircleCenterX;
    private int outerCircleCenterY;
    private int innerCircleCenterX;
    private int innerCircleCenterY;

    private int outerCircleRadius;
    private int innerCircleRadius;

    private Paint outerCirclePaint;
    private Paint innerCirclePaint;

    private boolean pressed;
    // ranges (-1,1) to know % of change in inner circle center with respect to outer circle center
    private double actuatorX;
    private double actuatorY;

    public Joystick(Context context) {
        super(context);
        init(null);
        setFocusable(true);


    }

    public Joystick(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Joystick(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet set) {

        this.outerCirclePaint = new Paint();
        this.outerCirclePaint.setColor(Color.LTGRAY);
        this.outerCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.innerCirclePaint = new Paint();
        this.innerCirclePaint.setColor(getResources().getColor(R.color.android_default));
        this.innerCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        this.pressed = false;

    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setPositions(w,h);
    }

    // util func to set positions of joystick relative to it's parent
    private void setPositions(int width, int height){
        this.outerCircleCenterX = this.innerCircleCenterX = width / 2;
        this.outerCircleCenterY = this.innerCircleCenterY = height / 2;
        // set radius to be min so it won't exceed boundaries
        int radius = Math.min(width, height);
        this.outerCircleRadius = radius / 5;
        this.innerCircleRadius = this.outerCircleRadius / 2;
    }

    public boolean getIsPressed() {
        return pressed;
    }

    public void setIsPressed(boolean pressed) {
        this.pressed = pressed;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(this.isPressed((double)event.getX(), (double)event.getY()))
                    this.setIsPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if(this.getIsPressed())
                    this.setActuator((double)event.getX(), (double)event.getY());
                break;
            case MotionEvent.ACTION_UP:
                this.setIsPressed(false);
                this.resetActuator();
                break;
        }

        this.updateInnerCirclePosition();
        // inform service of the change, X=aileron, Y=elevator*(-1)
        if(this.notifyService != null)
            notifyService.onChange(this.actuatorX, (-1)*this.actuatorY);
        postInvalidate();
        return super.onTouchEvent(event);
    }

    //util func to calc distance between touch of user to center
    private double getDistanceFromCenter(double touchPositionX, double touchPositionY){
        return Math.sqrt(
                Math.pow((this.outerCircleCenterX - touchPositionX), 2) +
                Math.pow((this.outerCircleCenterY - touchPositionY), 2)
        );
    }

    // check if user touched within the outer circle
    public boolean isPressed(double touchPositionX, double touchPositionY){
        return getDistanceFromCenter(touchPositionX, touchPositionY) < this.outerCircleRadius;
    }
    // this will decide how much % we need to move in x,y
    public void setActuator(double touchPositionX, double touchPositionY){
        double deltaX = touchPositionX - this.outerCircleCenterX;
        double deltaY = touchPositionY - this.outerCircleCenterY;
        double deltaDistance = getDistanceFromCenter(touchPositionX, touchPositionY);

        if(deltaDistance < this.outerCircleRadius){
            this.actuatorX = deltaX/this.outerCircleRadius;
            this.actuatorY = deltaY/this.outerCircleRadius;
        } else {
            // can't increase more
            this.actuatorX = deltaX/deltaDistance;
            this.actuatorY = deltaY/deltaDistance;
        }
    }
    public void resetActuator() {
        this.actuatorX = 0.0;
        this.actuatorY = 0.0;
    }

    public void updateInnerCirclePosition() {
        this.innerCircleCenterX = (int) (this.outerCircleCenterX +
                this.actuatorX*this.outerCircleRadius);
        this.innerCircleCenterY = (int) (this.outerCircleCenterY +
                this.actuatorY*this.outerCircleRadius);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw outer circle
        canvas.drawCircle(
                outerCircleCenterX,
                outerCircleCenterY,
                outerCircleRadius,
                outerCirclePaint
        );
        // Draw inner circle
        canvas.drawCircle(
                innerCircleCenterX,
                innerCircleCenterY,
                innerCircleRadius,
                innerCirclePaint
        );
    }

}
