package com.fpring.lsp;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Intercepta a leitura de assets do app de biometria da Samsung.
 * Quando ele pede um ic_fingerprint_*.json, devolve o aro padrao (Icons.java)
 * ou o icone importado no app do modulo (PNG ou Lottie), lidos via ContentProvider.
 */
public class Hook implements IXposedHookLoadPackage {
    private static final String TARGET = "com.samsung.android.biometrics.app.setting";
    private static final Uri CFG = Uri.parse("content://com.fpring.lsp.config/cfg");
    private static final String FILE = "content://com.fpring.lsp.config/file/";

    private static volatile int size = Icons.DEF_SIZE;
    private static volatile int stroke10 = Icons.DEF_STROKE10;
    private static volatile String darkKind = "", lightKind = "";
    private static volatile byte[] darkData, lightData;
    private static long ver = -1;
    private static long lastRead = 0;

    private static byte[] readAll(Context c, String slot) {
        try (InputStream in = c.getContentResolver().openInputStream(Uri.parse(FILE + slot))) {
            if (in == null) return null;
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toByteArray();
        } catch (Throwable t) {
            XposedBridge.log("FpRing: erro lendo " + slot + ": " + t);
            return null;
        }
    }

    private static synchronized void refreshConfig() {
        long now = System.currentTimeMillis();
        if (now - lastRead < 3000) return;
        lastRead = now;
        try {
            Context c = AndroidAppHelper.currentApplication();
            if (c == null) return;
            Cursor cur = c.getContentResolver().query(CFG, null, null, null, null);
            if (cur == null) return;
            try {
                if (!cur.moveToFirst()) return;
                size = cur.getInt(0);
                stroke10 = cur.getInt(1);
                String dk = cur.getString(2), lk = cur.getString(3);
                long v = cur.getLong(4);
                if (v != ver) {
                    ver = v;
                    darkKind = dk == null ? "" : dk;
                    lightKind = lk == null ? "" : lk;
                    darkData = darkKind.isEmpty() ? null : readAll(c, "dark");
                    lightData = lightKind.isEmpty() ? null : readAll(c, "light");
                    XposedBridge.log("FpRing: config size=" + size + " stroke10=" + stroke10
                            + " dark=" + darkKind + " light=" + lightKind);
                }
            } finally {
                cur.close();
            }
        } catch (Throwable t) {
            XposedBridge.log("FpRing: config padrao (" + t + ")");
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (param.args.length == 0 || !(param.args[0] instanceof String)) return;
                    String name = (String) param.args[0];
                    name = name.substring(name.lastIndexOf('/') + 1);
                    if (!name.startsWith("ic_fingerprint_")) return;
                    refreshConfig();

                    boolean light = name.contains("light");
                    byte[] data;
                    String kind;
                    if (light && lightData != null) { data = lightData; kind = lightKind; }
                    else { data = darkData; kind = darkKind; }

                    byte[] out = null;
                    if (data != null) {
                        out = "png".equals(kind)
                                ? Icons.imageLottie(data, size).getBytes(StandardCharsets.UTF_8)
                                : data;
                    } else {
                        String json = Icons.get(name, size, stroke10);
                        if (json != null) out = json.getBytes(StandardCharsets.UTF_8);
                    }
                    if (out != null) param.setResult(new ByteArrayInputStream(out));
                } catch (Throwable t) {
                    XposedBridge.log("FpRing: " + t);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(AssetManager.class, "open", String.class, hook);
            XposedHelpers.findAndHookMethod(AssetManager.class, "open", String.class, int.class, hook);
            XposedBridge.log("FpRing: hook ativo em " + TARGET);
        } catch (Throwable t) {
            XposedBridge.log("FpRing: falha ao hookar " + t);
        }
    }
}
