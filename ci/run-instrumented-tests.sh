#!/usr/bin/env bash
set -euo pipefail

API_LEVEL="${1:-29}"

adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'

# Disable animations for faster/more reliable Espresso tests
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# Grant external storage permissions; only meaningful on API <= 32
if [ "$API_LEVEL" -le 32 ]; then
  adb shell pm grant com.javaide.mobile android.permission.WRITE_EXTERNAL_STORAGE || true
  adb shell pm grant com.javaide.mobile android.permission.READ_EXTERNAL_STORAGE || true
fi

./gradlew connectedDebugAndroidTest
