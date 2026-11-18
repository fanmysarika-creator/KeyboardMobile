package com.dima.gkeyboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GKeyboard");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Два простых шага, чтобы сделать клавиатуру основной:");
        hint.setTextSize(15);
        hint.setTextColor(Color.DKGRAY);
        hint.setPadding(0, pad, 0, pad);
        root.addView(hint);

        Button b1 = new Button(this);
        b1.setText("1. Включить GKeyboard");
        b1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        root.addView(b1);

        Button b2 = new Button(this);
        b2.setText("2. Выбрать по умолчанию");
        b2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
            }
        });
        root.addView(b2);

        EditText test = new EditText(this);
        test.setHint("Поле для проверки клавиатуры");
        test.setPadding(0, pad * 2, 0, pad);
        root.addView(test);

        setContentView(root);
    }
}
