package com.example.smartsleep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


public class MainActivity extends AppCompatActivity {

    //TODO: Delete this when scanning for device by name is implemented
    //Change this depending on the address of your sample peripheral
    String mDeviceAddress = "66:93:13:80:9B:D4";
    String deviceName = "SmartSock";

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
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private ArrayDeque<BluetoothGattCharacteristic> characteristicReadQueue = new ArrayDeque<BluetoothGattCharacteristic>();

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    FirebaseFirestore db;
    DocumentReference userDatabase = null;
    private SignInButton signInButton;
    private Button signOutButton;
    TextView connectionTextBox;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private int RC_SIGN_IN = 1;

    //stores characteristic of each sensor characteristic, initializes to null so it doesn't crash if it doesn't find characteristic
    BluetoothGattCharacteristic hrGattChar = null, o2GattChar = null , soundGattChar = null, motionGattChar = null, tempGattChar = null;
    BluetoothGattCharacteristic[] desiredChars = {hrGattChar, o2GattChar, soundGattChar, motionGattChar, tempGattChar};

    //textViews for sensor values
    TextView oxygenValue, tempValue, soundValue, motionValue, heartRateValue, alerts;
    ConstraintLayout hrBackground, o2Background, tempBackground, soundBackground, motionBackground;




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

                // Collects all needed gatt services & characteristics
                //performs first read chara
                retrieveGattServices(mBluetoothLeService.getSupportedGattServices());


            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {


                Log.i(TAG, "Collecting data");

                String[] sensorData = intent.getStringArrayExtra("SENSOR_DATA");

                // Collecting the incoming data
                displayData(sensorData[0], sensorData[1], sensorData[2], sensorData[3], sensorData[4]);

                Log.d(TAG, sensorData[0] + sensorData[1] + sensorData[2] + sensorData[3] + sensorData[4]);

                if(characteristicReadQueue.size() > 0)
                {
                    getCharacteristicValues();
                };

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
        // Access Cloud Firestore instance
        db = FirebaseFirestore.getInstance();


        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("448508620469-c2t83ud39qldtblh2q2tvd1dp5n5g7op.apps.googleusercontent.com")
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

        connectionTextBox = (TextView) findViewById(R.id.summary_View);
        updateConnectionState(R.string.connecting);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        //BLE Device Scan START **********************************************************************************************************************
        //TODO: Scan for device based on name instead of hardcoding device address



        /**ScanFilter filter = new ScanFilter.Builder()
                .setDeviceName(deviceName)
                .build();
        scanner.startScan(filter, scanSettings, scanCallback);8**/

        //BLE Device Scan END **********************************************************************************************************************


        //mBluetoothLeService.connect (mDeviceAddress);

        //Initialize TextViews and onClick *****************************************************************************************************


        // Find View for HeartRate
        hrBackground = (ConstraintLayout) findViewById(R.id.heart_rate);
        //Set HeartRate click
        hrBackground.setOnClickListener(view -> {
            Intent heartRateIntent = new Intent(MainActivity.this, HeartRateActivity.class);
            startActivity(heartRateIntent);
        });

        // Find View for OxygenLevels
        o2Background = (ConstraintLayout) findViewById(R.id.oxygen);
        //Set OxygenLevels click
        o2Background.setOnClickListener(view -> {
            Intent oxygenLevelsIntent = new Intent(MainActivity.this, OxygenLevelsActivity.class);
            startActivity(oxygenLevelsIntent);
        });

        // Find View for Motion
        motionBackground = (ConstraintLayout) findViewById(R.id.motion);
        //Set Motion click
        motionBackground.setOnClickListener(view -> {
            Intent motionIntent = new Intent(MainActivity.this, MotionActivity.class);
            startActivity(motionIntent);
        });

        // Find View for Temperature
        tempBackground = (ConstraintLayout) findViewById(R.id.temperature);
        //Set Temperature click
        tempBackground.setOnClickListener(view -> {
            Intent temperatureIntent = new Intent(MainActivity.this, TemperatureActivity.class);
            startActivity(temperatureIntent);
        });

        // Find View for sound
        soundBackground = (ConstraintLayout) findViewById(R.id.sound);
        //Set sound click
        soundBackground.setOnClickListener(view -> {
            Intent soundIntent = new Intent(MainActivity.this, SoundActivity.class);
            startActivity(soundIntent);
        });


        connectionTextBox.setOnClickListener(view -> {
            mBluetoothLeService.connect(mDeviceAddress);
        });



        heartRateValue = (TextView) findViewById(R.id.heart_rate_value);
        oxygenValue = (TextView) findViewById(R.id.oxygen_value);
        tempValue = (TextView) findViewById(R.id.temperature_value);
        soundValue = (TextView) findViewById(R.id.sound_value);
        motionValue = (TextView) findViewById(R.id.motion_value);
        alerts = (TextView) findViewById(R.id.alert_View);
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

    //retrieves Gatt services & characteristics from device.  Stores values of chars needed to be read
    private void retrieveGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
                    int index = 0;

            // Loops through available Characteristics
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, GattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);


