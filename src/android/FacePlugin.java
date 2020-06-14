package com.jasonhe.facePlugin;
import org.apache.cordova.PermissionHelper;
import android.Manifest;
import android.annotation.SuppressLint;
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

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.sensetime.liveness.silent.SilentLivenessActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;


import static android.app.Activity.RESULT_CANCELED;

/**
 * This class echoes a string called from JavaScript.
 */
public class FacePlugin extends CordovaPlugin {

    private static final int PERMISSIONS_REQUEST_CODE = 10086;
    private boolean mInterrupt = false; // 中断
    Intent silentLivenessIntent;
    CallbackContext callbackContext;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scanFace")) {
            this.callbackContext = callbackContext;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
            String message = args.getString(0);
            this.scanFace(message, callbackContext);
            return true;
        }
        return false;
    }
    @SuppressLint("InlinedApi")
    private void scanFace(String message, CallbackContext callbackContext) {
//        if (message != null && message.length() > 0) {
//            callbackContext.success(message);
//        } else {
//            callbackContext.error("Expected one non-empty string argument.");
//        }
        checkPermissions();

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
            startDetectionActivity();
        }

    }


    @SuppressLint("InlinedApi")
    private void startDetectionActivity() {

        cordova.startActivityForResult(this, this.silentLivenessIntent, 0);

    }
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (resultCode) {
            case RESULT_CANCELED:
            {
                this.mInterrupt = false;
                this.callbackContext.error("RESULT_CANCELED");
            }

            break;
            case SilentLivenessActivity.CANCEL_INITIATIVE:{
                this.mInterrupt = true;
                this.callbackContext.error("CANCEL_INITIATIVE");
            }

            break;
            default:
            {
                this.mInterrupt = false;
                if (intent != null && !intent.getBooleanExtra(SilentLivenessActivity.RESULT_DEAL_ERROR_INNER, false)) {
                    final File imageResultFile = new File(SilentLivenessActivity.FILE_IMAGE);
                    if (imageResultFile.exists()) {
                        final Bitmap source = BitmapFactory.decodeFile(SilentLivenessActivity.FILE_IMAGE);

                        callbackContext.success("data:image/jpeg;base64,base64" + getBase64OfImage(source));
                    }

                }
            }

            break;
        }
    }
    private Bitmap getResizedBitmap(Bitmap bm, float factor) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(factor, factor);
        // recreate the new Bitmap
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

    private String getBase64OfImage(Bitmap bm) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    /**
     * Choosing a picture launches another Activity, so we need to implement the
     * save/restore APIs to handle the case where the CordovaActivity is killed by the OS
     * before we get the launched Activity's result.
     *
     *
     */
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }


    public void onRequestPermissionResult(int requestCode,
                                          String[] permissions,
                                          int[] grantResults) throws JSONException {

        try {
            for(int i=0;i<grantResults.length;i++){
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Log.d("FacePlugin", "Permission not granted by the user");
                    // Tell the JS layer that something went wrong...
                    this.callbackContext.error("PERMISSION_DENIED");

                    return;
                }
            }

            switch (requestCode) {
                case PERMISSIONS_REQUEST_CODE:
                    Log.d("FacePlugin", "User granted the permission for READ_EXTERNAL_STORAGE");
                    cordova.startActivityForResult(this, this.silentLivenessIntent, 0);

                    break;
            }
        } catch (Exception e) {
            Log.e("ImagePicker:", e.getMessage());
        }
    }

}
