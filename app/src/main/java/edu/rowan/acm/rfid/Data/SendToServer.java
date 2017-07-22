package edu.rowan.acm.rfid.Data;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.rowan.acm.rfid.R;


/**
 * Also Known as Carrier Pigeon <(*_*<)
 * Created by John on 3/7/2016.
 */
public class SendToServer {

    private static final String REGISTRATION_URL = "http://www.profhacks.com/api/post/registration/";
    private static final String EVENT_URL = "http://www.profhacks.com/api/post/event/";

    private static final String LOG_TAG = SendToServer.class.getSimpleName();

    private static Context context;

    public static void send() {
        SendScans api = new SendScans();
        api.execute();
    }

    public static class SendScans extends AsyncTask<Void, Void, Boolean>  {

        private String makeJSon(Scan scan, boolean isEvent) {
            String jsonString = "";
            try {
                JSONObject data = new JSONObject();
                data.put("auth_token", SaveData.getPassword());
                data.put("serial", scan.getGuestId());
                data.put("timestamp", scan.getTime());
                if(isEvent)
                    data.put("message", scan.getLocation());

                jsonString = data.toString();
            } catch (JSONException e) {
                Log.e(LOG_TAG, "makeJSon: " + e.toString());
                e.printStackTrace();
            }
            return jsonString;
        }

        private void sendJSon(String output, String urlOut) throws Exception {

            try {
                URL url = new URL(urlOut);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setReadTimeout(10000); // 10 seconds
                connection.setConnectTimeout(15000); // 15 seconds
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true); // You need to set it to true  if you want to send (output) a request body
                connection.connect();

                OutputStream outputStream = connection.getOutputStream();
                DataOutputStream dStream = new DataOutputStream(outputStream);
                dStream.writeBytes(output); // Writes out the string to the underlying output stream as a sequence of bytes
                dStream.flush(); // Flushes the data output stream.
                dStream.close(); // Closing the output stream.
                Log.d(LOG_TAG, urlOut + " Sent String " + output);

                //====== response from server
                int responseCode = connection.getResponseCode();
                if(responseCode > 199 && responseCode < 208) {
                    Log.d(urlOut + " Response ", responseCode + "");
                } else {
                    Log.d(urlOut + " Response Code bad", responseCode + "");

                    // User is already registered
                    if(responseCode == 400) {
                        SaveData.remove();

                        if(context != null)
                            showErrorToast(context.getString(R.string.error_already_registered));
                        Log.d(urlOut + " not added to queue", "is reg Duplicate" + urlOut);
                    }
                    else {
                        showErrorToast("Error: " + responseCode);
                    }

                    throw new Exception();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(urlOut + " Error ", output + " exception: " + e);
                throw new Exception();
            }
        }


        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                while(SaveData.peek()!= null) {
                    if (SaveData.peek().getLocation().equals("Registration")) {
                        sendJSon(makeJSon(SaveData.peek(), false), REGISTRATION_URL);
                    } else {
                        sendJSon(makeJSon(SaveData.peek(), true), EVENT_URL);
                    }
                    SaveData.remove();
                }
            }
            catch (Exception e){
                Log.d("doInBackground: ", e.toString());
                return false;
            }
            return true;
        }
    }

    /**
     * Display a toast to the user with an error message
     * @param text The message to show
     */
    private static void showErrorToast(final String text) {
        if(context != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public static void setContext(Context context) {
        SendToServer.context = context;
    }

    public static Context getContext() {
        return context;
    }
}

