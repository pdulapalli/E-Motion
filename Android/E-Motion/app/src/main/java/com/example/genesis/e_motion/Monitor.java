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
import android.content.SharedPreferences;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;


public class Monitor extends AppCompatActivity {

    /** Given Android/Arduino Communication Variables **/

    // TAG is used to debug in Android logcat console
    private static final String TAG = "ArduinoAccessory";

    private static final String
            ACTION_USB_PERMISSION = "com.example.bme590.android_dualworldmemory.action.USB_PERMISSION";

    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    private ToggleButton buttonLED;

    private SharedPreferences pref;

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    final Context context = this; //Need self-referential context for dialog alerts


    /** Functional Variables **/
    private int currentPerson; //1,2, or 3 correlate to togglebuttons on right
    private int currentImage; //1=none, 2=smile, 3=frown
    private int testValue; //value to compare to saved data according to algorithm
    public String names[]; //person names
    private int dataBack[];
    private boolean testing;
    public final static String SENT_PERSON = "com.example.anna.emotion.PERSONTO";//ID of value of person to be changed
    public final static String SENT_NAME = "com.example.anna.emotion.NAMETO";

    private TextView debug;

    //Will have to initialize with real saved data or a backup plan
    private int personData[][];// = {
    //    {5,5,7,7},
    //    {2,2,4,4},
    //    {8,8,12,12}
    //};//Just for testing purposes, will be 3 rows and 4 columns


    private static final int[] sourceImage = new int[]
            {
                    R.drawable.smileyface,
                    R.drawable.frownyface,
                    R.drawable.fail,
                    R.drawable.blank,
                    R.drawable.wait
            };   //Holds the 5 different image(IDs) used for imageview

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
    // public Object onRetainNonConfigurationInstance() {
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

        //Get Saved Data
        pref = getSharedPreferences("info", MODE_PRIVATE);
        names = new String[3]; personData = new int[3][4];
        names[0] = pref.getString("1name", "Person 1");names[1]=pref.getString("2name", "Person 2");names[2]=pref.getString("3name", "Person 3");
        personData[0][0] = pref.getInt("1smile1", 1000);personData[0][1]=pref.getInt("1smile2", 1000);
        personData[0][2] = pref.getInt("1frown1", 1000);personData[0][3]=pref.getInt("1frown2", 1000);
        personData[1][0] = pref.getInt("2smile1", 1000);personData[1][1]=pref.getInt("2smile2", 1000);
        personData[1][2] = pref.getInt("2frown1", 1000);personData[1][3]=pref.getInt("2frown2", 1000);
        personData[2][0] = pref.getInt("3smile1", 1000);personData[2][1]=pref.getInt("3smile2", 1000);
        personData[2][2] = pref.getInt("3frown1", 1000);personData[2][3]=pref.getInt("3frown2", 1000);

        currentPerson = 0;
        currentImage=3;
        testing = false;
        testValue = 0;
        dataBack = new int[]{0,0};

        if (getLastNonConfigurationInstance() != null) { //WHY ARE THESE CROSSED OUT???????????????
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }

        setContentView(R.layout.activity_monitor);

        debug = (TextView) findViewById(R.id.Debugger);
        String personVals = new String(""+Integer.toString(personData[currentPerson][0])+" "+Integer.toString(personData[currentPerson][1])+" "+Integer.toString(personData[currentPerson][2])+" "+Integer.toString(personData[currentPerson][3]));
        debug.setText(personVals);

