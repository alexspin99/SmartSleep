package com.example.smartsleep;
import android.app.Activity;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TimeSeriesGraph implements Serializable {

    //variables
    private Activity activity;
    private GraphView graph;
    ArrayList<Integer[]> dataArr;
    private LineGraphSeries<DataPoint> series = new LineGraphSeries<>();


    //Constructor
    public TimeSeriesGraph(){

    }

    //constructs graph in sensor value activities
    public void constructGraph(Activity act, ArrayList<Integer[]> data){
        //finds graphview
        activity = act;
        graph = (GraphView) activity.findViewById(R.id.Graph);

        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(act));

        //Labels are currently set to 0 because they do not display in the correct format
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);

        //adds all data to series
        for (int i = 0; i < data.size(); i++){
            DataPoint dataPt = new DataPoint(data.get(i)[0], data.get(i)[1]);
            series.appendData(dataPt, true, 50);
        }

        graph.addSeries(series);
    }


}
