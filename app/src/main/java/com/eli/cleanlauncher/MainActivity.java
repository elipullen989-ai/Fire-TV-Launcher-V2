package com.eli.cleanlauncher;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A minimal, ad-free home screen: a grid of your installed apps and a clock.
 * No network access, no recommendations, no sponsored tiles.
 *
 * D-pad: move around, SELECT opens an app, long-press SELECT opens a menu
 * (Move, to rearrange apps with the arrow buttons, or App info). Press MENU (the
 * three-lines button) or open the "Themes" tile to switch themes.
 *
 * Each theme can show a slideshow of your own pictures, as a banner at the top, as
 * the full-screen background, or both at once with different pictures (Pictures tile).
 * The banner sits above the apps like Fire TV's featured ad and slides away when you
 * move down to the lower rows. Put images
 * in: /sdcard/Android/data/com.eli.cleanlauncher/files/backgrounds/THEME_FOLDER/
 * (folders are created the first time the launcher opens). With no pictures, the
 * theme's plain background is used.
 */
public class MainActivity extends Activity {

    private static final int COLUMNS = 5;
    private static final String PREFS = "launcher";
    private static final String PREF_THEME = "themeName";
    private static final String PREF_ORDER = "order";
    private static final String PREF_IMAGE_MODE = "imageMode";
    private static final String PREF_PAUSE_UNTIL = "pauseUntil";
    private static final String PREF_SAVER_MIN = "screensaverMin";
    private static final long SAVER_SLIDE_MS = 15000;   // how long each screensaver picture stays
    private static final long AMAZON_PAUSE_MS = 5 * 60 * 1000;   // time you get on Amazon's home
    private static final int MODE_BANNER = 0;       // pictures in a banner at the top
    private static final int MODE_BACKGROUND = 1;   // pictures behind everything
    private static final int MODE_OFF = 2;          // no pictures
    private static final int MODE_COMBINED = 3;     // banner at the top AND full-screen background
    private static final long SLIDE_MS = 12000;   // how long each picture stays up
    private static final long FADE_MS = 1200;     // crossfade between pictures

    // ---------------------------------------------------------------- Themes

    private static int col(String hex) {
        return Color.parseColor(hex);
    }

    // Declared before THEMES (static init order).
    private static final int[] RAINBOW = {
            col("#E40303"), col("#FF8C00"), col("#FFED00"),
            col("#008026"), col("#004DFF"), col("#750787")
    };

    private static final int[] PASTEL = {
            col("#F8BBD0"), col("#B3E5FC"), col("#FFF59D"),
            col("#C5E1A5"), col("#D1C4E9")
    };

    private static class AppTheme {
        final String name;      // shown in the picker
        final String title;     // shown top-left of the home screen
        final String folder;    // picture folder name for the slideshow
        final int bgTop, bgBottom, text, subText, focus, focusDp, radiusDp;
        final int scrim;        // dark overlay over slideshow pictures (keeps text readable)
        final int[] cards;      // tile background colors (cycled by position)
        final int[] swatch;     // preview strip in the picker
        final int[] bar;        // optional accent bar under the header (null = none)
        final boolean field;    // draw a striped football field instead of a gradient
        final boolean caps;     // UPPERCASE labels

        AppTheme(String name, String title, String folder, int bgTop, int bgBottom, int text,
                 int subText, int focus, int focusDp, int radiusDp, int scrim, int[] cards,
                 int[] swatch, int[] bar, boolean field, boolean caps) {
            this.name = name;
            this.title = title;
            this.folder = folder;
            this.bgTop = bgTop;
            this.bgBottom = bgBottom;
            this.text = text;
            this.subText = subText;
            this.focus = focus;
            this.focusDp = focusDp;
            this.radiusDp = radiusDp;
            this.scrim = scrim;
            this.cards = cards;
            this.swatch = swatch;
            this.bar = bar;
            this.field = field;
            this.caps = caps;
        }
    }

