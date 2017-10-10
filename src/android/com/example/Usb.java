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

public class Usb {

    final static String TAG = "ARI2";

    private Context mContext;

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbInterface mInterface;
    private int mDeviceVersion;

    /* USB DFU ID's (may differ by device) */
    public final static int USB_VENDOR_ID = 1155;   // VID while in DFU mode 0x0483
    public final static int USB_PRODUCT_ID = 57105; // PID while in DFU mode 0xDF11

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
                            setDevice(device);


                            if (mOnUsbChangeListener != null) {
                                mOnUsbChangeListener.onUsbConnected();
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
                    Log.e(TAG, "attached and req perm");
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    Log.e(TAG, "detached ");
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (mDevice != null && mDevice.equals(device)) {
                        release();
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

                Log.i("ARI", getDeviceInfo(device));
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

        json.put("model", device.getDeviceName());
        json.put("id", device.getDeviceId() + " (0x" + Integer.toHexString(device.getDeviceId()) + ")");
        json.put("class", device.getDeviceClass());
        json.put("subclass", device.getDeviceSubclass());
        json.put("vendor", device.getVendorId() + " (0x" + Integer.toHexString(device.getVendorId()) + ")");
        json.put("product", device.getProductId() + " (0x" + Integer.toHexString(device.getProductId()) + ")");
        json.put("version", Integer.toHexString(mDeviceVersion));
        return json
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
