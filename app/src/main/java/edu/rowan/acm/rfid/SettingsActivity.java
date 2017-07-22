package edu.rowan.acm.rfid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.Date;

import edu.rowan.acm.rfid.Data.ReadWrite;
import edu.rowan.acm.rfid.Data.SaveData;
import edu.rowan.acm.rfid.NFC.NfcManager;
import edu.rowan.acm.rfid.NFC.WriteTagHelper;

/**
 * Settings Activity
 * Allow the user to enter the API password and write to an unformatted tag
 */
public class SettingsActivity extends Activity {
    // Used to write data to the NFC tag
    WriteTagHelper writeHelper;

    NfcManager nfcManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        nfcManager = new NfcManager(this);
        nfcManager.onActivityCreate();

        writeHelper = new WriteTagHelper(this, nfcManager);
        nfcManager.setOnTagWriteErrorListener(writeHelper);
        nfcManager.setOnTagWriteListener(writeHelper);
        nfcManager.onActivityCreate();

        Button writeButton = (Button) findViewById(R.id.write_button);
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = getStringToWrite();
                writeHelper.writeText(text);
            }
        });
        nfcManager.setOnTagReadListener(new NfcManager.TagReadListener() {
            @Override
            public void onTagRead(String tagRead) {
            }
        });

        Button passButton = (Button) findViewById(R.id.password_button);
        passButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText passBtn = (EditText) findViewById(R.id.password_edit_text);
                SaveData.setPassword(passBtn.getText().toString());
            }
        });
        Button queueBtn = (Button) findViewById(R.id.queue_button);
        queueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), SaveData.getSize()+"", Toast.LENGTH_LONG).show();

            }
        });
    }
    public void onNewIntent(Intent intent) {
        nfcManager.onActivityNewIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcManager.onActivityPause();
        try {
            ReadWrite.writeOut(new SaveData(), getApplicationContext());
            // Toast.makeText(getBaseContext(), "Saved", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "failed to save", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcManager.onActivityResume();
    }
    /**
     * Get the string to write to the NFC tag
     * @return The string to write
     */
    private String getStringToWrite() {
        return new Date().toString();
    }
}