    private static final AppTheme[] THEMES = {
            // 0: the familiar Fire TV look (dark navy, Prime blue highlight)
            new AppTheme("Fire TV", "Home", "firetv",
                    col("#232F3E"), col("#0F171E"), Color.WHITE, col("#B0BEC5"),
                    col("#00A8E1"), 4, 12, 0x66000000,
                    new int[]{col("#1B2530")},
                    new int[]{col("#232F3E"), col("#0F171E"), col("#00A8E1")},
                    null, false, false),

            // 1: football field with mowed stripes and yard lines, gold highlight
            new AppTheme("Football", "Game Day", "football",
                    col("#1B5E20"), col("#2E7D32"), Color.WHITE, col("#E8F5E9"),
                    col("#FFD54F"), 5, 8, 0x55000000,
                    new int[]{col("#0D3B13")},
                    new int[]{col("#1B5E20"), col("#FFFFFF"), col("#2E7D32")},
                    new int[]{Color.WHITE, Color.WHITE}, true, false),

            // 2: pride, rainbow tiles and a rainbow bar
            new AppTheme("Pride", "Pride", "pride",
                    col("#1A1030"), col("#0B0714"), Color.WHITE, col("#D1C4E9"),
                    Color.WHITE, 4, 12, 0x77000000,
                    RAINBOW,
                    RAINBOW,
                    RAINBOW, false, false),

            // 3: soft pastel colors on deep violet, for your own Heartstopper pictures
            new AppTheme("Heartstopper", "Heartstopper", "heartstopper",
                    col("#2A2342"), col("#1B1830"), Color.WHITE, col("#E1D8F5"),
                    col("#F8BBD0"), 5, 16, 0x66000000,
                    PASTEL,
                    PASTEL,
                    PASTEL, false, false),
    };

    /** Mowed-lawn football field: alternating green bands with faint white yard lines. */
    private static class FieldDrawable extends Drawable {
        private final Paint paint = new Paint();
        private final int a, b;

