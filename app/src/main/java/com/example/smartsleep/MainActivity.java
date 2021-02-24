package com.example.smartsleep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.TextView;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {


    //TODO: Delete this when scanning for device by name is implemented
    //Change this depending on the address of your sample peripheral
    String mDeviceAddress = "4E:F9:14:D9:6F:04";


    //private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    //Bluetooth permission
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000;
    //coarse location permission
    private static int PERMISSION_REQUEST_CODE = 1;
    private final static String TAG = MainActivity.class.getSimpleName();
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    //VARIABLE OF DEVICE NAME TO CONNECT TO


    private SignInButton signInButton;
    private Button signOutButton;
    TextView connectionTextBox;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private int RC_SIGN_IN = 1;




    // Code to manage BLE Service lifecycle.
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


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                mBluetoothLeService.close();
                mBluetoothLeService.connect(mDeviceAddress);
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        signInButton = findViewById(R.id.sign_in_button);
        signOutButton = findViewById(R.id.sign_out_button);
        mAuth = FirebaseAuth.getInstance();



        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);

        //checks if you are already signed in
        updateUI(mAuth.getCurrentUser());

        //set on click listener for sign in button
        signInButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                signIn();
            }
        });

        //set on click listener for sign out button
        signOutButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                mGoogleSignInClient.signOut();
                Toast.makeText(MainActivity.this, "You Are Logged Out", Toast.LENGTH_SHORT).show();
                signOutButton.setVisibility(View.INVISIBLE);
                signInButton.setVisibility(View.VISIBLE);
            }

        });




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


        //BLE Device Scan START **********************************************************************************************************************
        //TODO: Scan for device based on name instead of hardcoding device address


        //BLE Device Scan END **********************************************************************************************************************



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

        connectionTextBox = (TextView) findViewById(R.id.summary_View);
        connectionTextBox.setOnClickListener(view -> {
            mBluetoothLeService.connect(mDeviceAddress);
        });

    }



    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionTextBox.setText(resourceId);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    //Sign In Functions START ******************************************************************************************************
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask){
        try{
            GoogleSignInAccount acc = completedTask.getResult(ApiException.class);
            Toast.makeText(MainActivity.this, "Sign In Successful", Toast.LENGTH_SHORT).show();
            signInButton.setVisibility(View.INVISIBLE);
            FirebaseGoogleAuth(acc);
        }
        catch(ApiException e){
            Toast.makeText(MainActivity.this, "Sign In Failed", Toast.LENGTH_SHORT).show();
            FirebaseGoogleAuth(null);
        }
    }

    private void FirebaseGoogleAuth(GoogleSignInAccount acct){
        AuthCredential authCredential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(authCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_SHORT).show();
                    FirebaseUser user = mAuth.getCurrentUser();
                    updateUI(user);
                }
                else{
                    Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    updateUI(null);
                }
            }
        });
    }

    private void updateUI(FirebaseUser fUser){
        //make sign out button visible, show username, etc.

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (fUser != null) {
            Toast.makeText(MainActivity.this, "Sign In Successful", Toast.LENGTH_SHORT).show();
            signOutButton.setText("Sign Out " + account.getEmail());

            signOutButton.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.INVISIBLE);
        }


    }

    //Sign In Functions END *******************************************************************************************




    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


}