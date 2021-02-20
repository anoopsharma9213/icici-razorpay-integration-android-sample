package com.lse.sampleicicirazorpayapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class AppConstants {

    public static String APP_STORAGE = "ICICIRAZORPAYSAMPLEAPP";
    public static String SERVICE_URL = "http://piyuservesytech.xyz/services/consumer/index.php";

    public static void debugLog(String Logdata) {
        String LOG_TAG = "ICICIRAZORPAYSAMPLEAPP";
        Log.d(LOG_TAG, Logdata);
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
