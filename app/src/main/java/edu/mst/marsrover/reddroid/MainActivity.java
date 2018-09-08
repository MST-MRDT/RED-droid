package edu.mst.marsrover.reddroid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.util.Arrays;

import edu.mst.marsrover.reddroid.rovecomm.RoveComm;

public class MainActivity extends AppCompatActivity implements RoveComm.OnReceiveData, SeekBar.OnSeekBarChangeListener {


    private RoveComm roveComm;

    private SeekBar seekLeft, seekRight;
    private boolean forwardLeft = true;
    private boolean forwardRight = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create instance of rovecomm to use for life of activity
        roveComm = new RoveComm(this);

        seekLeft = findViewById(R.id.fullscreen_power_seek_left);
        seekRight = findViewById(R.id.fullscreen_power_seek_right);

        seekLeft.setOnSeekBarChangeListener(this);
        seekRight.setOnSeekBarChangeListener(this);

        // Onclick logic for directional button
        findViewById(R.id.fullscreen_direction_button_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopDrive();
                Button button = (Button) view;
                if(button.getText().equals(getString(R.string.reverse))) {
                    button.setText(R.string.forward);
                } else {
                    button.setText(R.string.reverse);
                }
                forwardLeft = !forwardLeft;
            }
        });

        findViewById(R.id.fullscreen_direction_button_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopDrive();
                Button button = (Button) view;
                if(button.getText().equals(getString(R.string.reverse))) {
                    button.setText(R.string.forward);
                } else {
                    button.setText(R.string.reverse);
                }
                forwardRight = !forwardRight;
            }
        });

        findViewById(R.id.fullscreen_stop_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopDrive();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startActivity(new Intent(this, SettingsActivity.class));
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        roveComm.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        int max = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("drive_speed","0"));

        if(max != -1) {
            seekLeft.setMax(max);
            seekRight.setMax(max);
        }
        super.onResume();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        int left = seekLeft.getProgress();
        int right = seekRight.getProgress();

        if(!forwardLeft) left *= -1;
        if(!forwardRight) right *= -1;

        sendNewDrivePower(left, right);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    /**
     * Method to stop drive
     */
    private void stopDrive() {
        seekLeft.setProgress(0);
        seekRight.setProgress(0);
        sendNewDrivePower(0,0);
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

        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("send_packets_switch", false)) {

            // Send specifically to drive board
            roveComm.sendData(528, data, "192.168.1.130");
            Log.e("RoveComm", "Sending Drive Powers: " + left + ", " + right);
        }
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
}