        FieldDrawable(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect r = getBounds();
            int bands = 12;
            float w = r.width() / (float) bands;
            for (int i = 0; i < bands; i++) {
                paint.setColor(i % 2 == 0 ? a : b);
                canvas.drawRect(r.left + i * w, r.top, r.left + (i + 1) * w, r.bottom, paint);
            }
            paint.setColor(0x44FFFFFF);
            for (int i = 1; i < bands; i += 2) {
                canvas.drawRect(r.left + i * w - 2, r.top, r.left + i * w + 2, r.bottom, paint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    // ---------------------------------------------------------------- State

    private static class AppEntry {
        String label;
        String packageName;
        Intent intent;
        Drawable image;
        boolean isBanner;
        boolean themePicker;
        boolean picturePicker;
        boolean screensaverPicker;
        boolean amazonHome;
        boolean fixed;      // Themes and Settings tiles stay at the end and can't be moved
    }

    private final List<AppEntry> apps = new ArrayList<>();
    private AppAdapter adapter;
    private SharedPreferences prefs;
    private AppTheme theme = THEMES[0];
    private GridView grid;
    private int movingPos = -1;   // index of the app being moved, or -1
    private int imageMode = MODE_BANNER;

    // Screensaver state
    private int screensaverMin = 0;          // 0 = off
    private FrameLayout stageView;           // the whole screen; the screensaver is added on top
    private FrameLayout saverView;           // non-null while the screensaver is showing
    private ImageView saverFront;
    private ImageView saverBack;
    private List<File> saverFiles = new ArrayList<>();
    private int saverIndex = 0;
    private int saverGeneration = 0;         // bumped to cancel stale picture loads
    private final Runnable idleStart = () -> showScreensaver(false);
    private final Runnable saverNext = () -> showNextSaverSlide();
    private FrameLayout bannerFrame;   // the picture banner (banner mode only)

    // Slideshow state
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable nextSlide = () -> showNextSlide();
    private boolean bannerOnTopRow = true;  // false once you move down past the first row
    private ImageView bannerFront;  // banner picture currently showing (banner modes)
    private ImageView bannerBack;   // banner picture fading in
    private ImageView bgFront;      // the picture currently showing
    private ImageView bgBack;       // fades in on top, then is copied to bgFront
    private View scrimView;         // dark overlay, visible only while pictures are showing
    private List<File> slides = new ArrayList<>();
    private int slideIndex = 0;
    private int slideGeneration = 0; // bumped to cancel stale slide loads

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApps();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedTheme = prefs.getString(PREF_THEME, "");
        for (AppTheme t : THEMES) {
            if (t.folder.equals(savedTheme)) {
                theme = t;
            }
        }
        imageMode = prefs.getInt(PREF_IMAGE_MODE, MODE_BANNER);
        screensaverMin = prefs.getInt(PREF_SAVER_MIN, 0);
        createPictureFolders();
        buildUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        startSlideshow();
        applyKeepScreenOn();
        resetIdleTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        movingPos = -1;
        stopSlideshow();
        handler.removeCallbacks(idleStart);
        dismissSaverQuietly();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(packageReceiver);
        } catch (IllegalArgumentException ignored) {
            // not registered
        }
    }

    /** A launcher must not "back out" to nothing. */
    @Override
    public void onBackPressed() {
        // intentionally empty
    }

    /** MENU button on the Fire TV remote opens the theme picker. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showThemePicker();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---------------------------------------------------------------- UI

    // ---------------------------------------------------------------- Moving apps

    /** While an app is being moved, the D-pad moves the app instead of the highlight. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        resetIdleTimer();
        if (saverView != null) {
            // Any button just closes the screensaver; it does nothing else.
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                hideScreensaver();
            }
            return true;
        }
        if (movingPos < 0) {
            return super.dispatchKeyEvent(event);
        }
        boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
        int target;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                target = movingPos - 1;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                target = movingPos + 1;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                target = movingPos - COLUMNS;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                target = movingPos + COLUMNS;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                if (isDown && event.getRepeatCount() == 0) {
                    stopMove();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
        if (isDown && target >= 0 && target < movableCount() && target != movingPos) {
            apps.add(target, apps.remove(movingPos));
            movingPos = target;
            saveOrder();
            adapter.notifyDataSetChanged();
            if (grid != null) {
                grid.setSelection(target);
            }
        }
        return true;
    }

    private int movableCount() {
        int n = 0;
        for (AppEntry e : apps) {
            if (!e.fixed) {
                n++;
            }
        }
        return n;
    }

    private void showItemMenu(AppEntry entry, int position) {
        if (entry.fixed) {
            return;
        }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(entry.label)
                .setItems(new CharSequence[]{"Move", "App info"}, (dialog, which) -> {
                    if (which == 0) {
                        startMove(position);
                    } else {
                        openAppInfo(entry);
                    }
                })
                .show();
    }

    private void startMove(int position) {
        movingPos = position;
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Use the arrow buttons to move it. Press Select when done.",
                Toast.LENGTH_LONG).show();
    }

    private void stopMove() {
        movingPos = -1;
        adapter.notifyDataSetChanged();
    }

    private void saveOrder() {
        StringBuilder sb = new StringBuilder();
        for (AppEntry e : apps) {
            if (e.fixed) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(e.packageName);
        }
        prefs.edit().putString(PREF_ORDER, sb.toString()).apply();
    }

    // ---------------------------------------------------------------- UI

    private void buildUi() {
        // Bottom to top: plain background, picture layers, dark scrim, then the home screen.
        dismissSaverQuietly();
        FrameLayout stage = new FrameLayout(this);
        stageView = stage;
        if (theme.field) {
            stage.setBackground(new FieldDrawable(theme.bgTop, theme.bgBottom));
        } else {
            stage.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{theme.bgTop, theme.bgBottom}));
        }

        // Picture layers come in pairs; the top one fades in over the other.
        final int match = ViewGroup.LayoutParams.MATCH_PARENT;
        bgFront = null;
        bgBack = null;
        bannerFront = null;
        bannerBack = null;
        scrimView = null;
        bannerFrame = null;

        // Background (and Combined) mode: pictures fill the screen with a dark scrim over them.
        if (imageMode == MODE_BACKGROUND || imageMode == MODE_COMBINED) {
            bgFront = newPictureView();
            bgBack = newPictureView();
            bgBack.setAlpha(0f);
            FrameLayout layers = new FrameLayout(this);
            layers.addView(bgFront, new FrameLayout.LayoutParams(match, match));
            layers.addView(bgBack, new FrameLayout.LayoutParams(match, match));
            stage.addView(layers, new FrameLayout.LayoutParams(match, match));
            scrimView = new View(this);
            scrimView.setBackgroundColor(theme.scrim);
            scrimView.setVisibility(View.GONE);
            stage.addView(scrimView, new FrameLayout.LayoutParams(match, match));
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutTransition(new LayoutTransition());   // banner slides in and out smoothly
        bannerOnTopRow = true;

        // Header: title on the left, clock on the right
        FrameLayout header = new FrameLayout(this);
        header.setPadding(dp(48), dp(28), dp(48), dp(12));

        TextView title = new TextView(this);
        title.setText(theme.title);
        title.setTextColor(theme.text);
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL));

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(theme.subText);
        clock.setTextSize(24);
        header.addView(clock, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL));

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Optional accent bar under the header
        if (theme.bar != null) {
            View bar = new View(this);
            bar.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT, theme.bar));
            root.addView(bar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(6)));
        }

        // Picture banner, where Fire TV puts its featured ad. It sits above the app grid, not
        // inside it, so it stays put while you scroll. Hidden until there are pictures.
        if (imageMode == MODE_BANNER || imageMode == MODE_COMBINED) {
            bannerFront = newPictureView();
            bannerBack = newPictureView();
            bannerBack.setAlpha(0f);
            GradientDrawable frameBg = new GradientDrawable();
            frameBg.setCornerRadius(dp(theme.radiusDp));
            frameBg.setColor(theme.cards[0]);
            bannerFrame = new FrameLayout(this);
            bannerFrame.setBackground(frameBg);
            bannerFrame.setClipToOutline(true);
            bannerFrame.setVisibility(View.GONE);
            bannerFrame.addView(bannerFront, new FrameLayout.LayoutParams(match, match));
            bannerFrame.addView(bannerBack, new FrameLayout.LayoutParams(match, match));
            LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(match, dp(170));
            bannerParams.setMargins(dp(48), dp(12), dp(48), 0);
            root.addView(bannerFrame, bannerParams);
        }

        // App grid
        grid = new GridView(this);
        grid.setNumColumns(COLUMNS);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(dp(20));
        grid.setVerticalSpacing(dp(20));
        grid.setPadding(dp(48), dp(16), dp(48), dp(32));
        grid.setClipToPadding(false);
        grid.setSelector(focusBorder());
        grid.setDrawSelectorOnTop(true);
        grid.setFocusable(true);
        grid.setVerticalScrollBarEnabled(false);

        adapter = new AppAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            AppEntry entry = apps.get(position);
            if (entry.themePicker) {
                showThemePicker();
            } else if (entry.picturePicker) {
                showPicturePicker();
            } else if (entry.screensaverPicker) {
                showScreensaverPicker();
            } else {
                if (entry.amazonHome) {
                    // Stop the auto-open service from bouncing you straight back.
                    prefs.edit().putLong(PREF_PAUSE_UNTIL,
                            System.currentTimeMillis() + AMAZON_PAUSE_MS).apply();
                }
                launch(entry);
            }
        });
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            showItemMenu(apps.get(position), position);
            return true;
        });

        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean top = position < COLUMNS;
                if (top != bannerOnTopRow) {
                    bannerOnTopRow = top;
                    updateBannerVisibility();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        stage.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(stage);
        grid.requestFocus();
    }

    private Drawable focusBorder() {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(theme.radiusDp + 2));
        d.setColor(Color.TRANSPARENT);
        d.setStroke(dp(theme.focusDp), theme.focus);
        return d;
    }

    private Drawable cardBackground(int position) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(theme.radiusDp));
        d.setColor(theme.cards[position % theme.cards.length]);
        return d;
    }

    private Drawable pictureTile() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{theme.focus, theme.bgTop});
        d.setCornerRadius(dp(theme.radiusDp));
        return d;
    }

    private Drawable rainbowTile() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, RAINBOW);
        d.setCornerRadius(dp(theme.radiusDp));
        return d;
    }

    // ---------------------------------------------------------------- Slideshow

    /** Make an (empty) picture folder for every theme so they are easy to find and fill. */
    private void createPictureFolders() {
        File base = getExternalFilesDir(null);
        if (base == null) {
            return;
        }
        for (AppTheme t : THEMES) {
            new File(base, "backgrounds/" + t.folder).mkdirs();
        }
    }

    private List<File> listSlides(AppTheme t) {
        List<File> out = new ArrayList<>();
        File base = getExternalFilesDir(null);
        if (base == null) {
            return out;
        }
        File[] files = new File(base, "backgrounds/" + t.folder).listFiles();
        if (files == null) {
            return out;
        }
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && (n.endsWith(".jpg") || n.endsWith(".jpeg")
                    || n.endsWith(".png") || n.endsWith(".webp")
                    || n.endsWith(".jfif"))) {
                out.add(f);
            }
        }
        return out;
    }

