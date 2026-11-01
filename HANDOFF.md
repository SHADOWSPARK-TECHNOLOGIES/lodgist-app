# Lodgist Android handoff

Repository: /home/moronto/lodgist-app
Remote: https://github.com/SHADOWSPARK-TECHNOLOGIES/lodgist-app.git
Branch: main
Parent before this change: 1d8a4bd9f626f0fe4c96b8c776accde79f080af1

## What this build does

The Explore screen loads GET https://lodgist-api-production.up.railway.app/api/listings?limit=12.
That route is owned by the Lodgist web/API project. This app does not define it.

A 200 with data [] clears the local home table and shows "No published homes yet."
Seeded rows whose ids start with lodg_ are deleted on that result, and the screen also hides any that remain.
status VERIFIED on a listing does not mark it verified. Only verificationLabel "Verified by Lodgist" does.
The live API currently returns success true, data [], total 0. Checked 7 October 2026 while writing this note.

A failed load deletes seeded rows and says the list could not be loaded. It does not show sample homes as inventory.
A failed load shows "Could not load homes." It is not labelled as an empty market.
Reload calls the same read again.

A landlord form save is labelled "Saved on this phone" and "Not submitted." It does not start an audit.
The fake escrow checkout sheet is no longer opened.

An inspection request is saved on the phone with status SAVED_ON_PHONE and no inspector name.
The screen says it was not submitted. Cancel does not claim a refund.
Pay and release do not write payment rows. The pay button is disabled. Old local payment rows, if any, are labelled "Not a payment."

No payment route was added. The backend rental-check contract exists on feat/rental-check-manual-payment and is not merged or deployed. It publishes no bank details and no pay destination. POST /api/rental-checks/:reference/pay returns 503 and must not be treated as checkout.

## Build identity

Debug APK: app/build/outputs/apk/debug/app-debug.apk
Built 7 October 2026 04:54 +0100. Size 26,610,567 bytes.
sha256 db7e792747732aa2d6c6bdf61d00dbe887689a504d04f9786a5bea825ca752f4
package: com.aistudio.lodgist.xrkwqv
versionCode 1, versionName 1.0
compileSdk 36, minSdk 24, targetSdk 36
Signed with the existing debug keystore. Upload key was not created or replaced.
This is not a release AAB and was not uploaded.

## Checks

Passed: ./gradlew :app:testDebugUnitTest
15 tests, 0 failures. Includes PublicListingsTest (empty list, status-is-not-verification, unknown 404, network failure, seed id filter) and the existing Robolectric suite.
Passed: ./gradlew :app:assembleDebug
Live GET /api/listings returned 200, data [], total 0.

## Runtime verification, 7 October 2026

adb devices listed nothing. ~/Android/Sdk has no emulator binary and no system-images directory. No AVD exists under ~/.android/avd.
Free memory was about 4 GB, with 3.1 GB of swap already used. Disk free was 65 GB. A Play system image plus the emulator package is a large install, and 4 GB free is tight for running one. No image was installed.

Rechecked after that note. adb still listed no devices. The emulator binary and system-images directory are still absent. Available memory was about 5.9 GB. That is still tight for an emulator on a machine already using 9.5 GB, so no image was installed.
Runtime verification of the empty screen, the failed-load screen, the reload button, the local-save labels, and the disabled pay button remains blocked until a device is attached.

## Runtime verification, 31 October 2026 — the emulator does exist

The blocker above is out of date. An emulator is installed and boots; what was missing was the right path.

- `~/Android/Sdk/emulator/emulator` is present.
- `~/Android/Sdk/system-images/android-37.0/google_apis_playstore/x86_64` is present.
- The AVD is `Medium_Phone_API_37.0` and it lives under `~/.config/.android/avd/`, **not** `~/.android/avd`. A plain `emulator -avd Medium_Phone_API_37.0` fails with `Unknown AVD name`; the earlier checks only looked in `~/.android/avd` and concluded no AVD existed.
- It boots headless in 40–170 s:

      ANDROID_AVD_HOME=/home/moronto/.config/.android/avd \
        ~/Android/Sdk/emulator/emulator -avd Medium_Phone_API_37.0 \
        -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -memory 2048

- First cold start ANRs on this host at 2 GB with swiftshader (main thread blocked >5 s). The app then renders Explore behind the dialog, and a warm relaunch is responsive. Do not read that ANR as an app defect without checking a warm start.

Verified on the emulator so far:

- `GET /api/listings` from the device: real `200`, body 110 bytes, renders "No published homes yet".
- Airplane mode: `UnknownHostException`, renders "You are offline", not an empty market.
- A proxy answering `401` cannot exercise the unauthorized state: OkHttp tunnels `CONNECT` before it sees a status, so the stub answered `CONNECT ... 501`. That state stays test-only until a device build points at a plain-HTTP base URL.

Still device-unverified: the listing detail screen (production publishes nothing, so no row can be opened), the landlord local-save labels, the inspection labels, and the disabled pay control.

## Runtime verification, 1 November 2026 — a phone is attached, and the fold landed

`adb devices` now lists a physical device, `0826317000000025`, API 36. The
earlier note that no device is attached is out of date.

Emulator re-check, same boot command as above:

- installed over the existing app instead of wiping it, so the version-3
  database was upgraded in place: no Room errors, no crash, database rebuilt
  at version 4, and the screen rendered "No published homes yet. Sample homes
  are not shown." — the deleted seed rows are gone from an upgraded install.
- a live `200` from `/api/listings` arrived during that run.