                //checks if chara is one of the needed sensor charas
                //adds them to queue and saves in array
                if(uuid.equals(getString(R.string.HR))){
                    desiredChars[0] = gattCharacteristic;
                    characteristicReadQueue.add(desiredChars[0]);
                }
                if (uuid.equals(getString(R.string.O2))){
                    desiredChars[1] = gattCharacteristic;
                    characteristicReadQueue.add(desiredChars[1]);
                }
                if (uuid.equals(getString(R.string.SOUND))){
                    desiredChars[2] = gattCharacteristic;
                    characteristicReadQueue.add(desiredChars[2]);
                }
                if (uuid.equals(getString(R.string.MOTION))){
                    desiredChars[3] = gattCharacteristic;
                    characteristicReadQueue.add(desiredChars[3]);
                }
                if (uuid.equals(getString(R.string.TEMP))){
                    desiredChars[4] = gattCharacteristic;
                    characteristicReadQueue.add(desiredChars[4]);
                }

                index++;
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);

            getCharacteristicValues();
        }
    }

    private void getCharacteristicValues()
    {
        if (!characteristicReadQueue.isEmpty()) {



            //READS CHARACTERISTIC
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothGattCharacteristic currentChar = characteristicReadQueue.remove();

                    mBluetoothLeService.readCharacteristic(characteristicReadQueue.element());
                    mBluetoothLeService.setCharacteristicNotification(characteristicReadQueue.element(), true);
                    characteristicReadQueue.add(currentChar);

                }
            }, 2000); //Time in milisecond

        }


    }

    //displays data
    private void displayData(String hrData, String o2Data, String motionData, String tempData, String soundData) {


        //Creates map to upload to cloud
        Map<String, Object> data = new HashMap<>();

            if (hrData != null) {
                heartRateValue.setText(hrData);
                data.put("HR", Integer.parseInt(hrData));
            }
            if (o2Data != null) {
                oxygenValue.setText(o2Data);
                data.put("O2", Integer.parseInt(o2Data));
            }
            else
                data.put("O2", "--");
            if (motionData != null) {
                motionValue.setText(motionData);
                data.put("Motion", Integer.parseInt(motionData));
            }
            else
                data.put("Motion", "--");
            if (tempData != null) {
                tempValue.setText(tempData);
                data.put("Temp", Integer.parseInt(tempData));
            }
            else
                data.put("Temp", "--");
            if (soundData != null) {
                soundValue.setText(soundData);
                data.put("Sound", Integer.parseInt(soundData));
            }
            else
                data.put("Sound", "--");

            checkForAlerts(hrData, o2Data, motionData, tempData, soundData);

            //Upload data to cloud
            uploadToFirestore(data);


    }

    private void checkForAlerts(String hrData, String o2Data, String motionData, String tempData, String soundData){
        int hrDataNum;
        int o2DataNum;
        int motionDataNum;
        int tempDataNum;
        int soundDataNum;
        //70-190 bpm is healthy resting heart rate for a newborn
        int HRmin = 80;
        int HRmax = 190;

        //retrieve data
        if (hrData != null) {
            hrDataNum = Integer.parseInt(hrData);
            Alerts(HRmax, HRmin, hrDataNum);
        }
        if (o2Data != null)
            o2DataNum = Integer.parseInt(o2Data);
        if (motionData != null)
            motionDataNum = Integer.parseInt(motionData);
        if (tempData != null)
            tempDataNum = Integer.parseInt(tempData);
        if (soundData != null)
            soundDataNum = Integer.parseInt(soundData);



        /**   Add alerts for all other sensors, make sure correct sensor is sent.
         * Maybe add parameter saying which sensor
        Alerts(O2max, O2min, o2Data);
        Alerts(MotionMax, MotionMin, motionData);
        Alerts(TempMax, TempMin, tempData);
        Alerts(SoundMax, SoundMin, soundData);
         **/


    }

    private void Alerts(int HRmax, int HRmin, int HRdata){

        if (HRdata > HRmax) {
            alerts.setText("HIGH HEART RATE");
            hrBackground.setBackgroundColor(getColor(R.color.alert));
        }
        else if (HRdata < HRmin) {
            alerts.setText("LOW HEART RATE");
            hrBackground.setBackgroundColor(getColor(R.color.alert));
        }
        else {
            alerts.setText("Healthy Environment.");
            hrBackground.setBackgroundColor(getColor(R.color.sensorBackgroundColor));
        }

    }

    private void uploadToFirestore(Map<String, Object> data){
        String currentTime = new Date().toString();

        //create document path to user



        if (userDatabase != null){
            userDatabase.collection("data").document(currentTime)
                    .set(data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        }
        else{
            db.collection("users").document("noUser").collection("data").document(currentTime)
                    .set(data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        }



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

            //find cloud database user collection
            db.collection("users").whereEqualTo("UID", fUser.getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                QuerySnapshot querySnapshot = task.getResult();
                                if (querySnapshot.isEmpty()) {
                                    //adds user to database
                                    Log.d(TAG, "User DNE in database ");
                                    addUserToDatabase(fUser);

                                } else {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d(TAG, document.getId() + " => " + document.getData());
                                        //sets user database to their collection
                                        Log.d(TAG, "User is in database");
                                        userDatabase = document.getReference();


                                    }
                                }
                            }
                            else {
                                Log.v(TAG, "Query for user failed: ", task.getException());

                            }

                        }
                    });


            signOutButton.setText("Sign Out " + account.getEmail());
            signOutButton.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.INVISIBLE);
        }


    }

    public void addUserToDatabase(FirebaseUser fUser){

        Map<String, Object> user = new HashMap<>();
        user.put("UID", fUser.getUid());

        String username = fUser.getDisplayName();


        Log.d(TAG, "Attempting to add new user " + username);
        db.collection("users").document(username)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "New user added to database successfully!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "New user not added to database", e);
                    }
                });

        userDatabase = db.collection("users").document(username);
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