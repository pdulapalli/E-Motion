#include <stdio.h>
#include <stdlib.h>

#define CAPTURE_DURATION 1 //seconds
#define SAMPLE_RATE 10 //Hz

typedef struct{
    short SM_Data;
    short FM_Data;
}dataPacket;

dataPacket myData[SAMPLE_RATE*CAPTURE_DURATION];
short meanSM, meanFM, rmsSM, rmsFM, maxSM, maxFM;

//Note: short is 16 bits integer

void computeMax(void);
short roundFunction(float originalVal);

int main(void){
    for(int i = 0; i < SAMPLE_RATE*CAPTURE_DURATION; i++){
        myData[i].SM_Data = (short) i;
        myData[i].FM_Data = (short) i+1;
    }

    computeMax();

    printf("%hi %hi\n", maxSM, maxFM);

    return 0;
}

void computeMax(void){
    short tempSM, tempFM;

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

short roundFunction(float originalVal){
    short output;
    float fraction;

    output = (int) (originalVal);
    fraction = originalVal - output;

    output = fraction >= 0.5 ? output + 1 : output;

    return output;
}