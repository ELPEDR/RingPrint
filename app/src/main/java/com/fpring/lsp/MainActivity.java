package com.fpring.lsp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    static final String PREFS = "cfg";
    private static final int MIN_SIZE = 30;      // seekbar 0..50 -> 30..80
    private static final int MIN_STROKE10 = 10;  // seekbar 0..40 -> 1.0..5.0
    private static final int MAX_IMPORT = 900 * 1024;
    private static final int REQ_DARK = 1, REQ_LIGHT = 2;

    private int size, stroke10;
    private Preview preview;
    private TextView sizeLabel, strokeLabel, darkStatus, lightStatus;
    private SeekBar sizeBar, strokeBar;
    private Bitmap darkBmp;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        size = sp.getInt("size", Icons.DEF_SIZE);
        stroke10 = sp.getInt("stroke10", Icons.DEF_STROKE10);

        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText("Fingerprint Ring");
        title.setTextSize(24);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Ajuste o aro (ou importe um icone) e toque em Aplicar. O processo da digital sera reiniciado (precisa de root).");
        hint.setPadding(0, dp(6), 0, dp(12));
        root.addView(hint);

        preview = new Preview(this);
        root.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        sizeLabel = new TextView(this);
        sizeLabel.setPadding(0, dp(16), 0, 0);
        root.addView(sizeLabel);
        sizeBar = new SeekBar(this);
        sizeBar.setMax(50);
        sizeBar.setProgress(size - MIN_SIZE);
        root.addView(sizeBar);

        strokeLabel = new TextView(this);
        strokeLabel.setPadding(0, dp(12), 0, 0);
        root.addView(strokeLabel);
        strokeBar = new SeekBar(this);
        strokeBar.setMax(40);
        strokeBar.setProgress(stroke10 - MIN_STROKE10);
        root.addView(strokeBar);

        SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                size = MIN_SIZE + sizeBar.getProgress();
                stroke10 = MIN_STROKE10 + strokeBar.getProgress();
                refresh();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        };
        sizeBar.setOnSeekBarChangeListener(l);
        strokeBar.setOnSeekBarChangeListener(l);

        // ---- icone personalizado ----
        TextView custom = new TextView(this);
        custom.setText("Icone personalizado (opcional)");
        custom.setTextSize(18);
        custom.setPadding(0, dp(20), 0, 0);
        root.addView(custom);

        TextView info = new TextView(this);
        info.setText("Aceita PNG com fundo transparente (usa o diametro acima) ou Lottie .json (usado como esta). "
                + "Use um icone branco no tema escuro/AOD e um escuro no tema claro.");
        info.setPadding(0, dp(4), 0, dp(8));
        root.addView(info);

        darkStatus = new TextView(this);
        root.addView(darkStatus);
        LinearLayout r1 = new LinearLayout(this);
        r1.addView(button("Importar escuro/AOD", v -> pick(REQ_DARK)), weight());
        r1.addView(button("Remover", v -> clearSlot("dark")), weight());
        root.addView(r1);

        lightStatus = new TextView(this);
        lightStatus.setPadding(0, dp(8), 0, 0);
        root.addView(lightStatus);
        LinearLayout r2 = new LinearLayout(this);
        r2.addView(button("Importar claro", v -> pick(REQ_LIGHT)), weight());
        r2.addView(button("Remover", v -> clearSlot("light")), weight());
        root.addView(r2);

        // ---- acoes ----
        Button apply = button("Aplicar (reiniciar processo da digital)", v -> apply());
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin = dp(20);
        root.addView(apply, ap);

        root.addView(button("Restaurar aro padrao", v -> {
            sizeBar.setProgress(Icons.DEF_SIZE - MIN_SIZE);
            strokeBar.setProgress(Icons.DEF_STROKE10 - MIN_STROKE10);
            clearSlot("dark");
            clearSlot("light");
        }));

        root.addView(button("Reiniciar o celular", v -> new AlertDialog.Builder(this)
                .setMessage("Reiniciar o celular agora?")
                .setPositiveButton("Reiniciar", (d, w) -> su("reboot", null))
                .setNegativeButton("Cancelar", null)
                .show()));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
        refresh();
    }

    private Button button(String text, View.OnClickListener c) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(c);
        return b;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private SharedPreferences sp() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private String kind(String slot) {
        return sp().getString(slot + "_kind", "");
    }

    private String describe(String slot) {
        String k = kind(slot);
        String what = k.isEmpty() ? "aro padrao" : ("png".equals(k) ? "PNG importado" : "Lottie importado");
        return ("dark".equals(slot) ? "Escuro/AOD: " : "Claro: ") + what
                + ("light".equals(slot) && k.isEmpty() ? " (usa o escuro, se houver)" : "");
    }

    private void refresh() {
        sizeLabel.setText("Diametro: " + size + "  (original Samsung ~46, vale so para aro e PNG)");
        strokeLabel.setText(String.format(Locale.US, "Espessura do aro: %.1f", stroke10 / 10f));
        darkStatus.setText(describe("dark"));
        lightStatus.setText(describe("light"));
        darkBmp = "png".equals(kind("dark"))
                ? BitmapFactory.decodeFile(new File(getFilesDir(), "custom_dark.png").getPath()) : null;
        preview.invalidate();
    }

    // ---------- importar ----------
    private void pick(int req) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "image/png", "application/json", "text/plain", "application/octet-stream"});
        startActivityForResult(i, req);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        if (req == REQ_DARK) importUri(data.getData(), "dark");
        else if (req == REQ_LIGHT) importUri(data.getData(), "light");
    }

    private void importUri(Uri uri, String slot) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while (in != null && (n = in.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > MAX_IMPORT) { toast("Arquivo grande demais (max ~900 KB)"); return; }
            }
            byte[] d = bo.toByteArray();
            String kind;
            byte[] out = d;
            if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
                kind = "png";
                Bitmap bm = BitmapFactory.decodeByteArray(d, 0, d.length);
                if (bm == null) { toast("PNG invalido"); return; }
                int max = Math.max(bm.getWidth(), bm.getHeight());
                if (max > 256) {
                    float k = 256f / max;
                    bm = Bitmap.createScaledBitmap(bm, Math.max(1, Math.round(bm.getWidth() * k)),
                            Math.max(1, Math.round(bm.getHeight() * k)), true);
                }
                ByteArrayOutputStream po = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.PNG, 100, po);
                out = po.toByteArray();
            } else {
                JSONObject o = new JSONObject(new String(d, StandardCharsets.UTF_8));
                if (!o.has("layers")) { toast("Esse JSON nao parece um Lottie"); return; }
                kind = "json";
            }
            clearFiles(slot);
            try (FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), "custom_" + slot + "." + kind))) {
                fo.write(out);
            }
            sp().edit().putString(slot + "_kind", kind).putLong("ver", System.currentTimeMillis()).commit();
            toast("Importado. Toque em Aplicar.");
            refresh();
        } catch (Exception e) {
            toast("Falha ao importar: " + e.getMessage());
        }
    }

    private void clearFiles(String slot) {
        new File(getFilesDir(), "custom_" + slot + ".png").delete();
        new File(getFilesDir(), "custom_" + slot + ".json").delete();
    }

    private void clearSlot(String slot) {
        clearFiles(slot);
        sp().edit().putString(slot + "_kind", "").putLong("ver", System.currentTimeMillis()).commit();
        refresh();
    }

    // ---------- aplicar / root ----------
    private void apply() {
        sp().edit().putInt("size", size).putInt("stroke10", stroke10).commit();
        su("pkill -f com.samsung.android.biometrics.app.setting"
                        + " || am force-stop com.samsung.android.biometrics.app.setting",
                "Aplicado. Bloqueie e desbloqueie a tela para ver.");
    }

    private void toast(String m) {
        runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show());
    }

    /** Roda um comando como root, sem travar a tela. */
    private void su(String cmd, String okMsg) {
        new Thread(() -> {
            String msg;
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                int code = p.waitFor();
                msg = (code == 0 || okMsg == null) ? okMsg : "Comando terminou com codigo " + code;
            } catch (Exception e) {
                msg = "Sem acesso root: " + e.getMessage();
            }
            if (msg != null) toast(msg);
        }).start();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** Previa: o icone e desenhado numa tela de 288 e ampliado 400% (como no Lottie). */
    private class Preview extends View {
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);

        Preview(Context c) {
            super(c);
            bg.setColor(0xFF1C1C22);
            ring.setColor(0xFFFAFAFA);
            ring.setStyle(Paint.Style.STROKE);
            txt.setColor(0xFFCCCCCC);
            txt.setTextAlign(Paint.Align.CENTER);
            txt.setTextSize(dp(14));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            c.drawRoundRect(0, 0, w, h, dp(16), dp(16), bg);
            float scale = Math.min(w, h) / 288f;
            String k = kind("dark");
            if ("png".equals(k) && darkBmp != null) {
                float d = size * 4f * scale;
                float f = d / Math.max(darkBmp.getWidth(), darkBmp.getHeight());
                float bw = darkBmp.getWidth() * f, bh = darkBmp.getHeight() * f;
                c.drawBitmap(darkBmp, null,
                        new RectF(w / 2f - bw / 2f, h / 2f - bh / 2f, w / 2f + bw / 2f, h / 2f + bh / 2f), null);
            } else if ("json".equals(k)) {
                c.drawText("Lottie importado (sem previa)", w / 2f, h / 2f, txt);
            } else {
                ring.setStrokeWidth(stroke10 / 10f * 4f * scale);
                c.drawCircle(w / 2f, h / 2f, size * 4f * scale / 2f, ring);
            }
        }
    }
}
