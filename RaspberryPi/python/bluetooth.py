#!/usr/bin/env python
import os
import glob
import time
import subprocess
from bluetooth import *

# Set-up the Environment for bluetooth scan
os.system('hciconfig hci0 up')
os.system('hciconfig hci0 sspmode 1')
os.system('hciconfig hci0 piscan')

# Set-up the bluetooth environment for connection
server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

#The UUID Which is the same of the Android Application
uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"


#Initial the bluetooth service
advertise_service( server_sock, "BluetoothServer",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ],
#                   protocols = [ OBEX_UUID ]
                    )



while True:
	print "Waiting for connection on RFCOMM channel %d" % port

	# Waiting the Bluetooth device connect
	client_sock, client_info = server_sock.accept()
	print "Accepted connection from ", client_info

	try:
		# The data receive from the device connected with 1024 bytes
		data = client_sock.recv(1024)
		print "received [%s]" % data
        	if len(data) == 0: break
	        print "received [%s]" % data
		# The Example Function to get the network connection data
		if data == 'ipaddress':
			result = subprocess.check_output("hostname -I", shell=True).replace("\n","");
			result = result + "\n" + subprocess.check_output("iwconfig | grep SSID", shell=True);
			print "result [%s]" % result
		# Send the message back to the Bluetooth device as the return message
		client_sock.send(result)
		print "sending [%s]" % data

	except IOError:
		pass

	except KeyboardInterrupt:

		print "disconnected"

		# Close the Bluetooth socket When there are any interruption
		client_sock.close()
		server_sock.close()
		print "all done"

		break
