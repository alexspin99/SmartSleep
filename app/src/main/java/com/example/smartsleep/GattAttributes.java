package com.example.smartsleep;

import java.util.HashMap;

/**
 * This class includes a HashMap of GATT attributes being sent to the application.
 */


//TODO: customize attributes to include everything being sent to device

public class GattAttributes {

    private static HashMap<String, String> attributes = new HashMap();

    //uuid variables
    //all BLE uuid can be, 0000XXXX-0000-1000-8000-00805f9b34fb where XXXX is obtained from hex uuid 0xXXXX
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String CURRENT_TIME_SERVICE = "00001805-0000-1000-8000-00805f9b34fb";
    public static String CURRENT_TIME_MEASUREMENT = "00002a2b-0000-1000-8000-00805f9b34fb";
    public static String PULSE_OXIMETER_SERVICE = "00001822-0000-1000-8000-00805f9b34fb";



    static {
        // Services
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put(CURRENT_TIME_SERVICE, "Current Time Service");
        attributes.put(PULSE_OXIMETER_SERVICE, "Pulse Oximeter Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Characteristics
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(CURRENT_TIME_MEASUREMENT, "Current Time Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
