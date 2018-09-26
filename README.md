# Tittle Framework

	Version: 1.0
	Minimum SDK version: 21

		
## Overview

Tittle framework allows you to set up, search and control Tittle lights.
	
## Installation 

Download the AAR file from https://github.com/clarityhk/tittle-sdk-samples/tree/master/distribution
Add AAR file as a dependency to your project (see https://developer.android.com/studio/projects/android-library for more detailed instructions)

Make sure settings.gradle has the library listed at the top:
```gradle
include ':app', ':tittlesdk-release'
```

Add dependency to the SDK to build.gradle:
```gradle
implementation project(":tittlesdk-release")
```

Add permissions for `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` to your ApplicationManifest.xml
```xml
<!-- INTERNET PERMISSION IS NEEDED FOR CONNECTING TO TITTLE VIA WIFI -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```


## Usage

	Description about this section
	
1. [Connect Tittle to Wifi](#connect)
2. [Set Light Mode](#light_mode)
3. [Search Tittles](#search_tittles)



<span id="connect"></span>
### Connect Tittle to Wifi
	
Tittle uses Wifi to receive commands from your Android device. Tittle app has two ways to connect to wifi "Smart config" and "Standard config". The SDK only supports Standard config for the time being.

Tittle Light support Smart Config or standard config for wifi configuration. This Framework now only support Standard Config.

- Step 1. Switch the Tittle Light to AP mode.
		
	You need to do the following steps for your Tittle
		
	- Press and hold the power button for 5 seconds until your tittle blinks in white color then release.
	
	- Press and hold again the power button for 3 seconds until your tittle blinks in yellow.
	
	- Go to the wifi setting in your phone and connect a network called "Tittle-AP".

- Step 2. Once your phone has connected to "Tittle-AP", setup connection with Tittle
 
```java
String ssid = "My wifi name"; // The name of the network to which you want to connect your Tittle to
String password = "wifi password"; // Password of the network to connect
int timeout = 45000; // Timeout for the config in ms. Config can take 30+ seconds.
// listener needs to implement the StandardConfig.StandardConfigListener interface, and will be called
// when the config has completed or failed / time'd out
config = new StandardConfig(ssid, password, timeout, listener);
config.connect();		
```

- Step 3. If the config succeeds, listener callback will be called with the IP address of the Tittle in the network it connected to. To start sending commands, switch your phone to the network that Tittle connected to.

<span id="light_mode"></span>
### Send commands to Tittle

To send commands to Tittle use TittleLightControl class. Create an instance of the class with
```java
String ip = "192.168.1.2" // IP of the tittle device in your network
TittleLightControl tittle = new TittleLightControl(ip, listener);
```
`listener` needs to implement the `TittleLightControl.TittleLightControlListener` interface.

Use `setLightMode` to switch on the light. Set intensity to 0 to switch light off.

```java
tittle.setLightMode(255, 255, 255, 255); // RGB color and ligth intensity as integers in 0 - 255 range
tittle.setLightMode(255, 255, 255, 0); // Turn off the light
```
	
<span id="search_tittles"></span>
### Search Tittles

If there are Tittle's already connected to wifi, you can search them with `TittleScanner` class.

```java
// First get the IP address of your device
InetAddress handsetIp = InetAddress.getByName(Util.getIPAddress());
TittleScanner scanner = new TittleScanner(handsetIp);
int timeout = 20000; // How long should we scan in ms. Broadcasts to Tittle are sent every 5 seconds, so use multiple's of 5000
// listener should implement TittleScanner.TittleScannerListener interface. Listener will be called each time Tittle's are found and once the scan has finished.
scanner.scan(timeout, listener);
```

## Sample project

See sample project at https://github.com/clarityhk/tittle-sdk-samples/tree/master/android_sample

