# SimInfo

An Android app that displays detailed information about the SIM card(s) installed in your device.

## Features

- Lists all SIM cards / subscriptions detected on the device
- Slide animation when navigating to the detail screen
- Detail screen shows:
  - Carrier name & display name
  - Phone number
  - ICCID, IMEI, MCC/MNC
  - Network type (2G / 3G / 4G / 5G)
  - Roaming status
  - SIM slot index
  - And more, depending on device/carrier

## Build

Open the project in Android Studio (Hedgehog or newer) and run the **app** configuration, or build from the command line:

```bash
./gradlew assembleDebug
```

## License

[MIT](LICENSE)
