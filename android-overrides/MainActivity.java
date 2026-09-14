package com.ronda.calcul;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // ⚡ الحالة
    private Map<Integer, Integer> left = new HashMap<>();
    private int[] allNums = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12};
    private int history[] = new int[100]; // آخر 100 ورقة
    private int historyCount = 0;

    // ⚡ Views
    private LinearLayout rootLayout;
    private LinearLayout droppedBar;
    private LinearLayout gridLayout;
    private TextView counterView;
    private TextView droppedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ⚡ شفافية النافذة
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // ⚡ نبنيو الواجهة
        buildUI();

        // ⚡ نبداو الـ Overlay
        startOverlayService();
    }

    private void buildUI() {
        // Root Layout
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.TRANSPARENT);
        rootLayout.setPadding(20, 20, 20, 20);

        // شريط آخر الأوراق
        droppedView = new TextView(this);
        droppedView.setText("آخر الأوراق: لا توجد");
        droppedView.setTextColor(Color.WHITE);
        droppedView.setTextSize(14);
        droppedView.setPadding(10, 10, 10, 10);
        droppedView.setBackgroundColor(0x44000000);
        rootLayout.addView(droppedView);

        // Counter
        counterView = new TextView(this);
        counterView.setText("40");
        counterView.setTextColor(Color.parseColor("#FFD700"));
        counterView.setTextSize(32);
        counterView.setPadding(10, 10, 10, 10);
        rootLayout.addView(counterView);

        // Grid - الأرقام
        gridLayout = new LinearLayout(this);
        gridLayout.setOrientation(LinearLayout.VERTICAL);

        // صفوف الشبكة (3 صفوف × 4 أعمدة)
        int[][] gridRows = {
            {1, 2, 3, 4},
            {5, 6, 7, 10},
            {11, 12, -1, -2} // -1 = فراغ، -2 = زر تراجع
        };

        for (int[] row : gridRows) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int num : row) {
                if (num == -1) {
                    // فراغ
                    View empty = new View(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    );
                    empty.setLayoutParams(params);
                    rowLayout.addView(empty);
                } else if (num == -2) {
                    // زر تراجع
                    Button undoBtn = new Button(this);
                    undoBtn.setText("↩");
                    undoBtn.setTextSize(20);
                    undoBtn.setOnClickListener(v -> undo());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, 200, 1
                    );
                    params.setMargins(5, 5, 5, 5);
                    undoBtn.setLayoutParams(params);
                    rowLayout.addView(undoBtn);
                } else {
                    // زر رقم
                    final int numFinal = num;
                    Button btn = new Button(this);
                    btn.setText(String.valueOf(num));
                    btn.setTextSize(22);
                    btn.setTag(num);
                    updateButtonState(btn, 4);
                    btn.setOnClickListener(v -> handleTap(numFinal));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, 200, 1
                    );
                    params.setMargins(5, 5, 5, 5);
                    btn.setLayoutParams(params);
                    rowLayout.addView(btn);
                }
            }
            gridLayout.addView(rowLayout);
        }

        rootLayout.addView(gridLayout);

        // زر التحديث
        Button resetBtn = new Button(this);
        resetBtn.setText("🔄 تحديث");
        resetBtn.setTextSize(16);
        resetBtn.setOnClickListener(v -> resetAll());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120
        );
        params.setMargins(5, 10, 5, 5);
        resetBtn.setLayoutParams(params);
        rootLayout.addView(resetBtn);

        // نضيفو للـ Activity
        setContentView(rootLayout);

        // ⚡ نبداو الحالة
        for (int n : allNums) {
            left.put(n, 4);
        }
        render();
    }

    private void updateButtonState(Button btn, int count) {
        if (count <= 0) {
            btn.setEnabled(false);
            btn.setAlpha(0.3f);
        } else {
            btn.setEnabled(true);
            btn.setAlpha(1.0f);
            if (count == 1) {
                btn.setBackgroundColor(Color.parseColor("#CCFF0000"));
                btn.setTextColor(Color.WHITE);
            } else if (count == 2) {
                btn.setBackgroundColor(Color.parseColor("#CCFFD700"));
                btn.setTextColor(Color.BLACK);
            } else if (count == 3) {
                btn.setBackgroundColor(Color.parseColor("#CC00CC44"));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.parseColor("#66FFFFFF"));
                btn.setTextColor(Color.WHITE);
            }
        }
    }

    private void handleTap(int num) {
        int current = left.get(num);
        if (current <= 0) return;

        left.put(num, current - 1);
        history[historyCount % 100] = num;
        historyCount++;

        render();
    }

    private void undo() {
        if (historyCount == 0) return;
        historyCount--;
        int lastNum = history[historyCount % 100];
        left.put(lastNum, left.get(lastNum) + 1);
        render();
    }

    private void resetAll() {
        for (int n : allNums) {
            left.put(n, 4);
        }
        historyCount = 0;
        render();
    }

    private void render() {
        // نحدّثو الأزرار
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) gridLayout.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                View v = row.getChildAt(j);
                if (v instanceof Button) {
                    Button btn = (Button) v;
                    Object tag = btn.getTag();
                    if (tag instanceof Integer) {
                        int num = (Integer) tag;
                        updateButtonState(btn, left.get(num));
                    }
                }
            }
        }

        // نحدّثو العداد
        int total = 0;
        for (int n : allNums) {
            total += left.get(n);
        }
        counterView.setText(String.valueOf(total));

        // نحدّثو آخر الأوراق
        StringBuilder sb = new StringBuilder("آخر الأوراق: ");
        int start = Math.max(0, historyCount - 4);
        if (historyCount == 0) {
            sb.append("لا توجد");
        } else {
            for (int i = start; i < historyCount; i++) {
                sb.append("#").append(history[i % 100]).append(" ");
            }
        }
        droppedView.setText(sb.toString());
    }

    private void startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
                return;
            }
        }
        Intent serviceIntent = new Intent(this, OverlayService.class);
        startService(serviceIntent);
    }
}