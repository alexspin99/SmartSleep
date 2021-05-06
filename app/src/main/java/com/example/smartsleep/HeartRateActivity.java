package com.example.smartsleep;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.transition.Explode;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.UUID;

public class HeartRateActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        TextView sensorValue = (TextView) findViewById(R.id.sensorValue);
        sensorValue.setText("Heart Rate Sensor Value History");


        //create graph
        TimeSeriesGraph graph = new TimeSeriesGraph();
        Intent intent = getIntent();
        ArrayList<Integer[]> dataArr = (ArrayList<Integer[]>) intent.getSerializableExtra("GRAPH");
        graph.constructGraph(this, dataArr);


    }
}

