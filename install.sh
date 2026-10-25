#!/usr/bin/env bash
# Install Clean Launcher on a Fire TV over ADB (Wi-Fi).
#
# Usage:
#   ./install.sh <fire-tv-ip> [--apk path/to/app.apk] [--set-home] [--replace-home]
#   ./install.sh <fire-tv-ip> --restore
#
#   --set-home       Ask Android to use Clean Launcher as the Home app.
#   --replace-home   Also disable Amazon's stock home screen (where the ads live).
#                    Reversible with --restore. Implies --set-home.
#   --restore        Re-enable Amazon's stock home screen.
set -euo pipefail

PKG="com.eli.cleanlauncher"
ACTIVITY="$PKG/.MainActivity"
AMAZON_HOME="com.amazon.tv.launcher"
DEFAULT_APK="$(dirname "$0")/app/build/outputs/apk/release/app-release.apk"

if [[ $# -lt 1 ]]; then
  sed -n '2,13p' "$0"
  exit 1
fi

IP="$1"; shift
APK="$DEFAULT_APK"
SET_HOME=0
REPLACE_HOME=0
RESTORE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)          APK="$2"; shift 2 ;;
    --set-home)     SET_HOME=1; shift ;;
    --replace-home) REPLACE_HOME=1; SET_HOME=1; shift ;;
    --restore)      RESTORE=1; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

command -v adb >/dev/null || { echo "adb not found. Install Android platform-tools first."; exit 1; }

echo "Connecting to $IP:5555 ..."
adb connect "$IP:5555" >/dev/null
sleep 2

STATE="$(adb -s "$IP:5555" get-state 2>&1 || true)"
if [[ "$STATE" != "device" ]]; then
  echo "Could not talk to the Fire TV (state: $STATE)."
  echo "Check that ADB debugging is on, and accept the 'Allow USB debugging?' prompt on the TV."
  exit 1
fi
ADB=(adb -s "$IP:5555")

if [[ $RESTORE -eq 1 ]]; then
  "${ADB[@]}" shell pm enable "$AMAZON_HOME" || "${ADB[@]}" shell pm enable --user 0 "$AMAZON_HOME"
  echo "Amazon's home screen is enabled again. Press Home on the remote."
  exit 0
fi

[[ -f "$APK" ]] || { echo "APK not found at $APK. Build it first (see README)."; exit 1; }

echo "Installing $APK ..."
"${ADB[@]}" install -r "$APK"

if [[ $SET_HOME -eq 1 ]]; then
  echo "Setting Clean Launcher as Home ..."
  "${ADB[@]}" shell cmd package set-home-activity "$ACTIVITY" || \
    echo "  (This Fire OS version didn't accept set-home-activity; see README.)"
fi

if [[ $REPLACE_HOME -eq 1 ]]; then
  echo "Disabling Amazon's stock home screen (reversible with --restore) ..."
  if ! "${ADB[@]}" shell pm disable-user --user 0 "$AMAZON_HOME"; then
    echo "  Fire OS refused to disable Amazon's home screen on this device."
    echo "  Clean Launcher is still installed; open it from Your Apps & Channels."
  fi
fi

echo "Launching ..."
"${ADB[@]}" shell am start -n "$ACTIVITY" >/dev/null
echo "Done."
