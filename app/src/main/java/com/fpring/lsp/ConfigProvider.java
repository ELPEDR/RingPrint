package com.fpring.lsp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/** Entrega configuracao e icones importados para o hook, que roda em outro processo. */
public class ConfigProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(MainActivity.PREFS, 0);
    }

    /** Colunas: size, stroke10, dark_kind, light_kind, ver */
    @Override
    public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
        SharedPreferences sp = prefs();
        MatrixCursor c = new MatrixCursor(new String[]{"size", "stroke10", "dark_kind", "light_kind", "ver"});
        c.addRow(new Object[]{
                sp.getInt("size", Icons.DEF_SIZE),
                sp.getInt("stroke10", Icons.DEF_STROKE10),
                sp.getString("dark_kind", ""),
                sp.getString("light_kind", ""),
                sp.getLong("ver", 0L)});
        return c;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String slot = uri.getLastPathSegment();
        if (!"dark".equals(slot) && !"light".equals(slot)) throw new FileNotFoundException();
        String kind = prefs().getString(slot + "_kind", "");
        File f = new File(getContext().getFilesDir(), "custom_" + slot + "." + kind);
        if (kind.isEmpty() || !f.exists()) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