Schema note: the database has no Room migrations and `fallbackToDestructiveMigration()`
is on, so the 3 -> 4 bump drops and rebuilds local tables once. Locally saved
favourites are lost on the upgrade. That is this project's existing policy,
not a new one.

The duplicate browse surface is gone: Explore is the only browse path, and
`PropertyDetailScreen` remains the only detail screen. `PublicListingsRepository.load()`
is the single reader; the client no longer calls `GET /api/listings/:id` even
though the route exists.

Test count dropped from 64 to 23 across the audit cuts, because four test
classes were deleted along with the code they described. Nothing was removed
to make a failing run pass.

## Installed on the phone, 1 November 2026

Install for audit and inspection:

    adb -s 0826317000000025 install -r app/build/outputs/apk/debug/app-debug.apk

    device      Infinix X6877, Android 16 (API 36), arm64-v8a
    package     com.aistudio.lodgist.xrkwqv, versionCode 1, versionName 1.0
    APK         22,099,525 bytes, installed 2026-11-01 18:50
    sha256      cd6c82da88c0227b065a6334e0c4fb624b7cf09103ca3a4d11aa50bedf313634

Confirmed on that device: launches to Explore, no crash, a live `200` from
`GET /api/listings` (110-byte body), and "No published homes yet. Sample homes
are not shown."

The local database on the phone, read back with the debug build's `run-as`:

    user_version = 4
    journal_mode = wal
    tables       = properties, inspection_bookings, landlord_listings,
                   escrow_payments, room_master_table
    row counts   = every table zero

So the `cached_properties` table is genuinely gone on a real install, and the
deleted seed rows are absent.

### Inspecting it yourself

The debug APK is debuggable, so `run-as` works and nothing here needs root.

    # app-only logs
    adb -s 0826317000000025 logcat -d | grep -iE 'lodgist|okhttp'

    # crash buffer
    adb -s 0826317000000025 logcat -d -b crash

    # what is on screen
    adb -s 0826317000000025 exec-out screencap -p > shot.png

    # memory
    adb -s 0826317000000025 shell dumpsys meminfo com.aistudio.lodgist.xrkwqv

    # database, all three files
    adb -s 0826317000000025 exec-out run-as com.aistudio.lodgist.xrkwqv cat databases/lodgist_database > db
    adb -s 0826317000000025 exec-out run-as com.aistudio.lodgist.xrkwqv cat databases/lodgist_database-wal > db-wal
    adb -s 0826317000000025 exec-out run-as com.aistudio.lodgist.xrkwqv cat databases/lodgist_database-shm > db-shm

**Pull the `-wal` file too.** The database uses write-ahead logging, so the
main file alone reports `user_version = 0` and no tables while the real schema
and rows sit in the WAL. Reading only the main file gives a false empty
picture. Keep the three files named exactly `db`, `db-wal`, `db-shm` so SQLite
opens them together.

Known cosmetic defect seen on the phone: the bottom bar's "Inspections" label
wraps to "Inspection / s" on this 1208 px width. Not fixed.


## Device test, when a phone is plugged in

Use the existing debug APK. Do not install a new signing key.

1. Enable USB debugging. Then run:
   /home/moronto/Android/Sdk/platform-tools/adb devices -l
   /home/moronto/Android/Sdk/platform-tools/adb install -r /home/moronto/lodgist-app/app/build/outputs/apk/debug/app-debug.apk
2. Open Lodgist on Wi-Fi. Explore should say "No published homes yet." It should not show the seeded Owerri, Lagos, Port Harcourt, or Abuja sample homes.
3. Turn on airplane mode. Tap Reload. The screen should say "Could not load homes." It should not say the market is empty, and it should not show sample homes.
4. Turn Wi-Fi back on. Tap Reload. It should return to "No published homes yet."
5. Open List a property, save a form, and confirm the label is "Saved on this phone" / "Not submitted." It must not say an audit started.
6. Save an inspection. The inspections screen must say saved on this phone, with no inspector name. Cancel must not say refunded.
7. Open a home detail if any exist. The pay control must stay disabled. The escrow screen must not offer a working release, and any old row must read "Not a payment."
8. Do not enter a card number. Do not call POST /api/rental-checks/:reference/pay. A 503 from that route is not checkout.

## Release prerequisites, inspected, not changed

Release signing looks for KEYSTORE_PATH, or my-upload-key.jks in the repo root, plus STORE_PASSWORD and KEY_PASSWORD, alias upload.
KEYSTORE_PATH, STORE_PASSWORD, and KEY_PASSWORD are unset. my-upload-key.jks is absent. debug.keystore is present and is only for the debug APK.
assembleRelease was started and did not produce app/build/outputs/apk/release. No upload key was created. Nothing was published.

A store candidate still needs: a release AAB, the final applicationId decision (it is still com.aistudio.lodgist.xrkwqv), an upload key that is not the debug keystore, and the account owner clearing Play Console address verification.

## Next action

Attach a phone and run the eight steps above. Do not wire rental-check payment until that API is deployed and a real payment destination is published.

## Play, still open

Play Console address verification is still an account-owner step. I did not open Play Console in this pass.
The application id is still the generated id com.aistudio.lodgist.xrkwqv. Changing it later creates a new Play app. It was not changed.
A store release needs a release AAB, the final application id decision, and a signing setup that is not the debug keystore. None of that was done.

## Rollback

Revert this commit. The previous tree still seeded local homes and treated inspection and escrow writes as confirmed. Do not restore that behavior if the goal is an honest empty list.

## Do not do next without the backend owner

Do not call a payment, escrow, or booking-confirmation endpoint. None was supplied in a revised contract.
