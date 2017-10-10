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
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.codec.binary.Base64;

public class cordovaPluginDfu extends CordovaPlugin {
  private static final String TAG = "cordovaPluginDfu";
  private Usb usb;
  private Dfu dfu;
  private CallbackContext cbc;

  private boolean send2JS(JSONObject msg) {
    if (cbc != null) {
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, msg);
      pluginResult.setKeepCallback(true);
      Log.e("ARIMx","cb?");
      cbc.sendPluginResult(pluginResult);
      Log.e("ARIMx","cb ok");
      return true;
    } else {
      Log.e("ARIMx","NO CBC");
      return false;
    }
  }

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {

    super.initialize(cordova, webView);
    cbc = null;
    Log.e("ARI4", "Initializing cordovaPluginDfu");
    usb = new Usb(this.cordova.getActivity().getApplicationContext());
    usb.setUsbManager((UsbManager) this.cordova.getActivity().getApplicationContext().getSystemService(this.cordova.getActivity().getApplicationContext().USB_SERVICE));
    Log.e("ARI4", "Initialized usb: "+usb);
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(Usb.ACTION_USB_PERMISSION));
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
    this.cordova.getActivity().getApplicationContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

    usb.setOnUsbChangeListener(new Usb.OnUsbChangeListener() {
      public void onUsbConnected() {
        JSONObject deviceInfo = usb.getDeviceInfo(usb.getUsbDevice());
        Log.e("ARIM","connected ");
        //status.setText(deviceInfo);
        dfu.setUsb(usb);
        send2JS(deviceInfo);
      }
    });

    dfu = new Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
    Log.e("ARIX","dfu done :)");
    // Handle case where USB device is connected before app launches;
    // hence ACTION_USB_DEVICE_ATTACHED will not occur so we explicitly call for permission
    usb.requestPermission(this.cordova.getActivity().getApplicationContext(), Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
  }

  //@Override
  //public boolean handleMessage(Message message) {
  //  Log.e("ARIM","messu? "+message);
  //  return false;
  //}

  //@Override


  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (action.equals("massErase")) {
      long ret=dfu.massErase();
      JSONObject json = new JSONObject();
      json.put("eraseTime", ret);
      final PluginResult result = new PluginResult(PluginResult.Status.OK, json);
      callbackContext.sendPluginResult(result);

    } else if (action.equals("readBytes")) {
      Log.e("ARI","readBytes,. "+args);
      JSONObject json = new JSONObject();
      //Arrays.fill( ret, (byte) 1 );
      try {
        int start=Integer.parseInt(args.getString(0));
        Log.e("ARI","readBytes. start= "+start);
        int bytes=Integer.parseInt(args.getString(1));
        Log.e("ARI","readBytes. start= "+bytes);
        byte[] ret = dfu.readBytes( start,bytes ) ;
        Log.e("ARI","readBytes. read?? ");
        for (int i=0;i<ret.length;i++)
          json.put("byte"+i, ret[i]);
        json.put("bytes",Base64.encodeBase64String(ret) )
      } catch (Exception e) {
        Log.e("ARI","dah "+e);
        return true;
      }
      final PluginResult result = new PluginResult(PluginResult.Status.OK, json);
      callbackContext.sendPluginResult(result);
    } else if(action.equals("registerReceiver")) {
      Log.e("ARI","registerReceiver: "+args);
      cbc = callbackContext;
      JSONObject json = new JSONObject();
      json.put("foo", "active");
      send2JS(json);
    }
    return true;
  }

}
