package com.example.anna.emotion;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ToggleButton;
import android.os.Handler;
import android.widget.*;
import android.os.Parcelable;
import android.os.HandlerThread;
import java.lang.Runnable;
import android.graphics.Color;

import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.ToggleButton;

import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;
import android.support.v7.app.AppCompatActivity;

import org.w3c.dom.Text;

public class MainActivity extends ActionBarActivity {

    private int currentPerson; //1,2, or 3 correlate to togglebuttons on right
    private int currentImage; //1=none, 2=smile, 3=frown
    private String[] names; //person names
    private boolean testing;
    public final static String SENT_PERSON = "com.example.anna.emotion.MESSAGETO";//ID of value of person to be changed

    private static final int[] sourceImage = new int[]
            {
                    R.drawable.SmileyFace,
                    R.drawable.FrownyFace,
                    R.drawable.Fail,
                    R.drawable.Blank,
                    R.drawable.Wait
            };   //Holds the 5 different image(IDs) used for imageview

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentPerson=1;currentImage=4;testing=false;
        names[1]="Person 1";names[2]="Person 2";names[3]="Person 3"; //Will need to access saved data
        setNames();
        personManager(currentPerson);
        setImage(currentImage);
    }

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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Manage Person and their Buttons
    public void onPerson1Click(View v){
        currentPerson=1;
        personManager(currentPerson);
    }
    public void onPerson2Click(View v){
        currentPerson=2;
        personManager(currentPerson);
    }
    public void onPerson3Click(View v){
        currentPerson=3;
        personManager(currentPerson);
    }

    public void personManager(int cp){
        ToggleButton tb1 = (ToggleButton) findViewById(R.id.Person1);
        ToggleButton tb2 = (ToggleButton) findViewById(R.id.Person2);
        ToggleButton tb3 = (ToggleButton) findViewById(R.id.Person3);
        if (cp==1){
            tb1.setChecked(true);
            tb2.setChecked(false);
            tb3.setChecked(false);
        }
        if (cp==2){
            tb1.setChecked(false);
            tb2.setChecked(true);
            tb3.setChecked(false);
        }
        if (cp==3){
            tb1.setChecked(false);
            tb2.setChecked(false);
            tb3.setChecked(true);
        }
    }

    public void setNames(){
        ToggleButton tb1 = (ToggleButton) findViewById(R.id.Person1);
        ToggleButton tb2 = (ToggleButton) findViewById(R.id.Person2);
        ToggleButton tb3 = (ToggleButton) findViewById(R.id.Person3);
        tb1.setTextOn(names[1]);tb1.setTextOff(names[1]);
        tb2.setTextOn(names[2]);tb2.setTextOff(names[2]);
        tb3.setTextOn(names[3]);tb3.setTextOff(names[3]);
    }

    //Go Train
    public void goToTrain(View v){
        Intent intent = new Intent(this, TrainActivity.class);
        intent.putExtra(SENT_PERSON, currentPerson);
        startActivityForResult(intent, 1); //DONT KNOW WHAT A REQUEST CODE IS.. 1 means what?
    }

    //Manage Test Capability and Display Image and Button
    public void onTestClick(View v){
        ToggleButton tb4 = (ToggleButton) findViewById(R.id.Test);
        testing=!testing;
        int result;
        if(testing){
            currentImage=5;//Tell them to wait before we go to test function
            ImageView iv = (ImageView) findViewById(R.id.emotion);
            iv.setImageResource(sourceImage[currentImage]);
            result = test();
        }
        else{
            result=4;
        }
        setImage(result);
        currentImage=result;
    }

    public void setImage(int im){
        ImageView iv = (ImageView) findViewById(R.id.emotion);
        iv.setImageResource(sourceImage[im]);
    }

    public int test(){ //TO DO: Make test procedure and store data
        return 1; //1 for smile, 2 for frown, 3 if it fails
    }

}
