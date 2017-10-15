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

//import android.content.Context;
//import android.content.IntentFilter;
import android.content.*;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import java.util.Arrays;
import java.util.Date;


import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

//import org.apache.commons.codec.binary.Base64;

public class cordovaPluginDfu extends CordovaPlugin {
  private static final String TAG = "cordovaPluginDfu";
  private Usb usb;
  private Dfu dfu;
  private CallbackContext cbc;
  private AccountManager manager;
  private Timer timer;
  private MyTimerTask myTimerTask;

  private boolean send2JS(JSONObject msg) {
    if (cbc != null) {
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, msg);
      pluginResult.setKeepCallback(true);
      Log.e("ARIMx","cb?");
      cbc.sendPluginResult(pluginResult);
      Log.e("ARIMx","cb ok");
      return true;
    } else {
      Log.e("ARIMx","NO CBC "+cbc);
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
        Log.e("ARIM","connected DFU listener"+deviceInfo);
        json.put("type","dfu");
        //status.setText(deviceInfo);
        dfu.setUsb(usb);
        send2JS(deviceInfo);
      }
    });
    usb.setOnUsbChangeListeners(new Usb.OnUsbChangeListeners() {
      public void onUsbConnecteds(String dada) {
        JSONObject deviceInfo = usb.getDeviceInfo(usb.getUsbDevice());
        json.put("type","serial");
        Log.e("ARIM","connected SERIAL '"+dada+"'"+deviceInfo);
        //status.setText(deviceInfo);
        send2JS(deviceInfo);
      }
    });

    dfu = new Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
    manager =AccountManager.get( cordova.getActivity().getApplicationContext() );
    Log.e("ARIX","dfu done :)");
    // Handle case where USB device is connected before app launches;
    // hence ACTION_USB_DEVICE_ATTACHED will not occur so we explicitly call for permission
  }

  //@Override
  //public boolean handleMessage(Message message) {
  //  Log.e("ARIM","messu? "+message);
  //  return false;
  //}

  //@Override
  class MyTimerTask extends TimerTask {

   @Override
   public void run() {
     Log.e("ARITICK","TOCK");
   }

  }


  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (action.equals("getAuth")) {
      Account[] accounts = manager.getAccountsByType("com.google");
      final JSONObject json = new JSONObject();
      for (final Account account : accounts) {
        String AUTH_TOKEN_TYPE = args.getString(0);
        AccountManagerFuture<Bundle> accountManagerFuture;
        accountManagerFuture=manager.getAuthToken(account, AUTH_TOKEN_TYPE, null, cordova.getActivity(), null,null);
        Bundle authTokenBundle;
        try {
          authTokenBundle  = accountManagerFuture.getResult();
          String token = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN).toString();
          Log.e("ARIQQ","got future: "+authTokenBundle);
          // Now you can use the Tasks API...
          Log.e("ARIQQ","got token: "+token);
          if (args.getString(1)=="invalidate") {
            manager.invalidateAuthToken("com.google", token);
            //token = updateToken(false);
          }

          try {
            json.put("provider","google");
            json.put("name",account.name);
            json.put("token",token);
          }  catch (Exception e) {
            Log.e("ARIQQ","json crap: "+e);
          }
          final PluginResult result = new PluginResult(PluginResult.Status.OK, json);
          callbackContext.sendPluginResult(result);
          break;
        }  catch (Exception e) {
          Log.e("ARIQQ","fut fail: "+e);
        }
      }
      final PluginResult result = new PluginResult(PluginResult.Status.OK, json);
      callbackContext.sendPluginResult(result);
    } else if (action.equals("massErase")) {
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
        int bytes=Integer.parseInt(args.getString(1));
        byte[] ret = dfu.readBytes( start,bytes ) ;
        String s="";
        for (int i=0;i<bytes;i++) {
          s=s+String.format("%02X", ret[i]);
        }
        json.put("startAddress",start);
        json.put("bytes",bytes);
        json.put("data",s);
      } catch (Exception e) {
        Log.e("ARI","readbyte errs: "+e);
        return true;
      }
      final PluginResult result = new PluginResult(PluginResult.Status.OK, json);
      callbackContext.sendPluginResult(result);
      send2JS(json);
    } else if(action.equals("registerReceiver")) {
      Log.e("ARI","registerReceiver: "+args);

      //if (cbc==null) {
        //myTimerTask = new MyTimerTask();
        //timer.schedule(myTimerTask, 1000, 5000);
      //}
      if (cbc==null) {
        cbc = callbackContext;
        Log.e("ARIMx","saved cbc "+cbc);
      }
      JSONObject json = new JSONObject();
      json.put("foo", "active");
      send2JS(json);
      usb.requestPermission(this.cordova.getActivity().getApplicationContext(), Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
    }
    return true;
  }

}
