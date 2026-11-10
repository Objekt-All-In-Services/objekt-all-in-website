package de.objektallin.services;

import android.content.ContentValues;
import android.database.Cursor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class Entry {
    public static final String JOB = "Auftrag", TASK = "Aufgabe", APPOINTMENT = "Termin";
    public static final String CLIENT = "Kunde", OFFER = "Angebot", INVOICE = "Rechnung";
    public static final String INCOME = "Einnahme", VEHICLE = "Fahrzeug", EQUIPMENT = "Gerät";
    public long id = 0;
    public String type = JOB, title = "", customer = "", details = "", status = "Offen";
    public String address = "", phone = "", owner = "", photoUris = "[]";
    public long amountCents = 0, paidCents = 0, dueDate = 0, updatedAt = 0;

    public ContentValues values() {
        ContentValues v = new ContentValues();
        v.put("type", type); v.put("title", title); v.put("customer", customer);
        v.put("details", details); v.put("status", status); v.put("address", address);
        v.put("phone", phone); v.put("owner", owner); v.put("photos", photoUris);
        v.put("amount_cents", amountCents); v.put("paid_cents", paidCents);
        v.put("due_date", dueDate); v.put("updated_at", System.currentTimeMillis());
        return v;
    }

    public static Entry fromCursor(Cursor c) {
        Entry e = new Entry();
        e.id = c.getLong(c.getColumnIndexOrThrow("id"));
        e.type = c.getString(c.getColumnIndexOrThrow("type"));
        e.title = c.getString(c.getColumnIndexOrThrow("title"));
        e.customer = c.getString(c.getColumnIndexOrThrow("customer"));
        e.details = c.getString(c.getColumnIndexOrThrow("details"));
        e.status = c.getString(c.getColumnIndexOrThrow("status"));
        e.address = c.getString(c.getColumnIndexOrThrow("address"));
        e.phone = c.getString(c.getColumnIndexOrThrow("phone"));
        e.owner = c.getString(c.getColumnIndexOrThrow("owner"));
        e.photoUris = c.getString(c.getColumnIndexOrThrow("photos"));
        e.amountCents = c.getLong(c.getColumnIndexOrThrow("amount_cents"));
        e.paidCents = c.getLong(c.getColumnIndexOrThrow("paid_cents"));
        e.dueDate = c.getLong(c.getColumnIndexOrThrow("due_date"));
        e.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return e;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("type",type); j.put("title",title); j.put("customer",customer);
        j.put("details",details); j.put("status",status); j.put("address",address);
        j.put("phone",phone); j.put("owner",owner); j.put("photos",new JSONArray(photoUris));
        j.put("amountCents",amountCents); j.put("paidCents",paidCents);
        j.put("dueDate",dueDate); j.put("updatedAt",updatedAt); return j;
    }
    public static Entry fromJson(JSONObject j) {
        Entry e = new Entry();
        e.type = j.optString("type", JOB); e.title=j.optString("title","");
        e.customer=j.optString("customer",""); e.details=j.optString("details","");
        e.status=j.optString("status","Offen"); e.address=j.optString("address","");
        e.phone=j.optString("phone",""); e.owner=j.optString("owner","");
        e.photoUris=j.optJSONArray("photos")!=null ? j.optJSONArray("photos").toString() : "[]";
        e.amountCents=j.optLong("amountCents",0); e.paidCents=j.optLong("paidCents",0);
        e.dueDate=j.optLong("dueDate",0); e.updatedAt=j.optLong("updatedAt",0);
        return e;
    }
}
