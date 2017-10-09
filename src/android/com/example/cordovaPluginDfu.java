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
import android.os.Handler;
import android.os.Message;

import android.util.Log;

import java.util.Date;

public class cordovaPluginDfu extends CordovaPlugin {
  private static final String TAG = "cordovaPluginDfu";
  private Usb usb;
  private Dfu dfu;
  private CallbackContext callbackContext;


  public void initialize(CordovaInterface cordova, CordovaWebView webView) {

    super.initialize(cordova, webView);
    Log.e("ARI4", "Initializing cordovaPluginDfu");
    usb = new Usb(this.cordova.getActivity().getApplicationContext());
    usb.setUsbManager((UsbManager) this.cordova.getActivity().getApplicationContext().getSystemService(this.cordova.getActivity().getApplicationContext().USB_SERVICE));
    Log.e("ARI4", "Initialized usb: "+usb);
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(Usb.ACTION_USB_PERMISSION));
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

    usb.setOnUsbChangeListener(new Usb.OnUsbChangeListener() {
      public void onUsbConnected() {
        final String deviceInfo = usb.getDeviceInfo(usb.getUsbDevice());
        Log.e("ARIM","connected "+deviceInfo);
        //status.setText(deviceInfo);
        dfu.setUsb(usb);
        Log.e("ARIM","erase");
        dfu.massErase();
        Log.e("ARIM","erased?");
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "WHAT");
        pluginResult.setKeepCallback(true);
        if (callbackContext) {
          Log.e("ARIM","cb?");
          callbackContext.sendPluginResult(pluginResult);
          Log.e("ARIM","cb ok");
        }
      }
    });

    dfu = new Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
    Log.e("ARIX","dfu done :)");
    // Handle case where USB device is connected before app launches;
    // hence ACTION_USB_DEVICE_ATTACHED will not occur so we explicitly call for permission
    //usb.requestPermission(this.cordova.getActivity().getApplicationContext(), Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
  }

  //@Override
  //public boolean handleMessage(Message message) {
  //  Log.e("ARIM","messu? "+message);
  //  return false;
  //}

  //@Override


  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
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