        //if there is no saved data, Persondata should be made to have all smile and frown data be equal so that in comparison it spits out an error
        setNames();
        setImage(currentImage);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        int temp[] = new int[] {0,0,0,0};
        if (requestCode == 1 ){ ////should we check resultCode
            temp[0]=data.getIntExtra("smilea",1000);
            temp[1]=data.getIntExtra("smileb",1000);
            temp[2]=data.getIntExtra("frowna",1000);
            temp[3]=data.getIntExtra("frownb",1000);
            names[currentPerson]=data.getStringExtra("personname");
            for (int i = 0; i < 4; i++)
            {
                if (temp[i]!=1000){
                    personData[currentPerson][i] = temp[i];
                }
            }
            setNames(); //in case name changed, we also want the name to update on its button


            /*IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
            registerReceiver(mUsbReceiver, filter);
            if (getLastNonConfigurationInstance() != null) { //WHY ARE THESE CROSSED OUT???????????????
                mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
                openAccessory(mAccessory);
            }*/
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        setNames(); //in case name changed, we also want the name to update on its button
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
            debug.setText("didnt reopen accessory");

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putInt("1smile1",personData[0][0]);
        prefEditor.putInt("1smile2",personData[0][1]);
        prefEditor.putInt("1frown1",personData[0][2]);
        prefEditor.putInt("1frown2", personData[0][3]);
        prefEditor.putString("1name", names[0]);
        prefEditor.putInt("2smile1", personData[1][0]);
        prefEditor.putInt("2smile2",personData[1][1]);
        prefEditor.putInt("2frown1",personData[1][2]);
        prefEditor.putInt("2frown2",personData[1][3]);
        prefEditor.putString("2name",names[1]);
        prefEditor.putInt("3smile1",personData[2][0]);
        prefEditor.putInt("3smile2",personData[2][1]);
        prefEditor.putInt("3frown1",personData[2][2]);
        prefEditor.putInt("3frown2",personData[2][3]);
        prefEditor.putString("3name",names[2]);
        prefEditor.commit();
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
            debug.setText("did not open");
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
    public int[] communicateWithArduino(){
        androidWrite(47);//47 will be the cue for Arduino to run once
        int data[] = new int[] {0,0};
        byte myBytes[] = new byte[] {0,0,0,0};
        int maybe;
        byte current;
        for(int counter = 0; counter < 4; counter++){
            maybe = 9;
            while(maybe == 9){
                current = androidRead();
                maybe = current;
                if (maybe != 9){
                    myBytes[counter] = current;
                    androidWrite(99);
                }
            }
        }
        byte toChange[] = new byte[] {myBytes[0], myBytes[1]};
        data[0] = bytesToInt(toChange);
        toChange[0] = myBytes[2];toChange[1]=myBytes[3];
        debug.setText(""+myBytes[0]+" "+myBytes[1]+" "+myBytes[2]+" "+myBytes[3]+" ");
        data[1] = bytesToInt(toChange);
        androidWrite(99);//read receipt
        return data;
        //First we will want to read a certain number of times, however many data points we get from each smile
        //Then we will want to process those data point to get just 1 value to compare to threshold.
        //We will have to do this for both electrodes

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

    //Manage Person and their Buttons
    public void onPerson1Click(View v){
        currentPerson = 0;
        personManager(currentPerson);
    }
    public void onPerson2Click(View v){
        currentPerson = 1;
        personManager(currentPerson);
    }
    public void onPerson3Click(View v){
        currentPerson = 2;
        personManager(currentPerson);
    }

    public void personManager(int cp){
        ToggleButton tb1 = (ToggleButton) findViewById(R.id.Person1);
        ToggleButton tb2 = (ToggleButton) findViewById(R.id.Person2);
        ToggleButton tb3 = (ToggleButton) findViewById(R.id.Person3);
        if (cp == 0){
            tb1.setChecked(true);
            tb2.setChecked(false);
            tb3.setChecked(false);
            tb1.setClickable(false);tb2.setClickable(true);tb3.setClickable(true);
        }
        if (cp == 1){
            tb1.setChecked(false);
            tb2.setChecked(true);
            tb3.setChecked(false);
            tb1.setClickable(true);tb2.setClickable(false);tb3.setClickable(true);
        }
        if (cp == 2){
            tb1.setChecked(false);
            tb2.setChecked(false);
            tb3.setChecked(true);
            tb1.setClickable(true);tb2.setClickable(true);tb3.setClickable(false);
        }
        String personVals = new String(""+Integer.toString(personData[currentPerson][0])+" "+Integer.toString(personData[currentPerson][1])+" "+Integer.toString(personData[currentPerson][2])+" "+Integer.toString(personData[currentPerson][3]));
        debug.setText(personVals);
    }

    public void setNames(){
        ToggleButton tb1 = (ToggleButton) findViewById(R.id.Person1);
        ToggleButton tb2 = (ToggleButton) findViewById(R.id.Person2);
        ToggleButton tb3 = (ToggleButton) findViewById(R.id.Person3);
        tb1.setTextOn(names[0]);
        tb1.setTextOff(names[0]);
        tb2.setTextOn(names[1]);
        tb2.setTextOff(names[1]);
        tb3.setTextOn(names[2]);
        tb3.setTextOff(names[2]);
        personManager(currentPerson);
    }

    //Go Train
    public void goToTrain(View v){
        //closeAccessory();
        Intent intent = new Intent(this, Calibrator.class);
        intent.putExtra(SENT_PERSON, currentPerson); //will also need name string of person!!!
        intent.putExtra(SENT_NAME, names[currentPerson]); //will also need name string of person!!!
        startActivityForResult(intent, 1); //DONT KNOW WHAT A REQUEST CODE IS.. 1 means what?
    }


    //Manage Test Capability and Display Image and Button
    public void onTestClick(View v){
        ToggleButton tb4 = (ToggleButton) findViewById(R.id.Test);
        testing = !testing;
        int result;
        if(testing){
            currentImage = 4;//Tell them to wait before we go to test function
            ImageView iv = (ImageView) findViewById(R.id.emotion);
            iv.setImageResource(sourceImage[currentImage]);
            result = test();
        }
        else{
            result = 3;
            String personVals = new String(""+Integer.toString(personData[currentPerson][0])+" "+Integer.toString(personData[currentPerson][1])+" "+Integer.toString(personData[currentPerson][2])+" "+Integer.toString(personData[currentPerson][3]));
            debug.setText(personVals);
        }
        setImage(result);
        currentImage=result;
    }

    public void setImage(int im){
        ImageView iv = (ImageView) findViewById(R.id.emotion);
        iv.setImageResource(sourceImage[im]);
    }

    public int test(){
        //Get data
        int currentFace[] = new int[] {31,31};
        currentFace = communicateWithArduino();
        String comp = new String(""+Integer.toString(currentFace[0])+" "+Integer.toString(currentFace[1]));
        debug.setText(comp);

        //Compare data
        boolean isSmile = false;
        boolean isFrown = false;
        if (currentFace[0] >= personData[currentPerson][0]){
            if (currentFace[1] <= personData[currentPerson][3]){
                isSmile = true;
            }
        }

        if (currentFace[1] >= personData[currentPerson][3]){
            //if (currentFace[0]<=2*personData[currentPerson][0]){
            isFrown = true;
            //}
        }

        //Make a decision
        //0 for smile, 1 for frown, 2 if it fails
        if (isSmile && isFrown){
            debug.setText("Both expressions possible");
            return 2; //because it could be either one
        }
        else if(isSmile){
            return 0;
        }
        else if(isFrown){
            return 1;
        }
        else{
            debug.setText("We do not think you are doing either");
            return 2; //because we don't think/can't tell if it is either one
        }
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


