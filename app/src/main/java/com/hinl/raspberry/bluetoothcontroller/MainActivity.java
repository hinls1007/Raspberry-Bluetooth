package com.hinl.raspberry.bluetoothcontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hinl.raspberry.bluetoothcontroller.services.BluetoothService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

	private static final String TAG = MainActivity.class.getName();



	private static final String PreferenceName = "Device_Preference";
	private static final String Extra_DeviceName = "DeviceName";


	// Request Code for the CoarseLocation permission
	private static final int RequestCode_Coarse_Location = 0x0A;


	TextView bluetooth_returns;
	EditText device_name, message_to_send;
	Button connect_btn, send_btn;



	BluetoothDevice bluetoothDevice;
	BluetoothService bluetoothService;
	BluetoothAdapter bluetoothAdapter;


	BlueToothMessageHandler bluetoothMessageHandler;

	SharedPreferences preferences;


	@Override
	public void onResume(){
		super.onResume();
		if (connect_btn!=null && send_btn!=null) {
			if (bluetoothDevice == null) {
				connect_btn.setEnabled(true);
				send_btn.setEnabled(false);
			} else {
				connect_btn.setEnabled(false);
				send_btn.setEnabled(true);
			}
		}
		if (preferences!=null){
			if (device_name!=null){
				device_name.setText(preferences.getString(Extra_DeviceName, ""));
			}
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (Build.VERSION.SDK_INT >= 21){
			requestPermission();
		} else {
			startActivity();
		}

	}

	public void startActivity(){
		initialViews();
		initialObjects();
	}

	/**
	 *
	 * For Android version 6.0 there were a wired permission needed for scanning bluetooth device
	 * This function is request for that permission
	 *
	 */
	public void requestPermission(){
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED){
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.ACCESS_COARSE_LOCATION)) {

				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

			} else {
				// No explanation needed, we can request the permission.
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
						RequestCode_Coarse_Location);
			}
		} else {
			startActivity();
		}
	}

	public void initialViews(){
		bluetooth_returns = (TextView) findViewById(R.id.bluetooth_returns);
		device_name = (EditText) findViewById(R.id.device_name);
		message_to_send = (EditText) findViewById(R.id.message_to_send);
		connect_btn = (Button) findViewById(R.id.connect_btn);
		send_btn = (Button) findViewById(R.id.send_btn);
		connect_btn.setOnClickListener(this);
		send_btn.setOnClickListener(this);


	}

	public void initialObjects(){
		preferences = getSharedPreferences(PreferenceName, MODE_PRIVATE);

		bluetoothMessageHandler = new BlueToothMessageHandler(new Handler(){
			@Override
			public void handleMessage(Message msg){
				if (msg.obj instanceof String){
					if (bluetooth_returns!=null){
						bluetooth_returns.setText((String)msg.obj);
					}
				}
			}

		});
		bluetoothService = new BluetoothService(this, bluetoothMessageHandler);
		bluetoothService.start();
	}


	/**
	 * Register the receiver and start scanning process
	 *
	 *
	 * @param deviceName The target device name which will connect later
	 */
	private synchronized void startSearching(String deviceName) {
		if (bluetoothAdapter == null) {
			IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			registerReceiver(new DeviceReceiver(deviceName), intentFilter);
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}
		if (!bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.startDiscovery();
		}
	}

	/**
	 * Send the String message to Bluetooth Device by BluetoothService Class
	 *
	 * @param message String message send to Bluetooth Device
	 */

	public void sendMessage(String message){
		if (bluetoothMessageHandler!=null) {
			bluetoothMessageHandler.setMessageToSend(message);
			bluetoothService.connect(bluetoothDevice);
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id){
			case R.id.connect_btn:
				if (device_name!=null) {
					String deviceName = device_name.getText().toString();
					startSearching(deviceName);
					preferences.edit().putString(Extra_DeviceName, deviceName).commit();
					device_name.setEnabled(false);

				}
				break;
			case R.id.send_btn:
				send_btn.setEnabled(false);
				if (message_to_send==null){
					return;
				}
				if (bluetooth_returns!=null){
					bluetooth_returns.setText("");
				}
				String message = message_to_send.getText().toString();
				sendMessage(message);
				break;
		}
	}

	/**
	 * Class for bluetooth message handling which communicate with BluetoothService
	 *
	 * Detect the device connection status
	 * Handling receive message from bluetooth device
	 * Get BluetoothService system message
	 */
	class BlueToothMessageHandler extends Handler{


		private String messageToSend = "";
		private Handler returnHandler;

		public BlueToothMessageHandler(Handler returnHandler){
			this.returnHandler = returnHandler;
		}


		public void setMessageToSend(String messageToSend){
			this.messageToSend = messageToSend;
		}

		@Override
		public void handleMessage(Message msg){
			int type = msg.what;
			switch (type){
				/**
				 * use the returnHandler to handle the message which return by the bluetooth device
				 */
				case BluetoothService.MESSAGE_READ:
					try {
						byte[] readBuf = (byte[]) msg.obj;
						// construct a string from the valid bytes in the buffer
						String readMessage = new String(readBuf, 0, msg.arg1);
						Message handlerMessage = returnHandler.obtainMessage(BluetoothService.MESSAGE_READ);
						handlerMessage.obj = readMessage;
						returnHandler.sendMessage(handlerMessage);
					} catch (Exception e){
						e.printStackTrace();
					}
					break;
				/**
				 * For bluetooth device state change handling
				 */
				case BluetoothService.MESSAGE_STATE_CHANGE:
					try {
						int state = bluetoothService.getState();
						if (state == BluetoothService.STATE_CONNECTED){
							Log.d(TAG, "connected");
							if (messageToSend !=null) {
								bluetoothService.write(messageToSend);
								send_btn.setEnabled(true);
							}
						}
					} catch (Exception e){
						e.printStackTrace();
					}
					break;
				/**
				 * For receive BlueToothService system message
				 */
				case BluetoothService.MESSAGE_TOAST:
					Log.d(TAG, "Toast"+msg.getData().getString(BluetoothService.TOAST));
					break;
			}
		}
	}

	/**
	 * Class for device searching, this broadcastreceiver is register the filter of
	 *
	 * BluetoothAdapter.ACTION_DISCOVERY_FINISHED
	 * BluetoothDevice.ACTION_FOUND
	 *
	 */

	class DeviceReceiver extends BroadcastReceiver{
		String deviceName = "";

		public DeviceReceiver(String deviceName){
			this.deviceName = deviceName;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			synchronized (this) {
				String action = intent.getAction();
				Log.d(TAG, "action:" + action);
				if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
					if (bluetoothDevice == null) {
						startSearching(deviceName);
					}
				} else if (action.equals(BluetoothDevice.ACTION_FOUND)){
					final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					Log.d(TAG, "device:"+ device.getName());

					//Store the Bluetooth device that match the device name and stop the search
					if (device.getName() != null && device.getName().equals(deviceName)) {
						if (bluetoothDevice != null) {
							try {
								unregisterReceiver(this);
							} catch (Exception e){
								e.printStackTrace();
							}
							return;
						}
						bluetoothDevice = device;

						connect_btn.setEnabled(false);
						send_btn.setEnabled(true);

					}
				}
			}
		}
	}



	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults) {
		switch (requestCode) {
			case RequestCode_Coarse_Location: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay! Do the
					// contacts-related task you need to do.
					startActivity();

				} else {

					this.finish();
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}
		}
	}
}