    private void startSlideshow() {
        handler.removeCallbacks(nextSlide);
        slideGeneration++;
        slides = (imageMode == MODE_OFF) ? new ArrayList<File>() : listSlides(theme);
        if (scrimView != null) {
            scrimView.setVisibility(slides.isEmpty() ? View.GONE : View.VISIBLE);
        }
        updateBannerVisibility();
        if (slides.isEmpty()) {
            if (bgFront != null) {
                bgFront.setImageDrawable(null);
            }
            if (bannerFront != null) {
                bannerFront.setImageDrawable(null);
            }
            return;
        }
        Collections.shuffle(slides);
        slideIndex = 0;
        showNextSlide();
    }

    private void stopSlideshow() {
        slideGeneration++;
        handler.removeCallbacks(nextSlide);
    }

    private void showNextSlide() {
        if (slides.isEmpty()) {
            return;
        }
        final int generation = slideGeneration;
        final int n = slides.size();
        final File fileA = slides.get(slideIndex % n);
        final boolean both = bannerFront != null && bgFront != null;
        // With the banner and the background both showing, they get different pictures.
        final File fileB = both ? slides.get((slideIndex + Math.max(1, n / 2)) % n) : fileA;
        slideIndex++;
        new Thread(() -> {
            final Bitmap a = decodeScaled(fileA, 1280, 720);
            final Bitmap b = (both && fileB != fileA) ? decodeScaled(fileB, 1280, 720) : a;
            runOnUiThread(() -> {
                if (generation != slideGeneration) {
                    return; // theme changed or screen left while this was loading
                }
                showBitmaps(a, b != null ? b : a);
                handler.postDelayed(nextSlide, SLIDE_MS);
            });
        }).start();
    }

