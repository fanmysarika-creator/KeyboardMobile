package com.dima.gkeyboard;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.util.Random;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GKeyboardService extends InputMethodService {

    private static final String[][] EN = {
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
            {"z", "x", "c", "v", "b", "n", "m"}};
    private static final String[][] RU = {
            {"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"},
            {"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"},
            {"я", "ч", "с", "м", "и", "т", "ь", "б", "ю"}};
    private static final String[][] SYM = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"@", "#", "$", "%", "&", "-", "+", "(", ")"},
            {"*", "\"", "'", ":", ";", "!", "?"}};

    private static final int L_EN = 0, L_RU = 1, L_SYM = 2;

    private int layer = L_EN;
    private int lastLetters = L_EN;
    private boolean shift = false, caps = false;
    private long lastShiftTap = 0;

    private LinearLayout root;
    private boolean dark;
    private int cBg, cKey, cKeySpecial, cText, cAccent;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SoundPool pool;
    private final int[] sounds = new int[3];
    private final Random rnd = new Random();
    private static final float VOLUME = 0.6f; // мягкий звук

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(aa).build();
            sounds[0] = pool.load(this, R.raw.key1, 1);
            sounds[1] = pool.load(this, R.raw.key2, 1);
            sounds[2] = pool.load(this, R.raw.key3, 1);
        } catch (Exception ignored) {
            pool = null;
        }
    }

    @Override
    public void onDestroy() {
        if (pool != null) pool.release();
        super.onDestroy();
    }

    private void tap(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (pool != null) {
            pool.play(sounds[rnd.nextInt(3)], VOLUME, VOLUME, 1, 0, 1f);
        }
    }

    @Override
    public View onCreateInputView() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        initColors();
        int p = dp(4);
        root.setPadding(p, p, p, p);
        build();
        return root;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE) {
            layer = L_SYM;
        } else if (layer == L_SYM) {
            layer = lastLetters;
        }
        caps = false;
        shift = false;
        updateAutoCap();
        if (root != null) {
            initColors();
            build();
        }
    }

    private void initColors() {
        dark = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (dark) {
            cBg = Color.parseColor("#1F2023");
            cKey = Color.parseColor("#3A3B3F");
            cKeySpecial = Color.parseColor("#2B2C30");
            cText = Color.parseColor("#EDEDED");
            cAccent = Color.parseColor("#8AB4F8");
        } else {
            cBg = Color.parseColor("#E8EAED");
            cKey = Color.WHITE;
            cKeySpecial = Color.parseColor("#D3D6DB");
            cText = Color.parseColor("#202124");
            cAccent = Color.parseColor("#1A73E8");
        }
        root.setBackgroundColor(cBg);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void build() {
        root.removeAllViews();
        String[][] rows = layer == L_RU ? RU : layer == L_SYM ? SYM : EN;
        boolean upper = shift || caps;

        // Row 1 and 2
        for (int r = 0; r < 2; r++) {
            LinearLayout row = newRow();
            if (r == 1 && rows[1].length < rows[0].length) {
                row.addView(spacer(0.5f));
            }
            for (String k : rows[r]) row.addView(charKey(k, upper));
            if (r == 1 && rows[1].length < rows[0].length) {
                row.addView(spacer(0.5f));
            }
            root.addView(row);
        }

        // Row 3: shift + letters + backspace
        LinearLayout row3 = newRow();
        if (layer == L_SYM) {
            row3.addView(special("=\\<", 1.5f, new Runnable() {
                public void run() { /* reserved */ }
            }, false));
        } else {
            String label = caps ? "⇪" : "⇧";
            row3.addView(special(label, 1.5f, new Runnable() {
                public void run() { toggleShift(); }
            }, shift || caps));
        }
        for (String k : rows[2]) row3.addView(charKey(k, upper));
        row3.addView(backspaceKey());
        root.addView(row3);

        // Row 4: bottom
        LinearLayout row4 = newRow();
        row4.addView(special(layer == L_SYM ? "ABC" : "?123", 1.5f, new Runnable() {
            public void run() {
                if (layer == L_SYM) {
                    layer = lastLetters;
                } else {
                    layer = L_SYM;
                }
                build();
            }
        }, false));
        row4.addView(globeKey());
        row4.addView(charKey(",", false, 1f));
        row4.addView(spaceKey());
        row4.addView(charKey(".", false, 1f));
        row4.addView(enterKey());
        root.addView(row4);
    }

    private LinearLayout newRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        return row;
    }

    private View spacer(float weight) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, weight));
        return v;
    }

    private TextView baseKey(String text, float weight, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(cText);
        t.setTextSize(20);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, weight);
        int m = dp(3);
        lp.setMargins(m, m, m, m);
        t.setLayoutParams(lp);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(8));
        g.setColor(color);
        t.setBackground(g);
        return t;
    }

    private View charKey(String k, boolean upper) {
        return charKey(k, upper, 1f);
    }

    private View charKey(final String k, boolean upper, float weight) {
        final String out = upper ? k.toUpperCase() : k;
        TextView t = baseKey(out, weight, cKey);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tap(v);
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) return;
                ic.commitText(out, 1);
                if (shift && !caps) shift = false;
                updateAutoCapAfterChar(out);
                build();
            }
        });
        return t;
    }

    private View special(String label, float weight, final Runnable action, boolean active) {
        TextView t = baseKey(label, weight, cKeySpecial);
        t.setTextSize(17);
        if (active) t.setTextColor(cAccent);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tap(v);
                action.run();
            }
        });
        return t;
    }

    private View globeKey() {
        TextView t = baseKey(layer == L_RU ? "RU" : "EN", 1f, cKeySpecial);
        t.setTextSize(15);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tap(v);
                if (layer == L_SYM) {
                    layer = lastLetters;
                } else {
                    layer = layer == L_EN ? L_RU : L_EN;
                }
                lastLetters = layer;
                build();
            }
        });
        t.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
                return true;
            }
        });
        return t;
    }

    private View spaceKey() {
        TextView t = baseKey(layer == L_RU ? "Русский" : layer == L_EN ? "English" : "", 4f, cKey);
        t.setTextSize(13);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tap(v);
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) return;
                ic.commitText(" ", 1);
                updateAutoCapAfterChar(" ");
                build();
            }
        });
        return t;
    }

    private View enterKey() {
        TextView t = baseKey("⏎", 1.5f, cAccent);
        t.setTextColor(dark ? Color.BLACK : Color.WHITE);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tap(v);
                InputConnection ic = getCurrentInputConnection();
                EditorInfo ei = getCurrentInputEditorInfo();
                if (ic == null) return;
                int action = ei == null ? EditorInfo.IME_ACTION_NONE
                        : (ei.imeOptions & EditorInfo.IME_MASK_ACTION);
                boolean noEnterAction = ei != null
                        && (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
                if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE
                        && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action);
                } else {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                }
                updateAutoCap();
                build();
            }
        });
        return t;
    }

    private final Runnable repeatDelete = new Runnable() {
        public void run() {
            doDelete();
            handler.postDelayed(this, 50);
        }
    };

    private View backspaceKey() {
        TextView t = baseKey("⌫", 1.5f, cKeySpecial);
        t.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        tap(v);
                        v.setPressed(true);
                        doDelete();
                        handler.postDelayed(repeatDelete, 400);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        handler.removeCallbacks(repeatDelete);
                        updateAutoCap();
                        build();
                        return true;
                }
                return true;
            }
        });
        return t;
    }

    private void doDelete() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) {
            ic.commitText("", 1);
        } else {
            ic.deleteSurroundingText(1, 0);
        }
    }

    private void toggleShift() {
        long now = System.currentTimeMillis();
        if (caps) {
            caps = false;
            shift = false;
        } else if (now - lastShiftTap < 350) {
            caps = true;
            shift = true;
        } else {
            shift = !shift;
        }
        lastShiftTap = now;
        build();
    }

    private void updateAutoCap() {
        if (caps) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null && (ei.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
                && (ei.inputType & (InputType.TYPE_TEXT_VARIATION_PASSWORD
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                | InputType.TYPE_TEXT_VARIATION_URI)) != 0) {
            shift = false;
            return;
        }
        CharSequence before = ic.getTextBeforeCursor(2, 0);
        if (before == null || before.length() == 0) {
            shift = true;
        } else {
            String s = before.toString();
            shift = s.endsWith("\n")
                    || s.equals(". ") || s.equals("! ") || s.equals("? ");
        }
    }

    private void updateAutoCapAfterChar(String ch) {
        if (!caps) updateAutoCap();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (root != null) {
            initColors();
            build();
        }
    }
}
