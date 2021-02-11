package com.example.smartsleep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import android.os.Bundle;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity {


    //Change this depending on the address of your sample peripheral
    String mDeviceAddress = "5D:8F:4E:76:9B:2F";


    //private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    //Bluetooth permission
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    //coarse location permission
    private static int PERMISSION_REQUEST_CODE = 1;
    private final static String TAG = MainActivity.class.getSimpleName();

    private BluetoothLeService mBluetoothLeService;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Setup BLE ****************************************************************************************************************************

        Log.d(TAG, "Request Location Permissions:");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //BLE Device Scan **********************************************************************************************************************
        //TODO: implement device scan to find our BLE device, create variable to store device identification



        //final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        //Log.d(TAG, "Connect request result=" + result);



        TextView heartRateValue = (TextView) findViewById(R.id.heart_rate_value);
        //String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
       // if(data != null)
         //   heartRateValue.setText(data);

        //TODO: later, add feature that purple view is fullscreen and after you are connected the bottom part scrolls up



        //Initialize TextViews and onClick *****************************************************************************************************


        // Find View for HeartRate
        ConstraintLayout heartRate = (ConstraintLayout) findViewById(R.id.heart_rate);
        //Set HeartRate click
        heartRate.setOnClickListener(view -> {
            Intent heartRateIntent = new Intent(MainActivity.this, HeartRateActivity.class);
            startActivity(heartRateIntent);
        });

        // Find View for OxygenLevels
        ConstraintLayout oxygenLevels = (ConstraintLayout) findViewById(R.id.oxygen);
        //Set OxygenLevels click
        oxygenLevels.setOnClickListener(view -> {
            Intent oxygenLevelsIntent = new Intent(MainActivity.this, OxygenLevelsActivity.class);
            startActivity(oxygenLevelsIntent);
        });

        // Find View for Motion
        ConstraintLayout motion = (ConstraintLayout) findViewById(R.id.motion);
        //Set Motion click
        motion.setOnClickListener(view -> {
            Intent motionIntent = new Intent(MainActivity.this, MotionActivity.class);
            startActivity(motionIntent);
        });

        // Find View for Temperature
        ConstraintLayout temperature = (ConstraintLayout) findViewById(R.id.temperature);
        //Set Temperature click
        temperature.setOnClickListener(view -> {
            Intent temperatureIntent = new Intent(MainActivity.this, TemperatureActivity.class);
            startActivity(temperatureIntent);
        });

        // Find View for sound
        ConstraintLayout sound = (ConstraintLayout) findViewById(R.id.sound);
        //Set sound click
        sound.setOnClickListener(view -> {
            Intent soundIntent = new Intent(MainActivity.this, SoundActivity.class);
            startActivity(soundIntent);
        });

    }


}