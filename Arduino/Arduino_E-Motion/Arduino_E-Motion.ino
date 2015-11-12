/**Arduino_E-Motion.ino
 * Last Modified: 11/12/2015
 **/

#include <Wire.h>
#include "Max3421e.h"
#include "Usbhost.h"
#include "AndroidAccessory.h"
#include "math.h"

//Musical Note Definitions
#define Note_A 0
#define Note_A_sharp 1
#define Note_B 2
#define Note_C 3
#define Note_C_sharp 4
#define Note_D 5
#define Note_D_sharp 6
#define Note_E 7
#define Note_E 8
#define Note_F 9
#define Note_G 10
#define Note_G_sharp 11

AndroidAccessory acc("Manufacturer",
    "Model",
    "Description",
    "1.0",
    "http://yoursite.com",
                "0000000012345678");

uint8_t playerID;

void setup(){
    //Set serial baud rate
    Serial.begin(115200);

    Serial.print("\r\nStart");
    boolean out = acc.isConnected();
    acc.powerOn();
}

void loop(){

}

void note(int buzzerPin, int numHalfSteps, int key, int duration){
    if(!key){
        key = 220;
    }

    const float notebase = 2;
    float halfSteps = (float) numHalfSteps;
    double power = pow(2, halfSteps);

    int out_freq = round(key*power);

    tone(buzzerPin, out_freq, duration);
}
