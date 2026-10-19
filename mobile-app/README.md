# PhysLive Android

The Flutter app hosts the responsive PhysLive web application. Staff and Student use the same session, permissions, simulation runtime and learning workflows as the web. The old standalone projectile calculator and duplicate mobile project have been removed from version control.

## Run

Use Flutter 3.41.9 and Android SDK 36 or newer.

```sh
flutter pub get
flutter run --dart-define=PHYSLIVE_WEB_URL=https://your-physlive-deployment.example
```

Without the define, the app asks for the deployment URL and remembers it. Production URLs require HTTPS. The Android emulator can use http://10.0.2.2:5173 for a local server; cleartext traffic is enabled only in debug builds.

The app includes image capture/gallery selection, back navigation, network retry, external checkout navigation and export sharing. An exported HTML replay works offline in a browser; creating a simulation requires the configured server/providers.

## Check and build

```sh
flutter analyze
flutter test
flutter build apk --debug
flutter build apk --release --dart-define=PHYSLIVE_WEB_URL=https://your-physlive-deployment.example
```

Release signing reads android/key.properties with storeFile, storePassword, keyAlias and keyPassword. storeFile is relative to the android directory. The keystore and properties are ignored. A local upload key was generated for the verified release APK; back up both signing files privately to keep the same application identity for updates. No key or password is uploaded to GitHub. CI builds a debug APK without those files.

The release APK has been built and its signature verified. No Android device was connected for installation/camera/share-sheet testing. The AAB command reported a missing SDK symbol-verification tool, so only the successful APK build is a delivered release artifact.
