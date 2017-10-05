# RED-droid
Base station control on an Android tablet/phone

## Development
This is an Android Studio project, so installing Android Studio is the first step. Once the project is built, editing the UI is exactly the same as any other Android Studio project.

This repository includes a java implementation of rovecomm. Currently, this code is apart of the the Android project, but it would be preferable to have a seperate module/library.

Source code within the repository is located in "app/src/main". Java files are located within "java/edu/mst/marsrover/reddroid", while the XML resources are within "res". Rovecomm specifically located within "java/edu/mst/marsrover/reddroid/rovecomm".

## Usage
Inorder for the device to run the rover, it must be connected to the local network on the rover. The app has been tested by bridging the Autonomous Pi connection between ethernet and wifi. 
