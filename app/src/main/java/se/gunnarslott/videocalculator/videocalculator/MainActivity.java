package se.gunnarslott.videocalculator.videocalculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";

    private int currentBitrateIndex = 0;
    private int currentLengthIndex = 0;
    private int currentFileSizeIndex = 0;

    private double currentBitrate = 0.0;
    private double currentLength = 0.0;
    private double currentFileSize = 0.0;

    //Eq. to 1000 Gpbs, 1 000 000 000 000,00 (One thousand billion) TB and 1000 years
    private static double MAX_BITRATE = 1000000000.0;
    private static double MAX_FILE_SIZE = 1048576000000000000.0;
    private static double MAX_LENGTH = 31536000000.0;

    private boolean bitrate_to_high = false;
    private boolean filesize_to_large = false;
    private boolean length_to_long = false;

    //Baseline units are seconds, Megabytes and kbps, and this is the divide/multiply factor for conversion
    int[] bitrateIndex = {1,1000};
    int[] fileSizeIndex = {1,1024,1048576};
    int[] lengthIndex = {60,3600};

    private EditText etBitrate;
    private EditText etLength;
    private EditText etFileSize;

    private RadioButton rbBitrate;
    private RadioButton rbLength;
    private RadioButton rbFileSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Since these initiations contains references to project resources they need to be within onCreate
        //Strings for "share" functionality. Needs to be coordinated with the resource arrays that populates spinners
        final String[] bitrateNames = {getString(R.string.Kbps),getString(R.string.Mbps)};
        final String[] fileSizeNames = {getString(R.string.MB),getString(R.string.GB),getString(R.string.TB)};
        final String[] lengthNames = {getString(R.string.minutes),getString(R.string.hours)};

        //Instantiate GUI components
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etFileSize = (EditText) findViewById(R.id.etFileSize);
        if (etFileSize != null) {
            etFileSize.setEnabled(false);
        }
        etBitrate = (EditText) findViewById(R.id.etBitrate);
        etLength = (EditText) findViewById(R.id.etLength);

        rbBitrate = (RadioButton) findViewById(R.id.rbBitrate);
        rbLength = (RadioButton) findViewById(R.id.rbLength);
        rbFileSize = (RadioButton) findViewById(R.id.rbFileSize);
        if (rbFileSize != null) {
            rbFileSize.setChecked(true);
        }

        //Instantiate spinners and implement onItemSelected
        Spinner btr_spinner;
        Spinner length_spinner;
        Spinner size_spinner;
        ArrayAdapter<CharSequence> adapter_bitrate;
        ArrayAdapter<CharSequence> adapter_length;
        ArrayAdapter<CharSequence> adapter_size;

        //Bitrate units spinner
        btr_spinner = (Spinner) findViewById(R.id.spnr_bitrate_units);

        adapter_bitrate = ArrayAdapter.createFromResource(this, R.array.bitrate_units, android.R.layout.simple_spinner_item);
        adapter_bitrate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assert btr_spinner != null;
        btr_spinner.setAdapter(adapter_bitrate);

        btr_spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
               public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                   double temp;
                    NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());

                    //currentBitrate is always in kbps (kilobits per second)
                    currentBitrateIndex = position;

                try {

                    temp = nf.parse(etBitrate.getText().toString()).doubleValue();
                    currentBitrate = temp * bitrateIndex[currentBitrateIndex];

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                   if (rbBitrate.isChecked()){
                        calc(-1);
                    } else {
                        calc(0);
                    }
                }

               @Override
               public void onNothingSelected(AdapterView<?> parent) {
                   //Cannot happen
               }
           }
        );

        //Length units spinner
        length_spinner = (Spinner) findViewById(R.id.spnr_lenght_units);

        adapter_length = ArrayAdapter.createFromResource(this, R.array.length_units, android.R.layout.simple_spinner_item);
        adapter_length.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assert length_spinner != null;
        length_spinner.setAdapter(adapter_length);

        length_spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
               public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    double temp;
                    NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());

                    //currentLength is always in seconds
                    currentLengthIndex = position;

                try {

                    //localized read from editText
                    temp = nf.parse(etLength.getText().toString()).doubleValue();
                    currentLength = temp * lengthIndex[currentLengthIndex];

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                   if (rbLength.isChecked()){
                       calc(-1);
                   } else {
                       calc(1);
                   }
               }

               @Override
               public void onNothingSelected(AdapterView<?> parent) {
                    //Cannot happen
               }
           }
        );

        //Size units spinner
        size_spinner = (Spinner) findViewById(R.id.spnr_size_units);

        adapter_size = ArrayAdapter.createFromResource(this, R.array.filesize_units, android.R.layout.simple_spinner_item);
        adapter_size.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assert size_spinner != null;
        size_spinner.setAdapter(adapter_size);

        size_spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                double temp;
                NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());

                //currentFileSize is always in Megabytes
                currentFileSizeIndex = position;

                try {
                    //localized read from editText
                    temp = nf.parse(etFileSize.getText().toString()).doubleValue();
                    currentFileSize = Math.round(temp * fileSizeIndex[currentFileSizeIndex]);

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (rbFileSize.isChecked()){
                    calc(-1);
                } else {
                    calc(2);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Cannot happen
            }
        }
        );

        //Instantiate floating Action button and override onClick
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    String message = "";

                    //Create intent and string for sending the calc to any text receiving app on the device
                    if (rbFileSize.isChecked()) {
                        //bitrate * lenght = file size
                        message = Double.toString(currentBitrate / bitrateIndex[currentBitrateIndex]) + " " + bitrateNames[currentBitrateIndex] + " * " + Double.toString(currentLength / lengthIndex[currentLengthIndex]) + " " + lengthNames[currentLengthIndex] + " = " + Double.toString(currentFileSize / fileSizeIndex[currentFileSizeIndex]) + " " + fileSizeNames[currentFileSizeIndex];

                    } else if (rbLength.isChecked()){
                        //file size / bitrate = length
                        message = Double.toString(currentFileSize / fileSizeIndex[currentFileSizeIndex]) + " " + fileSizeNames[currentFileSizeIndex] + " / " + Double.toString(currentBitrate / bitrateIndex[currentBitrateIndex]) + " " + bitrateNames[currentBitrateIndex] + " = " + Double.toString(currentLength / lengthIndex[currentLengthIndex]) + " " + lengthNames[currentLengthIndex];

                    } else if (rbBitrate.isChecked()){
                        //file size / length = bitrate
                        message =  Double.toString(currentFileSize / fileSizeIndex[currentFileSizeIndex]) + " " + fileSizeNames[currentFileSizeIndex] + " / " + Double.toString(currentLength / lengthIndex[currentLengthIndex]) + " " + lengthNames[currentLengthIndex] + " = " + Double.toString(currentBitrate / bitrateIndex[currentBitrateIndex]) + " " + bitrateNames[currentBitrateIndex];

                    }

                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }
            });
        }

        //Add text change listeners to bitrate edit text fields
        etBitrate.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Don't bother
            }

            //Calc file size if bitrate value changed
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Log.d(TAG, "etBitrate onChange");

                double temp;
                NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());

                    try {

                        //localized read from editText
                        temp = nf.parse(etBitrate.getText().toString()).doubleValue();

                        if (temp * bitrateIndex[currentBitrateIndex] > MAX_BITRATE){
                            Log.d(TAG, "Bitrate value to large");
                            bitrateToHigh();
                        } else if (bitrate_to_high) {
                            Log.d(TAG, "Bitrate not max value " + temp + " v.s. " + MAX_BITRATE);
                            bitrateRestore();
                            currentBitrate = Math.round(temp * bitrateIndex[currentBitrateIndex]);
                            //Call calc with mode = 0 (calc bitrate) to avoid writing result to edit text to trigger new calculation
                            calc(0);
                        } else {
                            currentBitrate = Math.round(temp * bitrateIndex[currentBitrateIndex]);
                            //Call calc with mode = 0 (calc bitrate) to avoid writing result to edit text to trigger new calculation
                            calc(0);
                        }


                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

            }

            @Override
            public void afterTextChanged(Editable s) {
                //Don't bother
                Log.d(TAG, " etBitrate afterTextChanged");
            }
        });

        etLength.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Don't bother
            }

            //Calc file size if value changed
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Log.d(TAG, "etLength onChange");

                double temp;
                NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
                try {

                    //localized read from editText
                    temp = nf.parse(etLength.getText().toString()).doubleValue();

                    if (temp * lengthIndex[currentLengthIndex] > MAX_LENGTH){
                        Log.d(TAG, "Length value to large");
                        lengthToLong();
                    } else if (length_to_long) {
                        lengthToLongRestore();
                        currentLength = Math.round(temp * lengthIndex[currentLengthIndex]);
                        //Call calc with mode = 1 (calc length) to avoid writing result to edit text to trigger new calculation
                        calc(1);
                    } else {
                        currentLength = Math.round(temp * lengthIndex[currentLengthIndex]);
                        //Call calc with mode = 1 (calc length) to avoid writing result to edit text to trigger new calculation
                        calc(1);
                    }


                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //Don't bother
                Log.d(TAG, " etLength afterTextChanged");
            }
        });

        etFileSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Don't bother
            }

            //Calc file size if value changed
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                double temp;
                NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());

                try {

                    //localized read from editText
                    temp = nf.parse(etFileSize.getText().toString()).doubleValue();

                    if (temp * fileSizeIndex[currentFileSizeIndex] > MAX_FILE_SIZE){
                        Log.d(TAG, "File size max value");
                        filesizeToLarge();
                    } else if (filesize_to_large) {
                        Log.d(TAG, "File size not max value " + temp + " v.s. " + MAX_FILE_SIZE);
                        filesizeToLargeRestore();
                        currentFileSize = Math.round(temp * fileSizeIndex[currentFileSizeIndex]);
                        //Call calc with mode = 2 (calc file size) to avoid writing result to edit text to trigger new calculation
                        calc(2);
                    } else {
                        currentFileSize = Math.round(temp * fileSizeIndex[currentFileSizeIndex]);
                        //Call calc with mode = 2 (calc file size) to avoid writing result to edit text to trigger new calculation
                        calc(2);
                    }


                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                //Don't bother
                Log.d(TAG, " etFileSize afterTextChanged");
            }
        });


    } //onCreate

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.rbBitrate:
                if (checked)
                    Log.d(TAG, "Radio button Bitrate clicked");
                    etBitrate.setEnabled(false);
                    etLength.setEnabled(true);
                    etFileSize.setEnabled(true);
                    break;

            case R.id.rbLength:
                if (checked)
                    Log.d(TAG, "Radio button Length clicked");
                    etBitrate.setEnabled(true);
                    etLength.setEnabled(false);
                    etFileSize.setEnabled(true);
                    break;

            case R.id.rbFileSize:
                if (checked)
                    Log.d(TAG, "Radio button FileSize clicked");
                    etBitrate.setEnabled(true);
                    etLength.setEnabled(true);
                    etFileSize.setEnabled(false);
                    break;
        }
    }

    //Print methods to centralize locale formatting and rounding and make use of the current Unit chosen by the user
    public String printBitrate(){
        double temp;
        temp = currentBitrate / bitrateIndex[currentBitrateIndex];
        return String.format(Locale.getDefault(), "%.2f", temp);
    }

    public String printLength(){
        double temp;
        temp = currentLength / lengthIndex[currentLengthIndex];
        return String.format(Locale.getDefault(), "%.2f", temp);
    }

    public String printFileSize(){
        double temp;
        temp = currentFileSize / fileSizeIndex[currentFileSizeIndex];
        return String.format(Locale.getDefault(), "%.2f", temp);
    }

    //Calculations of bitrate, file size or video length depending on what RadioButton is ticked
    public void calc(int mode) {

        Log.d(TAG, "Calc() mode = " + String.valueOf(mode));

        double temp;

        //if mode = 0, bitrate unit spinner change = no calculation
        if (rbBitrate.isChecked() && mode != 0) {

            Log.d(TAG, "calc Bitrate");

            try {

                Log.d(TAG, "currentBitrate " + currentBitrate);
                Log.d(TAG, "currentLength " + currentLength);
                Log.d(TAG, "currentFileSize " + currentFileSize);

                //Calc bitrate in Megabytes per second
                temp = currentFileSize / currentLength;
                Log.d(TAG, "MB per second " + temp);

                if (temp * 8000 <= MAX_BITRATE) {
                    //Save current bitrate in kbps
                    currentBitrate = temp * 8000;
                    Log.d(TAG, "new currentBitrate " + currentBitrate);

                    if (bitrate_to_high) {
                        bitrateRestore();
                    }

                    //Print current bitrate in chosen unit to screen
                    etBitrate.setText(printBitrate());
                } else bitrateToHigh();

            } catch(NumberFormatException nfe) {

                //Most likely one of the editViews are empty
                System.out.println("Could not parse " + nfe);

            }
        }

        //if mode = 1, length unit spinner change = no calculation
        else if (rbLength.isChecked() && mode != 1) {

            Log.d(TAG, "calc Length");

            try {

                Log.d(TAG, "currentBitrate " + currentBitrate);
                Log.d(TAG, "currentLength " + currentLength);
                Log.d(TAG, "currentFileSize " + currentFileSize);

                //Calc length in seconds
                temp = currentFileSize/(currentBitrate / 8000);
                Log.d(TAG, "Length in seconds " + temp);

                if (temp <= MAX_LENGTH) {
                    //Save current length in seconds
                    currentLength = temp;
                    Log.d(TAG, "new currentLength " + currentLength);

                    if (length_to_long) {
                        lengthToLongRestore();
                    }

                    //Print length in chosen unit to screen
                    etLength.setText(printLength());
                } else lengthToLong();


            } catch(NumberFormatException nfe) {

                //Most likely one of the editViews are empty
                System.out.println("Could not parse " + nfe);

            }
        }

        //if mode = 2, filesize unit spinner change = no calculation
        if (rbFileSize.isChecked() && mode != 2) {

            Log.d(TAG, "calc FileSize");

            try {

                Log.d(TAG, "currentBitrate " + currentBitrate);
                Log.d(TAG, "currentLength " + currentLength);
                Log.d(TAG, "currentFileSize " + currentFileSize);

                //Calc file size in MB
                temp = (currentBitrate / 8000) * currentLength;
                Log.d(TAG, "filesize in MB " + temp);

                if (temp <= MAX_FILE_SIZE) {
                    //Save current file size in MB
                    currentFileSize = temp;

                    if (filesize_to_large) {
                        filesizeToLargeRestore();
                    }

                    //Print file size in chosen unit to screen
                    etFileSize.setText(printFileSize());
                } else filesizeToLarge();

            } catch(NumberFormatException nfe) {

                //Most likely one of the editViews are empty
                System.out.println("Could not parse " + nfe);
            }
        }


    } //END OF calc()

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {

            showAboutDialog();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void bitrateToHigh(){

        //Show error message and turn background red and text bold
        showToast(getString(R.string.br_to_large));
        etBitrate.setBackgroundColor(Color.RED);
        etBitrate.setTypeface(null, Typeface.BOLD);
        bitrate_to_high = true;

    }
    private void bitrateRestore(){

        //Restore to normal background and text
        etBitrate.setBackgroundColor(Color.TRANSPARENT);
        etBitrate.setTypeface(null, Typeface.NORMAL);
        bitrate_to_high = false;

    }

    private void lengthToLong(){

        //Show error message and turn background red and text bold
        showToast(getString(R.string.length_to_large));
        etLength.setTypeface(null, Typeface.BOLD);
        etLength.setBackgroundColor(Color.RED);
        length_to_long = true;

    }
    private void lengthToLongRestore(){

        //Restore to normal text
        etLength.setTypeface(null, Typeface.NORMAL);
        etLength.setBackgroundColor(Color.TRANSPARENT);
        length_to_long = false;

    }

    private void filesizeToLarge(){

        //Show error message and turn text red and bold
        showToast(getString(R.string.fs_to_large));
        etFileSize.setTypeface(null, Typeface.BOLD);
        etFileSize.setBackgroundColor(Color.RED);
        filesize_to_large = true;

    }
    private void filesizeToLargeRestore(){

        //Restore to normal text
        etFileSize.setTypeface(null, Typeface.NORMAL);
        etFileSize.setBackgroundColor(Color.TRANSPARENT);
        filesize_to_large = false;

    }

    private void showToast(String s){

        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, s, duration);
        toast.show();

    }

    private void showAboutDialog() {

        //Create dialog
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

        //Get version number
        PackageInfo pInfo = null;

        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        assert pInfo != null;
        String heading = String.format("%s %s", getString(R.string.app_name), pInfo.versionName);
        String message = getString(R.string.about_text);
        alertDialog.setTitle(heading);
        alertDialog.setMessage(message);

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.positive),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();

    }
}
