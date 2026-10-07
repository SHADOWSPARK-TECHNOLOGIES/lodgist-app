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
Reload calls the same read again.

An inspection request is saved on the phone with status SAVED_ON_PHONE and no inspector name.
The screen says it was not submitted. Cancel does not claim a refund.
Pay and release do not write payment rows. The pay button is disabled. Old local payment rows, if any, are labelled "Not a payment."

No payment route was added. Wait for a revised contract from the backend owner.

## Build identity

Debug APK: app/build/outputs/apk/debug/app-debug.apk
Built 7 October 2026 04:54 +0100. Size 26,610,567 bytes.
sha256 653ab434344a52be5ddd2ddaa957bee535c66764e19ddf3fbc4fd7d6985f17d7
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

Unrun: emulator and device. adb reported no devices. No emulator binary under ~/Android/Sdk/emulator. The empty screen, reload button, and disabled pay button were not watched on a phone.
Unrun: release build, Play upload, signed AAB.

## Play, still open

Play Console address verification is still an account-owner step. I did not open Play Console in this pass.
The application id is still the generated id com.aistudio.lodgist.xrkwqv. Changing it later creates a new Play app. It was not changed.
A store release needs a release AAB, the final application id decision, and a signing setup that is not the debug keystore. None of that was done.

## Rollback

Revert this commit. The previous tree still seeded local homes and treated inspection and escrow writes as confirmed. Do not restore that behavior if the goal is an honest empty list.

## Do not do next without the backend owner

Do not call a payment, escrow, or booking-confirmation endpoint. None was supplied in a revised contract.