    private ImageView newPictureView() {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return v;
    }

    /** Fade new pictures in: the banner gets one, the background gets the other. */
    private void showBitmaps(Bitmap forBanner, Bitmap forBackground) {
        if (forBanner != null) {
            fadePair(bannerFront, bannerBack, forBanner);
        }
        if (forBackground != null) {
            fadePair(bgFront, bgBack, forBackground);
        }
    }

    /** Fire TV style: the banner shows while you are on the top row of apps and slides away below. */
    private void updateBannerVisibility() {
        if (bannerFrame != null) {
            boolean show = !slides.isEmpty() && bannerOnTopRow;
            bannerFrame.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void fadePair(final ImageView front, final ImageView back, Bitmap bmp) {
        if (front == null || back == null) {
            return;
        }
        back.setImageBitmap(bmp);
        back.setAlpha(0f);
        back.animate().alpha(1f).setDuration(FADE_MS).withEndAction(() -> {
            front.setImageDrawable(back.getDrawable());
            back.setAlpha(0f);
        }).start();
    }

    /** Load a picture shrunk to roughly TV size so big photos don't use up memory. */
    private Bitmap decodeScaled(File file, int maxW, int maxH) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= maxW
                    && bounds.outHeight / (sample * 2) >= maxH) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (Throwable e) {
            return null; // unreadable file or out of memory: skip this picture
        }
    }

