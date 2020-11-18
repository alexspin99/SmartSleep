package com.example.smartsleep;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find View for HeartRate
        TextView heartRate = (TextView) findViewById(R.id.heart_rate);
        //Set HeartRate click
        heartRate.setOnClickListener(view -> {
            Intent heartRateIntent = new Intent(MainActivity.this, HeartRateActivity.class);
            startActivity(heartRateIntent);
        });

        // Find View for OxygenLevels
        TextView oxygenLevels = (TextView) findViewById(R.id.oxygen);
        //Set OxygenLevels click
        oxygenLevels.setOnClickListener(view -> {
            Intent oxygenLevelsIntent = new Intent(MainActivity.this, OxygenLevelsActivity.class);
            startActivity(oxygenLevelsIntent);
        });

        // Find View for Motion
        TextView motion = (TextView) findViewById(R.id.motion);
        //Set Motion click
        motion.setOnClickListener(view -> {
            Intent motionIntent = new Intent(MainActivity.this, MotionActivity.class);
            startActivity(motionIntent);
        });

        // Find View for Temperature
        TextView temperature = (TextView) findViewById(R.id.temperature);
        //Set Temperature click
        temperature.setOnClickListener(view -> {
            Intent temperatureIntent = new Intent(MainActivity.this, TemperatureActivity.class);
            startActivity(temperatureIntent);
        });

        // Find View for sound
        TextView sound = (TextView) findViewById(R.id.sound);
        //Set sound click
        sound.setOnClickListener(view -> {
            Intent soundIntent = new Intent(MainActivity.this, SoundActivity.class);
            startActivity(soundIntent);
        });

    }


}