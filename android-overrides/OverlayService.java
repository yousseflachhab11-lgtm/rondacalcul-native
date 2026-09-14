package com.ronda.calcul;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private LinearLayout rootView;
    private WindowManager.LayoutParams params;

    // ⚡ الحالة
    private Map<Integer, Integer> left = new HashMap<>();
    private int[] allNums = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12};
    private int[] history = new int[100];
    private int historyCount = 0;

    // ⚡ Views
    private LinearLayout gridLayout;
    private TextView counterView;
    private TextView droppedView;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;

        // ⚡ 35% من عرض الشاشة
        int widthPx = (int) (screenWidth * 0.35);
        int heightPx = WindowManager.LayoutParams.WRAP_CONTENT;

        // ⚡ بنيو الـ View
        buildOverlayView();

        params = new WindowManager.LayoutParams(
            widthPx,
            heightPx,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dpToPx(5);
        params.y = dpToPx(50);

        // ⚡ الحالة الأولية
        for (int n : allNums) {
            left.put(n, 4);
        }

        try {
            windowManager.addView(rootView, params);
            render();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildOverlayView() {
        rootView = new LinearLayout(this);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));

        // ⚡ خلفية زجاجية شفافة
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#AA14141A"));
        bg.setCornerRadius(dpToPx(12));
        bg.setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"));
        rootView.setBackground(bg);

        // ⚡ شريط آخر الأوراق
        droppedView = new TextView(this);
        droppedView.setText("📌 لا توجد");
        droppedView.setTextColor(Color.WHITE);
        droppedView.setTextSize(11);
        droppedView.setPadding(dpToPx(4), dpToPx(3), dpToPx(4), dpToPx(3));
        rootView.addView(droppedView);

        // ⚡ الشبكة (grid)
        gridLayout = new LinearLayout(this);
        gridLayout.setOrientation(LinearLayout.VERTICAL);
        gridLayout.setPadding(0, dpToPx(4), 0, dpToPx(4));

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
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        0, dpToPx(44), 1
                    );
                    p.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                    empty.setLayoutParams(p);
                    rowLayout.addView(empty);
                } else if (num == -2) {
                    // زر تراجع
                    Button undoBtn = new Button(this);
                    undoBtn.setText("↩");
                    undoBtn.setTextSize(18);
                    undoBtn.setTextColor(Color.WHITE);
                    undoBtn.setPadding(0, 0, 0, 0);
                    undoBtn.setMinWidth(0);
                    undoBtn.setMinHeight(0);
                    
                    GradientDrawable gd = new GradientDrawable();
                    gd.setColor(Color.parseColor("#55FFFFFF"));
                    gd.setCornerRadius(dpToPx(6));
                    undoBtn.setBackground(gd);
                    
                    undoBtn.setOnClickListener(v -> undo());
                    
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        0, dpToPx(44), 1
                    );
                    p.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                    undoBtn.setLayoutParams(p);
                    rowLayout.addView(undoBtn);
                } else {
                    // زر رقم
                    final int numFinal = num;
                    Button btn = new Button(this);
                    btn.setText(String.valueOf(num));
                    btn.setTextSize(18);
                    btn.setTextColor(Color.WHITE);
                    btn.setPadding(0, 0, 0, 0);
                    btn.setMinWidth(0);
                    btn.setMinHeight(0);
                    btn.setTag(num);
                    
                    btn.setOnClickListener(v -> handleTap(numFinal));
                    
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        0, dpToPx(44), 1
                    );
                    p.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                    btn.setLayoutParams(p);
                    rowLayout.addView(btn);
                }
            }
            gridLayout.addView(rowLayout);
        }

        rootView.addView(gridLayout);

        // ⚡ الفوتر (العداد + تحديث)
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);

        counterView = new TextView(this);
        counterView.setText("40");
        counterView.setTextColor(Color.parseColor("#FFD700"));
        counterView.setTextSize(18);
        counterView.setGravity(Gravity.CENTER);
        counterView.setPadding(dpToPx(4), 0, dpToPx(4), 0);
        
        GradientDrawable counterBg = new GradientDrawable();
        counterBg.setColor(Color.parseColor("#AA000000"));
        counterBg.setCornerRadius(dpToPx(6));
        counterView.setBackground(counterBg);
        
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            dpToPx(48), dpToPx(36)
        );
        cp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
        counterView.setLayoutParams(cp);
        footer.addView(counterView);

        Button resetBtn = new Button(this);
        resetBtn.setText("🔄");
        resetBtn.setTextSize(14);
        resetBtn.setTextColor(Color.WHITE);
        resetBtn.setPadding(0, 0, 0, 0);
        resetBtn.setMinWidth(0);
        resetBtn.setMinHeight(0);
        
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(Color.parseColor("#55FFFFFF"));
        resetBg.setCornerRadius(dpToPx(6));
        resetBtn.setBackground(resetBg);
        
        resetBtn.setOnClickListener(v -> resetAll());
        
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            0, dpToPx(36), 1
        );
        rp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
        resetBtn.setLayoutParams(rp);
        footer.addView(resetBtn);

        rootView.addView(footer);
    }

    private void updateButtonState(Button btn, int count) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dpToPx(6));

        if (count <= 0) {
            gd.setColor(Color.parseColor("#33FFFFFF"));
            btn.setTextColor(Color.parseColor("#66FFFFFF"));
            btn.setEnabled(false);
        } else {
            btn.setEnabled(true);
            if (count == 1) {
                gd.setColor(Color.parseColor("#CCE63B3B"));
                btn.setTextColor(Color.WHITE);
            } else if (count == 2) {
                gd.setColor(Color.parseColor("#CCFFD700"));
                btn.setTextColor(Color.BLACK);
            } else if (count == 3) {
                gd.setColor(Color.parseColor("#CC2ECC55"));
                btn.setTextColor(Color.WHITE);
            } else {
                gd.setColor(Color.parseColor("#33FFFFFF"));
                btn.setTextColor(Color.WHITE);
            }
        }
        btn.setBackground(gd);
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
        StringBuilder sb = new StringBuilder("📌 ");
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootView != null && windowManager != null) {
            try {
                windowManager.removeView(rootView);
            } catch (Exception e) {
                // تجاهل
            }
        }
    }
}