    // ---------------------------------------------------------------- Theme picker

    private void showThemePicker() {
        BaseAdapter list = new BaseAdapter() {
            @Override
            public int getCount() {
                return THEMES.length;
            }

            @Override
            public Object getItem(int position) {
                return THEMES[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                AppTheme t = THEMES[position];

                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(24), dp(14), dp(24), dp(14));

                View swatch = new View(MainActivity.this);
                GradientDrawable sw = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT, t.swatch);
                sw.setCornerRadius(dp(t.radiusDp > 0 ? 8 : 0));
                swatch.setBackground(sw);
                row.addView(swatch, new LinearLayout.LayoutParams(dp(130), dp(40)));

                TextView name = new TextView(MainActivity.this);
                name.setText(t == theme ? t.name + "  (current)" : t.name);
                name.setTextSize(22);
                name.setTextColor(Color.WHITE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = dp(24);
                row.addView(name, lp);
                return row;
            }
        };

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Choose a theme")
                .setAdapter(list, (dialog, which) -> applyThemeIndex(which))
                .show();
    }

    // ---------------------------------------------------------------- Screensaver

    /** Restart the idle countdown. Called on every button press. */
    private void resetIdleTimer() {
        handler.removeCallbacks(idleStart);
        if (screensaverMin > 0 && saverView == null && movingPos < 0) {
            handler.postDelayed(idleStart, screensaverMin * 60L * 1000L);
        }
    }

