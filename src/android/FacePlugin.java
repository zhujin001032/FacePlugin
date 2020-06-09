package com.jasonhe.facePlugin;
import org.apache.cordova.PermissionHelper;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class FacePlugin extends CordovaPlugin {
    public static final int GRANTED = PackageManager.PERMISSION_GRANTED + 1;
    public static final int DENIED = PackageManager.PERMISSION_DENIED + 1;
    private static final int PERMISSIONS_REQUEST_CODE = 10086;
    private int mCameraPermission = DENIED;
    private int mWritePermission = DENIED;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scanFace")) {
//            cb = callbackContext;
//            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
//            pluginResult.setKeepCallback(true);
//            cb.sendPluginResult(pluginResult);
            String message = args.getString(0);
            this.scanFace(message, callbackContext);
            return true;
        }
        return false;
    }

    private void scanFace(String message, CallbackContext callbackContext) {
//        if (message != null && message.length() > 0) {
//            callbackContext.success(message);
//        } else {
//            callbackContext.error("Expected one non-empty string argument.");
//        }
        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < 23) {
            startDetectionActivity();
            return;
        }
        if (!PermissionHelper.hasPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            startDetectionActivity();
        }else {
            String[] permissions = new String[2];
            permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            permissions[1] = Manifest.permission.CAMERA;
            PermissionHelper.requestPermissions(this, 1000, permissions);

        }
       
    }



    private void startDetectionActivity() {
        cordova.getActivity().startActivityForResult(new Intent(getActivity(), SilentLivenessActivity.class), 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (permissions.length == 1) {
                switch (permissions[0]) {
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        mWritePermission = grantResults[0] + 1;
                        break;
                    case Manifest.permission.CAMERA:
                        mCameraPermission = grantResults[0] + 1;
                        break;
                    default:
                        break;
                }
            } else if (grantResults.length == 2) {
                mWritePermission = grantResults[0] + 1;
                mCameraPermission = grantResults[1] + 1;
            }
            switch ((mWritePermission << 1) + mCameraPermission) {
                case (GRANTED << 1) | DENIED:
                    Toast.makeText(cordova.getContext(), "没有相机权限", Toast.LENGTH_LONG).show();
                    break;
                case (GRANTED << 1) | GRANTED:
                    startDetectionActivity();
                    break;
                case DENIED | GRANTED:
                    Toast.makeText(cordova.getContext(), "没有存储权限", Toast.LENGTH_LONG).show();
                    break;
                case DENIED | DENIED:
                    Toast.makeText(cordova.getContext(), "请开启权限设置", Toast.LENGTH_LONG)
                            .show();
                    break;
                default:
                    break;
            }
        }
    }

}
