package com.example.genesis.e_motion;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.*;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;
import android.view.View.*;
import android.view.MotionEvent;
import android.text.TextUtils;


public class Calibrator extends AppCompatActivity {

    private static final String TAG = "ArduinoAccessory";

    private static final String
            ACTION_USB_PERMISSION = "com.example.bme590.android_dualworldmemory.action.USB_PERMISSION";

    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    private ToggleButton buttonLED;

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    final Context context = this; //Need self-referential context for dialog alerts

    /** Custom Communication/Timing Variables and Classes **/
    private boolean write_notRead_enable;
    private String dataFromArduino;
    private String dataToArduino;
    private boolean readingSecond;
    private int dataBack[];

    private TextView debug;

    // will contain in order: smile1,smile2,frown1,frown2
    private double thresholds[]; //thresholds for each electrode and each expression

    private EditText nameEdit;
    private String originalName;

    //END VARIABLE DEFINITIONS

    //BEGIN OVERRIDDEN FUNCTION DEFINITIONS

    /** Android Initialization and Communication Functions **/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d(TAG, "permission denied for accessory "
                                + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY); {
                    if (accessory != null && accessory.equals(mAccessory)) {
                        closeAccessory();
                    }
                }
            }
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        if (mAccessory != null) {
            return mAccessory;
        } else {
            return super.onRetainNonConfigurationInstance();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }

        write_notRead_enable = true;
        readingSecond = false;
        dataBack = new int[] {0, 0};

        setContentView(R.layout.activity_calibrator);

        thresholds = new double[] { 1000, 1000, 1000, 1000 }; //thresholds for each electrode and each expression
        //should assign unreasonable value here that can be checked

        Intent intent = getIntent();

        nameEdit = (EditText) findViewById(R.id.NameInput);
        originalName = intent.getStringExtra(Monitor.SENT_NAME);
        nameEdit.setText(originalName);

        nameEdit.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (nameEdit.getText().toString().equals(originalName)) {
                    nameEdit.setText("");
                }
                return false;
            }
        });

        nameEdit.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && TextUtils.isEmpty(nameEdit.getText().toString())) {
                    nameEdit.setText(originalName);
                } else if (hasFocus && nameEdit.getText().toString().equals(originalName)) {
                    nameEdit.setText(originalName);
                }
            }
        });

        debug = (TextView) findViewById(R.id.trainDebug);
        String personVals = new String("Created");
        debug.setText(personVals);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_monitor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mInputStream != null && mOutputStream != null) {
            return;
        }

        UsbAccessory accessories[] = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory,mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d(TAG, "mAccessory is null");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        closeAccessory();
        super.onDestroy();
    }

    //GIVEN FUNCTIONS

    private void openAccessory(UsbAccessory mAccessory2) {
        mFileDescriptor = mUsbManager.openAccessory(mAccessory2);
        if (mFileDescriptor != null) {
            mAccessory = mAccessory2;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Log.d(TAG, "accessory opened");
        } else {
            Log.d(TAG, "accessory open fail");
        }
    }

    private void closeAccessory() {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    //COMMUNICATION FUNCTIONS

    public void communicateWithArduino(){
        androidWrite(47);//47 will be the cue for Arduino to run once
        byte myBytes[] = new byte [] {0,0,0,0};
        int maybe = 9;
        byte current;
        for (int counter = 0; counter < 4; counter++){
            maybe = 9;
            while (maybe == 9){
                current = androidRead();
                maybe = current;
                if (maybe != 9){
                    myBytes[counter] = current;
                    androidWrite(99);
                }
            }
        }
        byte toChange[] = new byte [] {myBytes[0], myBytes[1]};
        dataBack[0] = bytesToInt(toChange);
        toChange[0] = myBytes[2];
        toChange[1] = myBytes[3];
        dataBack[1] = bytesToInt(toChange);
        debug.setText(""+myBytes[0]+" "+myBytes[1]+" "+myBytes[2]+" "+myBytes[3]+" "+dataBack[0]+" "+dataBack[1]);
        androidWrite(99);//read receipt
    }

    private byte androidRead(){ //for now written to receive 1 integer
        if(mInputStream != null){
            byte buffer[] = new byte[1];

            try{
                mInputStream.read(buffer);
            } catch (IOException e){
                Log.e(TAG, "read failed", e);
            }
            return buffer[0];
        }
        else {
            return 8;
        }
    }

    private void androidWrite(int dataOut){ //we use 47 as indication to do 1 run
        byte out[] = new byte[1];
        out[0] = (byte) dataOut;

        if(mOutputStream != null){
            try {
                mOutputStream.write(out, 0, 1);
            }
            catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }

    //BEGIN OUR FUNCTIONS

    public void onTrainSmile(View v){
        ToggleButton tb1 = (ToggleButton) findViewById(R.id.Train1);
        tb1.setChecked(true);
        double temp[] = new double[] {0,0};
        temp = computeThresholds();
        thresholds[0] = 0.85*(temp[0]); //based on data recieved
        thresholds[1] = 1.5*(temp[1]);
        debug.setText(""+temp[0]+" "+temp[1]+" "+thresholds[0]+" "+thresholds[1]);
        tb1.setChecked(false);
    }

    public void onTrainFrown(View v){
        ToggleButton tb1 = (ToggleButton) findViewById(R.id.Train2);
        tb1.setChecked(true);
        double temp[] = new double[] {0,0};
        temp = computeThresholds();
        thresholds[2] = 1.5*(temp[0]); //based on data recieved
        thresholds[3] = 0.75*(temp[1]);
        debug.setText(""+temp[0]+" "+temp[1]+" "+thresholds[2]+" "+thresholds[3]);
        tb1.setChecked(false);
    }

    public double[] computeThresholds(){
        double result[] = new double[] {0,0};
        double a[] = new double[] {0.0, 0.0};
        double toShow1[] = new double[] {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
        double toShow2[] = new double[] {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
        for(int b = 0; b < 10; b++){ //dataBack[smilemuscle, frownmuscle]
            communicateWithArduino();
            toShow1[b] = dataBack[0];
            a[0] += (dataBack[0]);
            toShow2[b] = a[0];
            a[1] += (dataBack[1]);
        }
        a[0] /= 10;
        a[1] /= 10;
        result[0] = a[0];
        result[1] = a[1];
        return result;
    }

    public void onDone(View v){
        Intent passback = new Intent();
        String newName = nameEdit.getText().toString();
        passback.putExtra("smilea",(int)thresholds[0]);
        passback.putExtra("smileb",(int)thresholds[1]);
        passback.putExtra("frowna",(int)thresholds[2]);
        passback.putExtra("frownb",(int)thresholds[3]);
        passback.putExtra("personname", newName);
        setResult(Activity.RESULT_OK, passback);
        closeAccessory();
        finish();
    }

    public int bytesToInt(byte toChange[]){
        int myInt;
        if(toChange[0] == 0){
            myInt = toChange[1];
        }
        else {
            myInt = toChange[0] << 7;
            myInt += toChange[1];
        }

        return myInt;
    }


}
