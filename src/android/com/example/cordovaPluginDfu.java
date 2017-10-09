/**
 */
package com.example;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

import android.util.Log;

import java.util.Date;

public class cordovaPluginDfu extends CordovaPlugin {
  private static final String TAG = "cordovaPluginDfu";
  private Usb usb;

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    Log.e("ARI4", "Initializing cordovaPluginDfu");
    usb = new Usb(this.cordova.getActivity().getApplicationContext());
    Log.e("ARI4", "Initialized usb: "+usb);
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(Usb.ACTION_USB_PERMISSION));
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));


    // Handle case where USB device is connected before app launches;
    // hence ACTION_USB_DEVICE_ATTACHED will not occur so we explicitly call for permission
    usb.requestPermission(this.cordova.getActivity().getApplicationContext(), Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
  }

  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if(action.equals("echo")) {
      String phrase = args.getString(0);
      // Echo back the first argument
      Log.d(TAG, phrase);
    } else if(action.equals("getDate")) {
      // An example of returning data back to the web layer
      final PluginResult result = new PluginResult(PluginResult.Status.OK, (new Date()).toString());
      Log.e("ARI","oujee");
      callbackContext.sendPluginResult(result);
    }
    return true;
  }

}