    /** While the screensaver is turned on, keep the TV awake so Fire TV's own one doesn't start. */
    private void applyKeepScreenOn() {
        if (screensaverMin > 0) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showScreensaver(boolean preview) {
        if (saverView != null || stageView == null) {
            return;
        }
        if (!preview && !hasWindowFocus()) {
            resetIdleTimer();   // a menu is open; try again later
            return;
        }
        // Pictures from the current theme's folder; if it is empty, from any theme's folder.
        List<File> pics = listSlides(theme);
        if (pics.isEmpty()) {
            for (AppTheme t : THEMES) {
                pics.addAll(listSlides(t));
            }
        }
        if (pics.isEmpty()) {
            if (preview) {
                Toast.makeText(this, "No pictures found. Add some to the "
                        + theme.folder + " folder first.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        Collections.shuffle(pics);
        saverFiles = pics;
        saverIndex = 0;

        final int match = ViewGroup.LayoutParams.MATCH_PARENT;
        FrameLayout layer = new FrameLayout(this);
        layer.setBackgroundColor(Color.BLACK);
        saverFront = newPictureView();
        saverBack = newPictureView();
        saverBack.setAlpha(0f);
        layer.addView(saverFront, new FrameLayout.LayoutParams(match, match));
        layer.addView(saverBack, new FrameLayout.LayoutParams(match, match));

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(30);
        clock.setShadowLayer(8, 0, 2, Color.BLACK);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.BOTTOM);
        clockParams.setMargins(0, 0, dp(48), dp(36));
        layer.addView(clock, clockParams);

        stageView.addView(layer, new FrameLayout.LayoutParams(match, match));
        saverView = layer;
        stopSlideshow();   // pause the banner/background slideshow while the screensaver runs
        showNextSaverSlide();
    }

    private void showNextSaverSlide() {
        if (saverView == null || saverFiles.isEmpty()) {
            return;
        }
        final int generation = saverGeneration;
        final File file = saverFiles.get(saverIndex % saverFiles.size());
        saverIndex++;
        new Thread(() -> {
            final Bitmap bmp = decodeScaled(file, 1920, 1080);
            runOnUiThread(() -> {
                if (generation != saverGeneration || saverView == null) {
                    return;   // dismissed while this was loading
                }
                if (bmp != null) {
                    fadePair(saverFront, saverBack, bmp);
                }
                handler.postDelayed(saverNext, SAVER_SLIDE_MS);
            });
        }).start();
    }

    /** Remove the screensaver without restarting anything else. */
    private void dismissSaverQuietly() {
        saverGeneration++;
        handler.removeCallbacks(saverNext);
        if (saverView != null && stageView != null) {
            stageView.removeView(saverView);
        }
        saverView = null;
        saverFront = null;
        saverBack = null;
    }

    private void hideScreensaver() {
        dismissSaverQuietly();
        startSlideshow();
        resetIdleTimer();
    }

    private void showScreensaverPicker() {
        CharSequence[] items = {"Off", "5 minutes", "10 minutes", "15 minutes", "20 minutes"};
        final int[] minutes = {0, 5, 10, 15, 20};
        int checked = 0;
        for (int i = 0; i < minutes.length; i++) {
            if (minutes[i] == screensaverMin) {
                checked = i;
            }
        }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Screensaver")
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    dialog.dismiss();
                    applyScreensaver(minutes[which]);
                })
                .setPositiveButton("Preview now", (dialog, which) ->
                        handler.postDelayed(() -> showScreensaver(true), 300))
                .setNegativeButton("Close", null)
                .show();
    }

    private void applyScreensaver(int minutes) {
        screensaverMin = minutes;
        prefs.edit().putInt(PREF_SAVER_MIN, minutes).apply();
        applyKeepScreenOn();
        resetIdleTimer();
        Toast.makeText(this, minutes == 0 ? "Screensaver is off"
                : "Screensaver starts after " + minutes + " minutes without a button press",
                Toast.LENGTH_LONG).show();
    }

    private void showPicturePicker() {
        CharSequence[] items = {"Banner at the top", "Full-screen background",
                "Banner + background", "Off"};
        final int[] modes = {MODE_BANNER, MODE_BACKGROUND, MODE_COMBINED, MODE_OFF};
        int checked = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == imageMode) {
                checked = i;
            }
        }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pictures")
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    dialog.dismiss();
                    applyImageMode(modes[which]);
                })
                .show();
    }

    private void applyImageMode(int mode) {
        stopSlideshow();
        movingPos = -1;
        imageMode = mode;
        prefs.edit().putInt(PREF_IMAGE_MODE, mode).apply();
        buildUi();
        loadApps();
        startSlideshow();
        if (mode != MODE_OFF && listSlides(theme).isEmpty()) {
            Toast.makeText(this, "No pictures yet. Add some to the "
                    + theme.folder + " folder (see the README).", Toast.LENGTH_LONG).show();
        }
    }

    private void applyThemeIndex(int index) {
        stopSlideshow();
        movingPos = -1;
        prefs.edit().putString(PREF_THEME, THEMES[index].folder).apply();
        theme = THEMES[index];
        buildUi();
        loadApps();
        startSlideshow();
    }

    // ---------------------------------------------------------------- Data

    private void loadApps() {
        PackageManager pm = getPackageManager();
        List<AppEntry> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // TV apps first (they carry banners), then regular launcher apps.
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String category : categories) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) {
                    continue;
                }
                AppEntry entry = new AppEntry();
                entry.packageName = pkg;
                entry.label = String.valueOf(info.loadLabel(pm));
                entry.intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(category)
                        .setClassName(pkg, info.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                Drawable banner = info.activityInfo.loadBanner(pm);
                if (banner == null) {
                    banner = info.activityInfo.applicationInfo.loadBanner(pm);
                }
                if (banner != null) {
                    entry.image = banner;
                    entry.isBanner = true;
                } else {
                    entry.image = info.loadIcon(pm);
                    entry.isBanner = false;
                }
                found.add(entry);
            }
        }

        Collections.sort(found, (a, b) -> a.label.toLowerCase(Locale.ROOT)
                .compareTo(b.label.toLowerCase(Locale.ROOT)));

        // Apply the order chosen with "Move"; apps not in it yet go after, A-Z.
        String savedOrder = prefs.getString(PREF_ORDER, "");
        if (!savedOrder.isEmpty()) {
            Map<String, AppEntry> byPkg = new HashMap<>();
            for (AppEntry e : found) {
                byPkg.put(e.packageName, e);
            }
            List<AppEntry> ordered = new ArrayList<>();
            for (String pkg : savedOrder.split("\n")) {
                AppEntry e = byPkg.remove(pkg);
                if (e != null) {
                    ordered.add(e);
                }
            }
            for (AppEntry e : found) {
                if (byPkg.containsKey(e.packageName)) {
                    ordered.add(e);
                }
            }
            found.clear();
            found.addAll(ordered);
        }

        // Themes tile
        AppEntry themes = new AppEntry();
        themes.label = "Themes";
        themes.packageName = "themes";
        themes.themePicker = true;
        themes.fixed = true;
        themes.image = rainbowTile();
        themes.isBanner = true;
        found.add(themes);

        // Pictures tile (banner / background / off)
        AppEntry pictures = new AppEntry();
        pictures.label = "Pictures";
        pictures.packageName = "pictures";
        pictures.picturePicker = true;
        pictures.fixed = true;
        pictures.image = pictureTile();
        pictures.isBanner = true;
        found.add(pictures);

        // Screensaver settings
        AppEntry saver = new AppEntry();
        saver.label = "Screensaver";
        saver.packageName = "screensaver";
        saver.screensaverPicker = true;
        saver.fixed = true;
        saver.image = getResources().getDrawable(android.R.drawable.ic_menu_slideshow);
        saver.isBanner = false;
        found.add(saver);

        // A way to reach Amazon's own home screen when auto-open is turned on.
        AppEntry amazon = new AppEntry();
        amazon.label = "Amazon Home";
        amazon.packageName = "amazonhome";
        amazon.amazonHome = true;
        amazon.fixed = true;
        amazon.intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage("com.amazon.tv.launcher")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        amazon.image = getResources().getDrawable(android.R.drawable.ic_menu_revert);
        amazon.isBanner = false;
        found.add(amazon);

        // Always provide a way into system settings.
        AppEntry settings = new AppEntry();
        settings.label = "Settings";
        settings.packageName = "settings";
        settings.fixed = true;
        settings.intent = new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settings.image = getResources().getDrawable(android.R.drawable.ic_menu_preferences);
        settings.isBanner = false;
        found.add(settings);

        apps.clear();
        apps.addAll(found);
        adapter.notifyDataSetChanged();
    }

    private void launch(AppEntry entry) {
        try {
            startActivity(entry.intent);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open " + entry.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppInfo(AppEntry entry) {
        if (entry.themePicker || entry.picturePicker || entry.screensaverPicker || entry.amazonHome
                || "settings".equals(entry.packageName)) {
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + entry.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, "App info isn't available for this app", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------------------------------------------------------------- Adapter

    private class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public Object getItem(int position) {
            return apps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout tile;
            ImageView image;
            TextView label;

            if (convertView == null) {
                tile = new LinearLayout(MainActivity.this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setPadding(dp(6), dp(6), dp(6), dp(6));

                image = new ImageView(MainActivity.this);
                tile.addView(image, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(112)));

                label = new TextView(MainActivity.this);
                label.setTextSize(15);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                label.setPadding(0, dp(8), 0, dp(4));
                tile.addView(label, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                tile.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                tile = (LinearLayout) convertView;
                image = (ImageView) tile.getChildAt(0);
                label = (TextView) tile.getChildAt(1);
            }

            AppEntry entry = apps.get(position);
            image.setBackground(cardBackground(position));
            image.setImageDrawable(entry.image);
            if (entry.isBanner) {
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setPadding(0, 0, 0, 0);
                image.setClipToOutline(true);
            } else {
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setPadding(dp(28), dp(14), dp(28), dp(14));
                image.setClipToOutline(false);
            }
            label.setTextColor(theme.text);
            String text = theme.caps ? entry.label.toUpperCase(Locale.ROOT) : entry.label;
            boolean moving = position == movingPos;
            label.setText(moving ? text + " (moving)" : text);
            tile.setScaleX(moving ? 1.08f : 1f);
            tile.setScaleY(moving ? 1.08f : 1f);
            return tile;
        }
    }
}
