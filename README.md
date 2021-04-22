# SmartSleep
Senior Capstone Design Application for Baby Sleep Monitor. App collects the data and organizes and analyzes.
===========================================

Application connects to a BLE peripheral with the DeviceAddress specified at the beginning of the MainActivity.java class
Reads multi-characteristics specified in the strings resource file.  For this project: Heart Rate, Oxygen Level, Sound, Motion and Temperature are used.

Application uploads the data to Firebase Firestore based on signed in user, if not signed in it adds to a user called "noUser" for testing purposes.
Application displays alerts in app if the readings are above or below a healthy threshold, seen in the checkForAlerts() function.
