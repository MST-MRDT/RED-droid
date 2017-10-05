package edu.mst.marsrover.reddroid;

import android.annotation.SuppressLint;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.VideoView;

import java.util.Arrays;

import edu.mst.marsrover.reddroid.rovecomm.RoveComm;

/**
 * Class that is application within Android. Android environment creates using onCreate() and
 * the android lifecycle.
 */
public class FullscreenActivity extends AppCompatActivity implements RoveComm.OnReceiveData {

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private VideoView mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private SeekBar mSeekbar;
    private RoveComm roveComm;
    private boolean forward = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create instance of rovecomm to use for life of activity
        roveComm = new RoveComm(this);

        // Load layout resource
        setContentView(R.layout.activity_fullscreen);

        mContentView = (VideoView) findViewById(R.id.fullscreen_content);

        mContentView.setVideoPath("https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/" +
                "f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8");
        mContentView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            // Close the progress bar and play the video
            public void onPrepared(MediaPlayer mp) {
                mContentView.start();
            }
        });

        mSeekbar = (SeekBar) findViewById(R.id.fullscreen_power_seek);

        // Onclick logic for directional button
        findViewById(R.id.fullscreen_direction_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button button = (Button) view;
                if(button.getText().equals("Reverse")) {
                    button.setText("Forward");
                    forward = true;
                } else {
                    button.setText("Reverse");
                    forward = false;
                }
            }
        });

        // Onclick logic for stop button
        findViewById(R.id.fullscreen_stop_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopDrive();
            }
        });

        // Hide Android UI
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.post(mHideRunnable);

        // Listener for rotation event. When device rotates even the slightest bit, this is called
        // Here we send an updated packet with the new drive calculations
        OrientationEventListener mOrientationListener = new OrientationEventListener(
                this, SensorManager.SENSOR_DELAY_GAME) {
            @Override
            public void onOrientationChanged(int i) {

                int degrees = i - 270;
                int power = forward ? mSeekbar.getProgress() : -mSeekbar.getProgress();
                int leftPower = power;
                int rightPower = power;

                // This construct defines the logic that resolves issues with degrees. Degrees has
                // been centered at 0 for straight ahead.

                // First two statements check rotation beyond the construct of positive 180 degrees
                if (degrees < -180) {

                    sendNewDrivePower(0, power);
                } else if (degrees < -90) {

                    sendNewDrivePower(power, 0);
                }

                // These three scale as necessary within the positive 180 bounds
                else if (degrees > 0) {
                    // Reduce the left side linearly
                    Log.e("HI", String.valueOf((1 - (degrees / 90.0))));
                    leftPower *= (1 - (degrees / 90.0));
                    sendNewDrivePower(leftPower, rightPower);
                } else if (degrees < 0) {
                    // Reduce the right side linearly
                    rightPower *= (1 + (degrees / 90.0));
                    sendNewDrivePower(leftPower, rightPower);
                } else {
                    // Full speed ahead
                    sendNewDrivePower(power, power);
                }
            }
        };

        // Check orientation ability and enable
        if (mOrientationListener.canDetectOrientation()) {
            Log.e("Activity", "Can detect orientation");
            mOrientationListener.enable();
        } else {
            Log.e("Activity", "Cannot detect orientation");
            mOrientationListener.disable();
        }
    }

    /**
     * Method to stop drive
     */
    private void stopDrive() {
        mSeekbar.setProgress(0);
        sendNewDrivePower(0, 0);
    }

    /**
     * Method called to format and send a packet with left & right motor power
     * @param left power, -1000 <-> 1000
     * @param right power, -1000 <-> 1000
     */
    private void sendNewDrivePower(int left, int right) {

        // Send power, two byte[2]
        byte[] data = new byte[4];
        data[0] = (byte) (left & 0xFF);
        data[1] = (byte) ((left >>> 8) & 0xFF);
        data[2] = (byte) (right & 0xFF);
        data[3] = (byte) ((right >>> 8) & 0xFF);

        // Send specifically to drive board
        roveComm.sendData(528, data, "192.168.1.130");
    }

    /**
     * Hides Android UI layers
     */
    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Implemented method to handle received packets. Is called automatically.
     * @param id DataID of the packet
     * @param contents data[]
     */
    @Override
    public void receiveData(int id, byte[] contents) {

        Log.e("RoveComm", "You have mail! " + Arrays.toString(contents));
    }

    @Override
    protected void onStop() {
        roveComm.onDestroy();
        super.onStop();
    }
}
