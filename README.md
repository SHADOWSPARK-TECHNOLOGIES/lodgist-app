# Lodgist — Android App 📱

Native Android client for [Lodgist](https://lodgist.online) — property discovery on the go.

## Features
- Browse properties (rent / sale / short-let)
- Save favorites + saved searches
- Agent chat & inquiries
- Push notifications for new matches
- Offline-first listing cache

## Stack
- **Kotlin** (Android)
- MVVM + Coroutines + Flow
- Retrofit / Room / Coil

## Build
```bash
./gradlew assembleDebug
```
Install the APK from `app/build/outputs/apk/debug/`.

## API
Connects to the Lodgist backend — set `BASE_URL` in `local.properties`.

> 📝 **Draft README** — maintainers: update architecture diagram, screenshots, and release notes.
