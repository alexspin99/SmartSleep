package com.example.smartsleep;

import android.os.Build;
import android.os.Bundle;
import android.transition.Explode;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class HeartRateActivity extends AppCompatActivity {

    //Heart Rate UUID
    UUID chara = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        TextView sensorValue = (TextView) findViewById(R.id.sensorValue);

        //TODO: access Firebase to view history of values
        sensorValue.setText("Heart Rate Sensor History");





    }
}

