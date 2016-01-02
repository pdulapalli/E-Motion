/**Arduino_E-Motion.ino--Real-time signal collection from electrodes using Atmel ATmega2560 microcontroller
 * Last Modified: 12/4/2015
 **/

#include <Wire.h>
#include "Max3421e.h"
#include "Usbhost.h"
#include "AndroidAccessory.h"
#include "math.h"

//Important Pins
#define GREEN_LED 8
#define FM_CHANNEL A1
#define SM_CHANNEL A2
#define CAPTURE_DURATION 1 //seconds
#define SAMPLE_RATE 10 //Hz
#define NUM_CAP_SAMPLES CAPTURE_DURATION*SAMPLE_RATE

//Define structure type dataPacket
typedef struct{
    int SM_Data;
    int FM_Data;
} dataPacket;

//Variables
boolean readyToSend;
byte dataToSend[4];
byte intInBytes[2];
byte *toSendPtr1;
byte *toSendPtr2;
dataPacket myData[SAMPLE_RATE*CAPTURE_DURATION];
long time1, time2, timeElapsed;
int brokenCounter;
int meanSM, meanFM, rmsSM, rmsFM, maxSM, maxFM;

AndroidAccessory acc("Manufacturer",
    "Model",
    "Description",
    "1.0",
    "http://yoursite.com",
                "0000000012345678");

void setup(){
    //Set serial baud rate
    Serial.begin(115200);

    readyToSend = false;
    time1=time2=timeElapsed=0;
    meanSM = meanFM = rmsFM = rmsSM = maxSM = maxFM = brokenCounter = 0;
    dataPacket hello = {1, 2};
    pinMode(GREEN_LED, OUTPUT); //WHITE (P1)

    Serial.print("\r\nStart");
    boolean out = acc.isConnected();
    //Serial.println(out, DEC);
    acc.powerOn();
}

void loop(){

    // Get "go" command
    byte msg[1];
    if (acc.isConnected()) {
        int len;
        len = acc.read(msg, sizeof(msg), 1); // read data into msg variable
        delay(10);
        if (len > 0) { //message array: only message ever received will be 47(record command) or 99(read receipt)
            if (msg[0] == 47){ // compare received data
                record();
            }
        }

     // Send data back
        if (readyToSend){
          boolean onThis;
          for (int q = 0; q < 4; q++){
            brokenCounter=0;
            onThis=true;
            while (onThis){
              brokenCounter=brokenCounter++;
              msg[0]=0;
              len = acc.read(msg, sizeof(msg), 1);
              if (len > 0 || brokenCounter==10) {
                Serial.println("I read something");
                  if (msg[0] == 99 || brokenCounter==10){
                    onThis=false;
                    delay(1250);
                    break;
                  }
              }
              acc.write(dataToSend[q]);
              delay(750);
              Serial.println("sent");
            }
          }
          readyToSend=false;
          Serial.println("done sending");
        }

        delay(10);

    }

    else{ //tablet not connected
        digitalWrite(GREEN_LED , LOW); // turn off light
        delay(10);
    }

}

void record(void){
    // Get data
    digitalWrite(GREEN_LED, HIGH);
    delay(500);//to give them time to react to light before beginning to record
    acquireMeasurements();
    computeRMS();
    computeMeans();
    computeMax();

    Serial.println("Max's:");
    Serial.println(maxSM);
    Serial.println(maxFM);

    // Make data sendable
    intToBytes(rmsSM);
    dataToSend[0] = intInBytes[0];
    dataToSend[1] = intInBytes[1];
    intToBytes(rmsFM);
    dataToSend[2] = intInBytes[0];
    dataToSend[3] = intInBytes[1];

    digitalWrite(GREEN_LED, LOW);
    readyToSend = true; //communicate back
    //delay(2000); //so that there is a break between training smiles
}

void acquireMeasurements(void){
    int samplePeriod, tempReadSM, tempReadFM;
    float samplePeriodFloat;

    samplePeriodFloat = 1000;
    samplePeriodFloat /= (float) SAMPLE_RATE;
    samplePeriod = roundFunction(samplePeriodFloat);

    int a;
    time1=millis();
    for(int i = 0 ; i < SAMPLE_RATE*CAPTURE_DURATION; i++){
      timeElapsed=millis()-time1;
      Serial.println("Elapsed time for sample (s):");
      Serial.println(timeElapsed);
      time1=millis();
        tempReadSM = analogRead(SM_CHANNEL);
        Serial.println(tempReadSM);
        tempReadFM = analogRead(FM_CHANNEL);
        Serial.println(tempReadFM);
        myData[i].SM_Data = tempReadSM;
        myData[i].FM_Data = tempReadFM;
        delay(samplePeriod);
    }
    timeElapsed=millis()-time1;
    Serial.println("Elapsed time for sample (s):");
    Serial.println(timeElapsed);

    /*time2=millis();
    timeElapsed=time2-time1;
    Serial.println("Elapsed time (s):");
    Serial.println(timeElapsed);*/
}

void computeMeans(void){
    float tempSM, tempFM;
    dataPacket *dataPtr;

    tempSM = tempFM = 0;

    //dataPtr = capturedData;

    for(int i = 0; i < SAMPLE_RATE*CAPTURE_DURATION; i++){
        tempSM += myData[i].SM_Data;
        tempFM += myData[i].FM_Data;

    }

    tempSM /= (float) SAMPLE_RATE*CAPTURE_DURATION;
    tempFM /= (float) SAMPLE_RATE*CAPTURE_DURATION;

    meanSM = roundFunction(tempSM);             //Store the means in their corresponding global variables
    meanFM = roundFunction(tempFM);
}

void computeRMS(void){
    float tempSM, tempFM;
    dataPacket *dataPtr;

    tempSM = tempFM = 0;

    for(int i = 0; i < SAMPLE_RATE*CAPTURE_DURATION; i++){
        //tempSM += pow(dataPtr->SM_Data, 2);
        tempSM += pow(myData[i].SM_Data, 2);
        //tempFM += pow(dataPtr->FM_Data, 2);
        tempFM += pow(myData[i].FM_Data, 2);

    }

    tempSM /= (float) SAMPLE_RATE*CAPTURE_DURATION;
    tempFM /= (float) SAMPLE_RATE*CAPTURE_DURATION;

    rmsSM = roundFunction(sqrt(tempSM));             //Store the RMS values in their corresponding global variables
    rmsFM = roundFunction(sqrt(tempFM));
}

void computeMax(void){
    int tempSM, tempFM;

    tempSM = tempFM = 0;

    for(int i = 0; i < SAMPLE_RATE*CAPTURE_DURATION; i++){
        if (myData[i].SM_Data > tempSM){
          tempSM = myData[i].SM_Data;
        }
        if (myData[i].FM_Data > tempFM){
          tempFM = myData[i].FM_Data;
        }
    }

    maxSM = tempSM;             //Store the max values in their corresponding global variables
    maxFM = tempFM;
}

int roundFunction(float originalVal){
    int output;
    float fraction;

    output = (int) (originalVal);
    fraction = originalVal - output;

    output = fraction >= 0.5 ? output + 1 : output;

    return output;
}

void intToBytes(int rawVal){
    unsigned int temp = (unsigned int) rawVal;
    temp = temp & 0x7FFF;

    intInBytes[0] = (temp >> 7);
    intInBytes[1] = temp & 0x7F;
}

int bytesToInt(byte myBytes[]){
    int myInt;

    myInt = myBytes[0] << 8;
    myInt += myBytes[1];

    return myInt;
}

