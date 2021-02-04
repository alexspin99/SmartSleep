package com.example.smartsleep;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MotionActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        TextView sensorValue = (TextView) findViewById(R.id.sensorValue);

        //TODO: set text to real time sensor value
        sensorValue.setText("Motion Sensor Value History");

    }
}
