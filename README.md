# E-Motion
An Android application works in conjunction with an AVR microcontroller circuit to interpret facial muscle sensor (EMG) data

**Platform:** Android Nexus Tablet (actually compatible with any Android device beyond v.4.4)

**Summary of Operation:** 

* The very low voltage muscle sensor readings go through extensive amplification, rectification, and filtering. 
* This data is acquired by the Analog-Digital Converter on the AVR microcontroller and ultimately offloaded to the Android device. 
* A “Training” activity allows the user to calibrate the recognition system by smiling and frowning during known intervals. 
* The “Monitoring” activity deduces whether the user is smiling, frowning, or has neither expression.





