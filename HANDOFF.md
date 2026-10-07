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

Runtime verification of the empty screen, the failed-load screen, the reload button, the local-save labels, and the disabled pay button remains blocked until a device is attached.

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
