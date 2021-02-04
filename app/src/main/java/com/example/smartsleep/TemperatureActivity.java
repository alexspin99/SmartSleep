package com.example.smartsleep;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TemperatureActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        TextView sensorValue = (TextView) findViewById(R.id.sensorValue);

        //TODO: access Firebase to view history of values
        sensorValue.setText("Temperature Sensor Value History");

    }
}
