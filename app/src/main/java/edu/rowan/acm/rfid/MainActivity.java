package edu.rowan.acm.rfid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import edu.rowan.acm.rfid.Data.ReadWrite;
import edu.rowan.acm.rfid.Data.SaveData;
import edu.rowan.acm.rfid.Data.Scan;
import edu.rowan.acm.rfid.Data.SendToServer;
import edu.rowan.acm.rfid.NFC.NfcManager;

/**
 * Main Activity
 */
public class MainActivity extends AppCompatActivity {

    // Handles reading and writing from NFC
    NfcManager nfcManager;

    // The location of the reader
    private String currentLocation = "unknown location";

    // Stores the taps that have not yet been sent to the server
    protected SaveData saveData = new SaveData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Populate the spinner with locations
        makeSpinner();

        // Create the NFC Manager
        createNfcManager();

        SaveData.setPassword(getString(R.string.api_key));

        // Where to display an error toast
        SendToServer.setContext(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        nfcManager.onActivityNewIntent(intent);
    }

    /**
     * Create the options menu on top of the activity
     * @param menu The options menu to add
     * @return True in order for the menu to be displayed
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    /**
     * Detect the options item that was selected and perform the appropriate action
     * @param item The menu option that was selected
     * @return Whether the tap was consumed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ======================   saveData Stuff ======================================

    /**
     * Add a scan to the list of tags
     * @param tag The tag to add
     */
    private void addScan(String tag) {
        SaveData.add(new Scan(tag, currentLocation));
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Load in save data
            readScanData();
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Failed to Load Scans", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        nfcManager.onActivityResume();
    }

    @Override
    protected void onPause() {
        try {
            // Save the data
            writeScanData();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "failed to save", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        nfcManager.onActivityPause();
        super.onPause();
    }

    /**
     * Read the saved scan data
     * @throws IOException The file was unable to be read
     * @throws ClassNotFoundException The save file was unable to be converted to saveData
     */
    private void readScanData() throws IOException, ClassNotFoundException {
        saveData = ReadWrite.readIn(getApplicationContext(), "Scans.ser");
    }

    /**
     * Save the scan data to a file
     * @throws IOException Unable to write to a file
     */
    private void writeScanData() throws IOException {
        ReadWrite.writeOut(saveData, getApplicationContext());
    }

    /**
     * Create the NFC Manager and the NFC listener
     */
    private void createNfcManager() {
        nfcManager = new NfcManager(this);
        nfcManager.onActivityCreate();
        nfcManager.setOnTagReadListener(new NfcManager.TagReadListener() {
            @Override
            public void onTagRead(String tagId) {
                tagWasScanned(tagId);
            }
        });
    }

    /**
     * When a tag was scanned by the app
     * @param tagId The tag that was read
     */
    private void tagWasScanned(String tagId) {
        showCheckmarkToast();
        vibratePhone(200);
        addScan(tagId);
        sendTagsToServer();
    }

    private void vibratePhone(int milliseconds) {
        ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(milliseconds);
    }

    private void sendTagsToServer() {
        if(SaveData.getPassword().equals("-1")) {
            Toast.makeText(getBaseContext(), "Please Insert Pass Key to Send Data", Toast.LENGTH_LONG).show();
        }
        SendToServer.send();
    }

    /**
     * Display a checkmark toast to notify the user of a successful scan
     */
    private void showCheckmarkToast(){
        // Inflate the toast layout
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_checkmark,
                (ViewGroup) findViewById(R.id.toast_layout_root));
        TextView text = (TextView) layout.findViewById(R.id.toast_text);
        text.setText(R.string.scan_successful);

        // Create the toast
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    /**
     * Make the spinner and add location items to it
     */
    private void makeSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.locations_spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.locations_array, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] locations = getResources().getStringArray(R.array.locations_array);
                currentLocation = locations[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
}
