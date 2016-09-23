# Raspberry-Bluetooth-Android_client



This project is modify by the Android sample code of [BluetoothChat](https://developer.android.com/samples/BluetoothChat/index.html).
It using the BluetoothService Class for controlling the bluetooth device connection and data transfer.

In the BluetoothService class, there are parameter to config

```java
private static final UUID MY_UUID_INSECURE = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
```

This UUID must be the same of the device you want to connect.

In the Application, input the name of bluetooth device you are going to connect and press 'START SEARCH' Button

After the device was found, the 'SEND MESSAGE' Button will disable.

The 'SEND MESSAGE' Button will enable for send message to the Bluetooth device.

After enter the String you want to send in the Message field, press 'SEND MESSAGE' Button to send the String to Bluetooth device. The return String will display under the 'SEND MESSAGE' Button