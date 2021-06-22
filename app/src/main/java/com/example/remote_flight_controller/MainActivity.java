package com.example.remote_flight_controller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.example.remote_flight_controller.databinding.ActivityMainBinding;
import com.example.remote_flight_controller.viewmodels.MainViewModel;
import com.example.remote_flight_controller.views.Joystick;

// MainActivity routine:
// First Frame -> ConnectBtn pressed -> check validity -> wait for socket connection to FG
// on connection success: -> close First Frame -> open Second Frame
// else: -> invoke error message to user
public class MainActivity extends AppCompatActivity {

    MainViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setup binding between VM and UI(MainActivity)
        vm = ViewModelProviders.of(this).get(MainViewModel.class);
        final ActivityMainBinding activityMainBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_main);
        activityMainBinding.setVm(vm);
        activityMainBinding.setLifecycleOwner(this);

        // add listeners for both bars
        SeekBar rudderSeekBar = (SeekBar)(findViewById(R.id.rudderSeekBar));
        SeekBar throttleSeekBar = (SeekBar)(findViewById(R.id.throttleSeekBar));
        seekBarChangeListener(rudderSeekBar);
        seekBarChangeListener(throttleSeekBar);

        // setup a special button click listener for both starting animation and connecting to FG
        Button connectBtn = (Button)(findViewById(R.id.connectButton));
        connectButtonClickListener(connectBtn);

        // dependency injection+strategy design pattern. Joystick is a stand-alone View component
        Joystick joystick = (Joystick)findViewById(R.id.joystick);
        joystick.notifyService= (aileron, elevator) -> {
            vm.setAileron(aileron);
            vm.setElevator(elevator);
        };

    }

    // don't forget to close the FG socket after done using the app
    @Override
    protected void onDestroy() {
        super.onDestroy();
        vm.disconnectFromFG();
    }

    private void seekBarChangeListener(SeekBar aSeekBar){
        aSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(seekBar.getId() == R.id.rudderSeekBar){
                    // seekBar is between (0,200) so we need to manipulate this value to be (-1,1)
                    progress -= 100;
                    vm.setRudder(((double)progress)/100);
                }
                if(seekBar.getId() == R.id.throttleSeekBar){
                    // seekBar is between (0,100) so we need to manipulate this value to be (0,1)
                    vm.setThrottle(((double)progress)/100);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void connectButtonClickListener(Button button){
        ProgressBar myAnimatedProgressBar = (ProgressBar) (findViewById(R.id.connectionProgressBar));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide keyboard when
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                // hide Error msg if was presented earlier
                LinearLayout connectionError = (LinearLayout)findViewById(R.id.connectionError);
                connectionError.setVisibility(View.GONE);
                // try connecting to FG
                vm.connectToFG();
                // if validity check fail then don't continue to animation
                if((!vm.getFeedbackIp().getValue().equals("")) || (!vm.getFeedbackPort().getValue().equals("")))
                    return;
                // setup animation to simulate a connection handling routine
                ConstraintLayout animationHolder = (ConstraintLayout)findViewById(R.id.animationHolder);
                animationHolder.setVisibility(View.VISIBLE);
                ObjectAnimator animator = ObjectAnimator.ofInt(myAnimatedProgressBar, "progress", 0, 101);
                animator.setDuration(2000);
                animator.setRepeatCount(0);
                animator.start();
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {}

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // socket had enough time to try and connect to FG
                        if(vm.getConnected()){
                            LinearLayout mainFrame = (LinearLayout)findViewById(R.id.mainFrame);
                            mainFrame.setVisibility(View.GONE);
                            ConstraintLayout joystickFrame = (ConstraintLayout)findViewById(R.id.joystickFrame);
                            joystickFrame.setVisibility(View.VISIBLE);
                        }
                        else{
                            animationHolder.setVisibility(View.GONE);
                            connectionError.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {}

                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
            }
        });
    }

}