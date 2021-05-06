package com.example.smartsleep;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SoundActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        TextView sensorValue = (TextView) findViewById(R.id.sensorValue);

        //TODO: access Firebase to view history of values
        sensorValue.setText("Sound Sensor Value History");


        //create graph
        TimeSeriesGraph graph = new TimeSeriesGraph();
        Intent intent = getIntent();
        ArrayList<Integer[]> dataArr = (ArrayList<Integer[]>) intent.getSerializableExtra("GRAPH");
        graph.constructGraph(this, dataArr);
    }

}
