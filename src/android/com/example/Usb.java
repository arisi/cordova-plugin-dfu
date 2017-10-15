/*
 * Copyright 2015 Umbrela Smart, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;


public class Usb {

    final static String TAG = "ARI2";

    private Context mContext;

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbInterface mInterface;
    private int mDeviceVersion;

    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;

    /* USB DFU ID's (may differ by device) */
    public final static int USB_VENDOR_ID = 1155;   // VID while in DFU mode 0x0483
    public final static int USB_PRODUCT_ID = 57105; // PID while in DFU mode 0xDF11
    public final static int USB_VENDOR_ID2 = 17539;   // VID while in DFU mode 0x0483
    public final static int USB_PRODUCT_ID2 = 22336; // PID while in DFU mode 0xDF11

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    /* Callback Interface */
    public interface OnUsbChangeListener {
        void onUsbConnected();
    }

    public void setOnUsbChangeListener(OnUsbChangeListener l) {
        mOnUsbChangeListener = l;
    }

    private OnUsbChangeListener mOnUsbChangeListener;

    public UsbDevice getUsbDevice() {
        return mDevice;
    }

    /* Broadcast Receiver*/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.e(TAG, "fiund? device " + device);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            int deviceVID = device.getVendorId();
                            int devicePID = device.getProductId();
                            Log.e("ARIS","found device "+deviceVID+":"+devicePID);
                            if (USB_VENDOR_ID2==deviceVID && USB_PRODUCT_ID2==devicePID) {
                              Log.e("ARIS","found device SERIAL");
                              setDevices(device);

                            } else {
                              Log.e("ARIS","found device DFU");
                              setDevice(device);
                              if (mOnUsbChangeListener != null) {
                                  mOnUsbChangeListener.onUsbConnected();
                              }
                            }

                        }
                    } else {
                        Log.e(TAG, "permission denied for device " + device);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    Log.e(TAG, "attached ");

                    //request permission for just attached USB Device if it matches the VID/PID
                    requestPermission(mContext, USB_VENDOR_ID, USB_PRODUCT_ID);
                    Log.e(TAG, "attached and req perm maybe ;)");
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    Log.e(TAG, "detached ");
                    if (serialPortConnected) {
                      Log.e(TAG, "detached serial");
                      serialPortConnected = false;
                      //serialPort.close();
                    } else {
                      Log.e(TAG, "detached dfu");
                      UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                      if (mDevice != null && mDevice.equals(device)) {
                          release();
                      }
                    }
                }
            }
        }
    };

    public BroadcastReceiver getmUsbReceiver() {
        return mUsbReceiver;
    }

    public Usb(Context context) {
        mContext = context;
        serialPortConnected = false;
        Log.e("ARI3","ouheee");
    }

    public void setUsbManager(UsbManager usbManager) {
        this.mUsbManager = usbManager;
    }

    public void requestPermission(Context context, int vendorId, int productId) {
        // Setup Pending Intent
        Log.e("ARI9","reqPerm");
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(Usb.ACTION_USB_PERMISSION), 0);
        Log.e("ARI9","reqPerm2: "+permissionIntent);
        UsbDevice device = getUsbDevice(vendorId, productId);
        Log.e("ARI9","reqPerm3: "+device);

        if (device != null) {
            Log.e("ARI9","reqPerm4");
            mUsbManager.requestPermission(device, permissionIntent);
            Log.e("ARI9","reqPerm5");
        } else {
          Log.e("ARI9","not dfu --serial? ");
          device = getUsbDevice(USB_VENDOR_ID2, USB_PRODUCT_ID2);
          Log.e("ARI9","reqPerm3s: "+device);

          if (device != null) {
              Log.e("ARI9","reqPerm4s IT IS SERIAL");
              mUsbManager.requestPermission(device, permissionIntent);
              //Log.e("ARI9","reqPerm5");
          } else {
            Log.e("ARI9","not for us definately!");

          }

        }
    }

    private UsbDevice getUsbDevice(int vendorId, int productId) {
        Log.e("ARI8","getdevicelist...");
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Log.e("ARI8","getdevicelist: "+deviceList);
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        UsbDevice device;
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                return device;
            }
        }
        return null;
    }

    public boolean release() {
        boolean isReleased = false;

        if (mConnection != null) {
            isReleased = mConnection.releaseInterface(mInterface);
            mConnection.close();
            mConnection = null;
        }
        return isReleased;
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            try {
                String data = new String(arg0, "UTF-8");
                Log.e("ARIS","got data: '"+data+"'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(mDevice, mConnection);
            Log.e("ARIS","got opened: '"+serialPort);
            if (serialPort != null) {
                if (serialPort.open()) {
                    serialPort.setBaudRate(9600);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);
                    Log.e("ARIS","got really opened: '");

                    // Everything went as expected. Send an intent to MainActivity
                    //Intent intent = new Intent(ACTION_USB_READY);
                    //context.sendBroadcast(intent);
                } else {
                    Log.e("ARIS","got not open '");
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {
                        Log.e("ARIS","got not open 2'");
                        //Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        //context.sendBroadcast(intent);
                    } else {
                        Log.e("ARIS","got not open 3'");
                        //Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        //context.sendBroadcast(intent);
                    }
                }
            } else {
                Log.e("ARIS","got not open no driver'");

                // No driver for given device, even generic CDC driver could not be loaded
                //Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                //context.sendBroadcast(intent);
            }
        }
    }

    public void setDevices(UsbDevice device) {
        mDevice = device;
        if (device != null) {
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            serialPortConnected = true;
            new ConnectionThread().run();
          }
    }
    public void setDevice(UsbDevice device) {
        mDevice = device;

        // The first interface is the one we want
        mInterface = device.getInterface(0);    // todo check when changing if alternative interface is changing

        if (device != null) {
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null && connection.claimInterface(mInterface, true)) {
                Log.i("ARI", "open SUCCESS");
                mConnection = connection;

                // get the bcdDevice version
                byte[] rawDescriptor = mConnection.getRawDescriptors();
                mDeviceVersion = rawDescriptor[13] << 8;
                mDeviceVersion |= rawDescriptor[12];

                //Log.i("ARI", getDeviceInfo(device));
            } else {
                Log.e("ARI", "open FAIL");
                mConnection = null;
            }
        }
    }

    public boolean isConnected() {
        return (mConnection != null);
    }

    public JSONObject getDeviceInfo(UsbDevice device) {
        JSONObject json = new JSONObject();
        if (device == null)
            return json;
        try {
          json.put("model", device.getDeviceName());
          json.put("id", device.getDeviceId() + " (0x" + Integer.toHexString(device.getDeviceId()) + ")");
          json.put("class", device.getDeviceClass());
          json.put("subclass", device.getDeviceSubclass());
          json.put("vendor", device.getVendorId() + " (0x" + Integer.toHexString(device.getVendorId()) + ")");
          json.put("product", device.getProductId() + " (0x" + Integer.toHexString(device.getProductId()) + ")");
          json.put("version", Integer.toHexString(mDeviceVersion));
          return json;
        } catch (JSONException e) {
          return json;
        }
    }

    public int getDeviceVersion() {
        return mDeviceVersion;
    }


    /**
     * Performs a control transaction on endpoint zero for this device.
     * The direction of the transfer is determined by the request type.
     * If requestType & {@link android.hardware.usb.UsbConstants#USB_ENDPOINT_DIR_MASK} is
     * {@link android.hardware.usb.UsbConstants#USB_DIR_OUT}, then the transfer is a write,
     * and if it is {@link android.hardware.usb.UsbConstants#USB_DIR_IN}, then the transfer
     * is a read.
     *
     * @param requestType MSB selects direction, rest defines to whom request is addressed
     * @param request     DFU command ID
     * @param value       0 for commands, >0 for firmware blocks
     * @param index       often 0
     * @param buffer      buffer for data portion of transaction,
     *                    or null if no data needs to be sent or received
     * @param length      the length of the data to send or receive
     * @param timeout     50ms f
     * @return length of data transferred (or zero) for success,
     * or negative value for failure
     */
    public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout) {
        synchronized (this) {
            return mConnection.controlTransfer(requestType, request, value, index, buffer, length, timeout);
        }
    }
}
