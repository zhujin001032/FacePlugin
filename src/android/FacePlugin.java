package com.jasonhe.facePlugin;
import org.apache.cordova.PermissionHelper;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.Toast;


import com.sensetime.liveness.silent.SilentLivenessActivity;
import com.sensetime.senseid.sdk.liveness.silent.common.type.ResultCode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;


import static android.app.Activity.RESULT_CANCELED;

/**
 * This class echoes a string called from JavaScript.
 */
public class FacePlugin extends CordovaPlugin implements SilentLivenessActivity.ScanFaceListener {
    private static final String TAG = "FacePlugin";
    private static final int PERMISSIONS_REQUEST_CODE = 10086;
    private boolean mInterrupt = false; // 中断
    private SilentLivenessActivity fragment = null;
    private int containerViewId = 2030; //<- set to random number to prevent conflict with other plugins

    Intent silentLivenessIntent;
    CallbackContext callbackContext;
    JSONArray args;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scanFace")) {
            this.callbackContext = callbackContext;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
            this.args = args;
            this.checkPermissions();
            return true;
        } else  if (action.equals("hideCamera")) {
            return this.hideCamera(callbackContext);

        } else  if (action.equals("showCamera")) {
            return this.showCamera(callbackContext);
        }
        return false;
    }

    private boolean hasView(CallbackContext callbackContext) {
        if(fragment == null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR,"预览不存在");
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
            return false;
        }

        return true;
    }


    private boolean hideCamera(CallbackContext callbackContext) {
        if(this.hasView(callbackContext) == false){
            return true;
        }
        fragment.stop = true;
        FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(fragment);
        fragmentTransaction.commit();

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        pluginResult.setKeepCallback(true);
        this.callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private boolean showCamera(CallbackContext callbackContext) {
        if(this.hasView(callbackContext) == false){
            return true;
        }
        fragment.stop = false;
        fragment.reBegin(ResultCode.OK);
        FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.show(fragment);
        fragmentTransaction.commit();

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        pluginResult.setKeepCallback(true);
        this.callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    @SuppressLint("InlinedApi")
    private boolean startCamera(int x, int y, int width, int height) {
        Log.d(TAG, "start camera action");

        if (fragment != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "相机已经开启");
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        fragment = new SilentLivenessActivity();
        DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();

        // offset
        int computedX = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, x, metrics);
        int computedY = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, y, metrics);

        // size
        int computedWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, metrics);
        int computedHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, metrics);

        fragment.setRect(computedX, computedY, computedWidth, computedHeight);
        fragment.setEventListener(this);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //create or update the layout params for the container view
                FrameLayout containerView = (FrameLayout)cordova.getActivity().findViewById(containerViewId);
                if(containerView == null){
                    containerView = new FrameLayout(cordova.getActivity().getApplicationContext());
                    containerView.setId(containerViewId);

                    FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                    cordova.getActivity().addContentView(containerView, containerLayoutParams);
                }
                //set camera back to front
//                containerView.setAlpha(opacity);
                containerView.bringToFront();
                //add the fragment to the container
                FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(containerView.getId(), fragment);
                fragmentTransaction.commit();
            }
        });

        return true;
    }



    @SuppressLint("InlinedApi")
    private void checkPermissions() {
        final Intent silentLivenessIntent = new Intent(cordova.getActivity(), SilentLivenessActivity.class);
        this.silentLivenessIntent = silentLivenessIntent;

        if (!PermissionHelper.hasPermission(this,Manifest.permission.CAMERA)){
            Toast.makeText(cordova.getContext(), "没有相机权限", Toast.LENGTH_LONG).show();
            String[] permissions = new String[2];
            permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            permissions[1] = Manifest.permission.CAMERA;
            PermissionHelper.requestPermissions(this, PERMISSIONS_REQUEST_CODE, permissions);
        }else {

            try {
                this.startCamera(this.args.getInt(0), args.getInt(1), args.getInt(2), args.getInt(3));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }


  

    @Override
    public void onScanFace(String face) {

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, face);
        pluginResult.setKeepCallback(true);
        this.callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onScanFaceError(String message) {

        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
        pluginResult.setKeepCallback(true);
        this.callbackContext.sendPluginResult(pluginResult);

    }

    @Override
    public void onScanFaceTips(String message) {

        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
        pluginResult.setKeepCallback(true);
        this.callbackContext.sendPluginResult(pluginResult);
    }
}
