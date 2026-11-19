package de.objektallin.services;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class AppDatabase extends SQLiteOpenHelper {
    public AppDatabase(Context context) { super(context, "objekt_all_in.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entries("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "type TEXT NOT NULL,title TEXT NOT NULL,customer TEXT NOT NULL DEFAULT '',"+
            "details TEXT NOT NULL DEFAULT '',status TEXT NOT NULL DEFAULT 'Offen',"+
            "address TEXT NOT NULL DEFAULT '',phone TEXT NOT NULL DEFAULT '',"+
            "owner TEXT NOT NULL DEFAULT '',photos TEXT NOT NULL DEFAULT '[]',"+
            "amount_cents INTEGER NOT NULL DEFAULT 0,paid_cents INTEGER NOT NULL DEFAULT 0,"+
            "due_date INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_entries_type ON entries(type)");
        db.execSQL("CREATE INDEX idx_entries_date ON entries(due_date)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { /* Schema version 1 */ }

    public long save(Entry e) {
        SQLiteDatabase db=getWritableDatabase();
        if(e.id>0){db.update("entries",e.values(),"id=?",new String[]{String.valueOf(e.id)});return e.id;}
        return db.insertOrThrow("entries",null,e.values());
    }
    public void delete(long id) {getWritableDatabase().delete("entries","id=?",new String[]{String.valueOf(id)});}
    public Entry get(long id) {
        try(Cursor c=getReadableDatabase().query("entries",null,"id=?",new String[]{String.valueOf(id)},null,null,null)){
            return c.moveToFirst()?Entry.fromCursor(c):null;
        }
    }
    public List<Entry> all() {
        List<Entry> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("entries",null,null,null,null,null,"CASE WHEN due_date>0 THEN 0 ELSE 1 END, due_date ASC, updated_at DESC")) {
            while(c.moveToNext()) out.add(Entry.fromCursor(c));
        }
        return out;
    }
    public List<Entry> byType(String type) {
        List<Entry> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("entries",null,"type=?",new String[]{type},null,null,"CASE WHEN due_date>0 THEN 0 ELSE 1 END, due_date ASC, updated_at DESC")) {
            while(c.moveToNext())out.add(Entry.fromCursor(c));
        }
        return out;
    }
    public String exportJson() throws Exception {
        JSONObject root=new JSONObject();root.put("format","objekt-all-in-backup-v1");
        root.put("exportedAt",System.currentTimeMillis());JSONArray items=new JSONArray();
        for(Entry e:all())items.put(e.toJson());root.put("entries",items);return root.toString(2);
    }
    public int importReplacing(String json) throws Exception {
        JSONObject root=new JSONObject(json);
        if(!"objekt-all-in-backup-v1".equals(root.optString("format")))throw new IllegalArgumentException("Das ist keine gültige Objekt All-In Sicherungsdatei.");
        JSONArray entries=root.getJSONArray("entries");if(entries.length()>100000)throw new IllegalArgumentException("Zu viele Einträge.");
        List<Entry> parsed=new ArrayList<>();for(int i=0;i<entries.length();i++)parsed.add(Entry.fromJson(entries.getJSONObject(i)));
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            db.delete("entries",null,null);
            for(Entry e:parsed) db.insertOrThrow("entries",null,e.values());
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return parsed.size();
    }
}
