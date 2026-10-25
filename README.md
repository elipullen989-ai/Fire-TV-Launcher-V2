# Clean Launcher for Fire TV

A tiny, ad-free home screen: your apps in a grid, a clock, nothing else.

- No `INTERNET` permission, no libraries, no tracking, no sponsored tiles.
- Shows TV banners for TV apps and icons for the rest, sorted A-Z, with a Settings tile.
- D-pad: move around, **Select** opens an app, **long-press Select** opens a menu: **Move** or **App info** (uninstall / clear data).

## 1. Prepare the Fire TV (one time)

1. **Settings > My Fire TV > About**: click the device name 7 times to unlock Developer Options.
2. **Settings > My Fire TV > Developer Options**: turn on **ADB debugging** and **Install unknown apps** (on newer Fire OS, allow it for the Downloader/ADB source).
3. Note the IP address: **Settings > My Fire TV > About > Network**.

## 2. Build the APK

You need JDK 17 and the Android SDK (easiest: open this folder in Android Studio and choose
**Build > Build APK**, or run `gradle assembleRelease` with Gradle 8.2+).

The APK ends up at `app/build/outputs/apk/release/app-release.apk`.

**No Android Studio?** Upload this folder to a new GitHub repository (keep the hidden
`.github` folder). Open the repo's **Actions** tab, choose **Build APK**, then **Run workflow**.
When it finishes (a few minutes), download `clean-launcher-apk` from the run page and unzip it.
Then install with `./install.sh <TV IP> --apk path/to/app-release.apk`, or load it on the TV
with the Downloader app.

## 3. Install over Wi-Fi

Needs `adb` (Android platform-tools) on your computer, on the same network as the Fire TV.

```bash
chmod +x install.sh
./install.sh 192.168.1.50                 # install and launch
./install.sh 192.168.1.50 --set-home      # also try to make it the Home app
./install.sh 192.168.1.50 --replace-home  # also disable Amazon's home screen (the ad source)
./install.sh 192.168.1.50 --restore       # undo --replace-home
```

Accept the "Allow USB debugging?" prompt on the TV the first time.

## Themes

Press the **Menu** button (three lines) on the Fire TV remote, or open the **Themes** tile
at the end of the grid. Your choice is remembered.

| Theme | Look |
|-------|------|
| **Fire TV** | The familiar dark navy with a blue highlight. This is the default. |
| **Football** | Mowed-stripe field with yard lines, "Game Day" title, gold highlight. |
| **Pride** | Rainbow tiles and a rainbow bar, dark purple background, white highlight. |
| **Heartstopper** | Soft pastel colors on deep violet, made for your own pictures (see below). |

Themes are defined in the `THEMES` array at the top of `MainActivity.java`, so adding or
tweaking one means editing a single block of colors.

## Rearranging apps

Long-press **Select** on an app and choose **Move**. The app grows slightly and says "(moving)".
Use the arrow buttons to slide it where you want it (the other apps shift to make room), then
press **Select** (or Back) to drop it. Your order is saved. Apps you install later appear after
your arranged ones, A-Z. The Themes and Settings tiles always stay at the end.

## Picture slideshow backgrounds

Every theme can show a slideshow of your own pictures, fading from one to the next every
12 seconds. The launcher has no internet access, so you add the pictures yourself.

By default the pictures play in a **banner at the top** of the home screen, where Fire TV puts its
featured ad. Open the **Pictures** tile to switch between **Banner at the top**,
**Full-screen background** (with a dark overlay so the app names stay readable) and **Off**.
Each theme has its own picture folder, and the theme colors don't change.

1. Open the launcher once. It creates one folder per theme on the TV:
   `/sdcard/Android/data/com.eli.cleanlauncher/files/backgrounds/`
   with subfolders `firetv`, `football`, `pride` and `heartstopper`.
2. Put pictures (JPG, PNG or WEBP, landscape, around 1920x1080) into a theme's subfolder.
   5 to 15 pictures per theme works well.
3. Push them from your computer (PowerShell, inside the `platform-tools` folder):

```
.\adb push "C:\Users\YOU\Pictures\football\." /sdcard/Android/data/com.eli.cleanlauncher/files/backgrounds/football/
```

   Or a single file: `.\adb push photo.jpg /sdcard/Android/data/com.eli.cleanlauncher/files/backgrounds/football/`

4. Go back to the launcher and pick the theme. A theme with no pictures shows no
   banner or background pictures. To remove pictures: `.\adb shell rm /sdcard/Android/data/com.eli.cleanlauncher/files/backgrounds/football/photo.jpg`

Updating the app: builds from GitHub are signed with a different key each time, so Android
may refuse to install over the old copy. If so, run `.\adb uninstall com.eli.cleanlauncher`
and install again. If you disabled Amazon's home screen, re-enable it first
(`.\adb shell pm enable com.amazon.tv.launcher`) so the TV is never left without a home screen.

## About the ads

The ads and sponsored rows live in Amazon's own home screen (`com.amazon.tv.launcher`), not in
other apps. This launcher shows none of that. The catch is that Fire OS keeps sending the Home
button to Amazon's launcher unless it is switched off, so:

- `--set-home` works on some Fire OS versions and is ignored on others.
- `--replace-home` is the reliable route. It uses `pm disable-user`, which is fully reversible
  (`--restore`, or `adb shell pm enable com.amazon.tv.launcher`). While it's disabled, some
  Amazon features that depend on the stock home screen (e.g. certain voice and recommendation
  surfaces) won't work.

Also worth turning off in **Settings > Preferences**: *Featured Content* (autoplay video/audio),
*Privacy Settings* (interest-based ads, app usage data).

Ads *inside* apps (Prime Video, Freevee, YouTube, etc.) are not touched by any launcher.
