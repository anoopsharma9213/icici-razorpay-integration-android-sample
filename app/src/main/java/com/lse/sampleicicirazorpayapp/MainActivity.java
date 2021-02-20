package com.lse.sampleicicirazorpayapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.razorpay.Checkout;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultWithDataListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements PaymentResultWithDataListener {

    ProgressBar loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Initiate Checkout Config */
        Checkout.preload(getApplicationContext());

        Button create_order = findViewById(R.id.create_order);
        loader = findViewById(R.id.loader);

        loader.setVisibility(View.GONE);

        create_order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loader.setVisibility(View.VISIBLE);
                new createNewOrder().execute();
            }
        });
    }

    /*Create New Order*/
    @SuppressLint("StaticFieldLeak")
    class createNewOrder extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                URL api = new URL(AppConstants.SERVICE_URL + "?api=serviceorderc");

                HttpURLConnection conn = (HttpURLConnection) api.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer MS4xNGRmZjhmYTgwOTBmZTI3ZDNlNTM1ZGIwNzUwZjA4Mg==");
                conn.setUseCaches(false);
                conn.setDoOutput(true);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8));
                writer.write("{\n" +
                        "    \"service_id\": \"1\",\n" +
                        "    \"latitude\":\"28.8256457\",\n" +
                        "    \"longitude\":\"79.1571723\",\n" +
                        "    \"first_name\": \"Anoop K\",\n" +
                        "    \"last_name\": \"Sharma\",\n" +
                        "    \"email\": \"a@g.com\",\n" +
                        "    \"mobile_number\": \"9716858355\",\n" +
                        "    \"address\": \"AMP Parl, Ashok Nagar Delhi - 110010\",\n" +
                        "    \"room_description\": \"[{\\\"room_type_id\\\":\\\"1\\\",\\\"quantity\\\":\\\"2\\\"},{\\\"room_type_id\\\":\\\"2\\\",\\\"quantity\\\":\\\"1\\\"},{\\\"room_type_id\\\":\\\"3\\\",\\\"quantity\\\":\\\"3\\\"}]\",\n" +
                        "    \"cleaning_time\": \"2021-01-10 12:00:00\",\n" +
                        "    \"comments\": \"Please perform this asap\"\n" +
                        "}");
                writer.flush();
                writer.close();

                if (conn.getResponseCode() == 200) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                } else {
                    return "301";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "301";
            }
        }

        @Override
        protected void onPostExecute(String response) {
            loader.setVisibility(View.GONE);
            AppConstants.debugLog(response);

            if (!response.equals("301")) {
                try {
                    JSONObject response_data = new JSONObject(response);
                    if (response_data.getString("status").equals("200")) {
                        SharedPreferences sharedPreferences = getSharedPreferences(AppConstants.APP_STORAGE, 0);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("ORDER_ID", response_data.getString("order_id"));
                        editor.apply();
                        String pay_key_id = response_data.getString("pay_id");
                        String pay_order_id = response_data.getString("pay_order_id");
                        String pay_order_amount = response_data.getString("pay_order_amount");
                        AppConstants.showToast(getApplicationContext(), response_data.getString("order_id") + " " + pay_key_id + " " + pay_order_id + " " + pay_order_amount);

                        initiatePayment(pay_key_id, pay_order_id, pay_order_amount);
                    } else {
                        AppConstants.showToast(getApplicationContext(), response_data.getString("message"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    AppConstants.showToast(getApplicationContext(), "Sorry! I was not able to process the data! Contact support team");
                }
            } else {
                AppConstants.showToast(getApplicationContext(), "Oh no! I cannot connect to internet! Confirm it please.");
            }
        }
    }

    /* Initiate Payment gateway */
    protected void initiatePayment(String key_id, String pay_order_id, String amount) {
        Checkout checkout = new Checkout();

        // Set API Key ID
        checkout.setKeyID(key_id);

        // Set Your Logo
        checkout.setImage(R.mipmap.ic_launcher);

        final Activity activity = this;

        try {
            JSONObject options = new JSONObject();
            options.put("order_id", pay_order_id);
            options.put("theme.color", "#3399cc");
            options.put("currency", "INR");
            options.put("amount", amount);
            options.put("prefill.email", "a@g.com");
            options.put("prefill.contact", "9716858355");
            checkout.open(activity, options);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onPaymentSuccess(String s, PaymentData paymentData) {
        AppConstants.debugLog(s + " | " + paymentData.getPaymentId() + " | " + paymentData.getOrderId() + " | " + paymentData.getSignature());
        SharedPreferences sharedPreferences = getSharedPreferences(AppConstants.APP_STORAGE, 0);
        String order_id = sharedPreferences.getString("ORDER_ID", "");
        new updateOrderSuccess().execute(order_id, paymentData.getPaymentId(), paymentData.getSignature());
    }

    @Override
    public void onPaymentError(int i, String s, PaymentData paymentData) {
        AppConstants.debugLog(s);
        SharedPreferences sharedPreferences = getSharedPreferences(AppConstants.APP_STORAGE, 0);
        String order_id = sharedPreferences.getString("ORDER_ID", "");
//        AppConstants.debugLog(s + " | " + paymentData.getPaymentId() + " | " + paymentData.getOrderId() + " | " + paymentData.getSignature());
        new updateOrderFailure().execute(order_id, s);
    }

    /* Verify Success and Auto Assign Worker */
    @SuppressLint("StaticFieldLeak")
    class updateOrderSuccess extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                URL api = new URL(AppConstants.SERVICE_URL + "?api=serviceordersu");

                HttpURLConnection conn = (HttpURLConnection) api.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer MS4xNGRmZjhmYTgwOTBmZTI3ZDNlNTM1ZGIwNzUwZjA4Mg==");
                conn.setUseCaches(false);
                conn.setDoOutput(true);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8));
                writer.write("{\n" +
                        "    \"order_id\": \"" + strings[0] + "\",\n" +
                        "    \"razorpay_payment_id\":\"" + strings[1] + "\",\n" +
                        "    \"razorpay_signature\":\"" + strings[2] + "\"\n" +
                        "}");
                writer.flush();
                writer.close();

                if (conn.getResponseCode() == 200) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                } else {
                    return "301";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "301";
            }
        }

        @Override
        protected void onPostExecute(String response) {
            loader.setVisibility(View.GONE);
            AppConstants.debugLog(response);

            if (!response.equals("301")) {
                try {
                    JSONObject response_data = new JSONObject(response);
                    if (response_data.getString("status").equals("200")) {
                        AppConstants.showToast(getApplicationContext(), "Payment Successful! Order Created");
                    } else {
                        AppConstants.showToast(getApplicationContext(), response_data.getString("message"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    AppConstants.showToast(getApplicationContext(), "Sorry! I was not able to process the data! Contact support team");
                }
            } else {
                AppConstants.showToast(getApplicationContext(), "Oh no! I cannot connect to internet! Confirm it please.");
            }
        }
    }

    /* Verify Fail */
    @SuppressLint("StaticFieldLeak")
    class updateOrderFailure extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                URL api = new URL(AppConstants.SERVICE_URL + "?api=serviceorderfl");

                HttpURLConnection conn = (HttpURLConnection) api.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer MS4xNGRmZjhmYTgwOTBmZTI3ZDNlNTM1ZGIwNzUwZjA4Mg==");
                conn.setUseCaches(false);
                conn.setDoOutput(true);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8));
                writer.write("{\n" +
                        "    \"order_id\": \"" + strings[0] + "\",\n" +
                        "    \"reason\": \"" + URLEncoder.encode(strings[1], "UTF-8") + "\"\n" +
                        "}");
                writer.flush();
                writer.close();

                if (conn.getResponseCode() == 200) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                } else {
                    return "301";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "301";
            }
        }

        @Override
        protected void onPostExecute(String response) {
            loader.setVisibility(View.GONE);
            AppConstants.debugLog(response);

            if (!response.equals("301")) {
                try {
                    JSONObject response_data = new JSONObject(response);
                    if (response_data.getString("status").equals("200")) {
                        AppConstants.showToast(getApplicationContext(), "Payment Failed!");
                    } else {
                        AppConstants.showToast(getApplicationContext(), response_data.getString("message"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    AppConstants.showToast(getApplicationContext(), "Sorry! I was not able to process the data! Contact support team");
                }
            } else {
                AppConstants.showToast(getApplicationContext(), "Oh no! I cannot connect to internet! Confirm it please.");
            }
        }
    }
}