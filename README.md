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

## Requirements

| Requirement | Value                        |
|---|------------------------------|
| Minimum SDK | Android 8.0 (API 26)         |
| Target SDK | Android 16 (API 36)          |
| Language | Kotlin                       |
| UI toolkit | Jetpack Compose + Material 3 |

## Permissions

| Permission | Reason |
|---|---|
| `READ_PHONE_STATE` | Read SIM / subscription info |
| `READ_PHONE_NUMBERS` | Read the phone number for each SIM |

Both permissions are requested at runtime on first launch.

## Build

Open the project in Android Studio (Hedgehog or newer) and run the **app** configuration, or build from the command line:

```bash
./gradlew assembleDebug
```

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- `TelephonyManager` / `SubscriptionManager` APIs (no third-party network or data libraries)

## License

[MIT](LICENSE)
