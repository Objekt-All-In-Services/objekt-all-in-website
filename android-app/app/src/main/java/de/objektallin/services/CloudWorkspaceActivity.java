package de.objektallin.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Calendar;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

/** Separater nativer Online-Arbeitsraum. Lokale v1.0-Daten bleiben unangetastet. */
public class CloudWorkspaceActivity extends Activity {
    private static final int NAVY=Color.rgb(7,30,56), GOLD=Color.rgb(210,163,78);
    private static final int BG=Color.rgb(244,247,251), MUTED=Color.rgb(102,118,132);
    private static final String[] TABS={"Übersicht","Aufträge","Termine","Finanzen","Mehr"};
    private static final String PREF="cloud_workspace", KEY_ALIAS="objekt_all_in_cloud_tokens_v1";
    private static final String[] KINDS={"Auftrag","Aufgabe","Termin","Kunde","Angebot","Rechnung","Einnahme","Fahrzeug","Gerät"};
    private static final String[] STATUSES={"Offen","Geplant","In Arbeit","Erledigt","Abgeschlossen","Bezahlt","Storniert"};
    private final ExecutorService network=Executors.newSingleThreadExecutor();
    private SharedPreferences pref;
    private String base="",pub="",access="",refresh="",userId="",kind="Auftrag";
    private JSONArray records=new JSONArray(),members=new JSONArray(),events=new JSONArray();
    private LinearLayout main;
    private boolean busy=false;
    private String tab="Übersicht", subCategory="", financeCategory="Rechnung";
    private long lastLoadAt=0L;

    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        pref=getSharedPreferences(PREF,MODE_PRIVATE);
        base="https://pqydohrjsuoxnzdoykgs.supabase.co";pub="sb_publishable_r6AKL_SLCbUI8Qw0U0GH8g_AUssip1T";
        readTokens();
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        if(access.isEmpty())loginScreen();else load();
    }
    @Override public void onDestroy(){network.shutdownNow();super.onDestroy();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(NAVY);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private void add(LinearLayout target,View child,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);target.addView(child,p);}
    private LinearLayout vertical(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private Button button(String label,Runnable click){Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextColor(Color.WHITE);b.setBackground(shape(NAVY,13));b.setOnClickListener(v->click.run());return b;}
    private EditText input(LinearLayout target,String caption,String value,boolean secure){
        add(target,text(caption,12,true),12);
        EditText e=new EditText(this);e.setText(value);e.setSingleLine(true);e.setTextSize(15);
        if(secure)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setBackground(shape(Color.rgb(240,246,250),9));e.setPadding(dp(13),dp(13),dp(13),dp(13));add(target,e,5);return e;
    }
    private GradientDrawable outlined(int color,int radius,int border){
        GradientDrawable g=shape(color,radius);
        g.setStroke(dp(1),border);
        return g;
    }
    private LinearLayout horizontal(){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }
    private void shell(String heading){
        LinearLayout root=vertical();root.setBackgroundColor(BG);
        root.setFocusableInTouchMode(true);
        setContentView(root);
        LinearLayout top=horizontal();
        top.setPadding(dp(16),dp(15),dp(16),dp(15));
        top.setBackgroundColor(NAVY);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.logo_original);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(70),dp(75));
        top.addView(logo,lp);
        LinearLayout titles=vertical();
        LinearLayout.LayoutParams labels=new LinearLayout.LayoutParams(0,-2,1);
        labels.leftMargin=dp(9);
        top.addView(titles,labels);
        TextView firm=text("OBJEKT ALL-IN",17,true);
        firm.setTextColor(Color.WHITE);
        firm.setLetterSpacing(.055f);
        titles.addView(firm);
        TextView sub=text("S E R V I C E S",11,true);
        sub.setTextColor(GOLD);
        add(titles,sub,2);
        if(!access.isEmpty()){
            String username=memberName(userId);
            TextView who=text("● "+username,12,true);
            who.setTextColor(Color.WHITE);
            who.setPadding(dp(11),dp(10),dp(11),dp(10));
            who.setBackground(shape(Color.rgb(21,62,92),28));
            top.addView(who);
        }
        root.addView(top);
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);
        main=vertical();
        main.setPadding(dp(17),dp(21),dp(17),dp(30));
        sc.addView(main);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        if(!access.isEmpty()){
            LinearLayout nav=horizontal();
            nav.setBackgroundColor(Color.WHITE);
            nav.setPadding(dp(5),dp(10),dp(5),dp(10));
            nav.setElevation(dp(6));
            for(String item:TABS){
                LinearLayout cell=vertical();
                cell.setGravity(Gravity.CENTER);
                int color=tab.equals(item)?NAVY:MUTED;
                TextView symbol=text(tabIcon(item),22,true);
                symbol.setTextColor(tab.equals(item)?GOLD:MUTED);
                symbol.setGravity(Gravity.CENTER);
                cell.addView(symbol);
                TextView label=text(item,11,tab.equals(item));
                label.setTextColor(color);
                label.setGravity(Gravity.CENTER);
                add(cell,label,4);
                nav.addView(cell,new LinearLayout.LayoutParams(0,dp(64),1));
                cell.setContentDescription("Bereich "+item);
                cell.setOnClickListener(v->{tab=item;subCategory="";render();});
            }
            root.addView(nav);
        }
    }
    private String tabIcon(String t){
        if("Übersicht".equals(t))return "⌂";
        if("Aufträge".equals(t))return "▣";
        if("Termine".equals(t))return "▤";
        if("Finanzen".equals(t))return "€";
        return "☰";
    }
    private void heading(String title,String sub){
        TextView h=text(title,27,true);
        h.setTextColor(NAVY);
        add(main,h,1);
        if(sub!=null && !sub.isEmpty()){
            TextView d=text(sub,14,false);
            d.setTextColor(MUTED);
            add(main,d,8);
        }
    }
    private LinearLayout card(){
        LinearLayout a=vertical();
        a.setPadding(dp(18),dp(17),dp(18),dp(17));
        a.setBackground(outlined(Color.WHITE,16,Color.rgb(221,228,236)));
        return a;
    }
    private void cardLink(String title,String subtitle,Runnable click){
        LinearLayout row=card();
        TextView head=text(title+"   ›",17,true);
        head.setTextColor(NAVY);
        add(row,head,0);
        if(subtitle!=null && !subtitle.isEmpty()){
            TextView d=text(subtitle,12,false);
            d.setTextColor(MUTED);
            add(row,d,6);
        }
        row.setOnClickListener(v->click.run());
        add(main,row,11);
    }
    private Button goldButton(String label,Runnable click){
        Button b=button(label,click);
        b.setTextColor(NAVY);
        b.setBackground(shape(GOLD,15));
        return b;
    }
    
    private String decrypt(String encrypted){
        try {
            if(encrypted.isEmpty())return "";
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
            SecretKey key=(SecretKey)ks.getKey(KEY_ALIAS,null);
            String[] parts=encrypted.split(":");if(parts.length!=2||key==null)return "";
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
            return new String(c.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
        }catch(Exception ignored){return "";}
    }
    private String encrypt(String data) throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias(KEY_ALIAS)){
            KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
            kg.generateKey();
        }
        SecretKey key=(SecretKey)ks.getKey(KEY_ALIAS,null);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key);
        return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(data.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
    }
    private void readTokens(){access=decrypt(pref.getString("access", ""));refresh=decrypt(pref.getString("refresh",""));userId=pref.getString("userId","");}
    private void saveTokens(JSONObject response) throws Exception {
        access=response.getString("access_token");refresh=response.getString("refresh_token");
        JSONObject user=response.optJSONObject("user");if(user!=null)userId=user.optString("id",userId);
        pref.edit().putString("access",encrypt(access)).putString("refresh",encrypt(refresh)).putString("userId",userId).apply();
    }
    private void clear(){access="";refresh="";userId="";pref.edit().remove("access").remove("refresh").remove("userId").apply();}
    private interface Work<T>{T run() throws Exception;}
    private interface Result<T>{void receive(T value);}
    private <T> void launch(Work<T> task,Result<T> done){
        if(busy)return;busy=true;
        network.execute(()->{
            try {T result=task.run();runOnUiThread(()->{busy=false;if(!isFinishing())done.receive(result);});}
            catch(Exception ex){String message=ex.getMessage();runOnUiThread(()->{busy=false;if(!isFinishing()){
                if(access.isEmpty())loginScreen();
                new AlertDialog.Builder(this).setTitle("Verbindung / Berechtigung").setMessage(message).setPositiveButton("OK",null).show();
            }});}
        });
    }
    private String http(String path,String method,JSONObject payload,boolean authenticated) throws Exception {
        return http(path,method,payload,authenticated,true);
    }
    private String http(String path,String method,JSONObject payload,boolean authenticated,boolean retry) throws Exception {
        URL url=new URL(base+path);
        if(!"https".equalsIgnoreCase(url.getProtocol()))throw new Exception("Nur HTTPS ist erlaubt.");
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(18000);c.setRequestMethod(method);
        c.setRequestProperty("apikey",pub);c.setRequestProperty("Content-Type","application/json");
        if(authenticated)c.setRequestProperty("Authorization","Bearer "+access);
        if("POST".equals(method)||"PATCH".equals(method))c.setRequestProperty("Prefer","return=representation");
        if(payload!=null){c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}}
        int status=c.getResponseCode();InputStream stream=status<400?c.getInputStream():c.getErrorStream();
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        if(stream!=null){byte[] buf=new byte[8192];int n;while((n=stream.read(buf))!=-1)bytes.write(buf,0,n);stream.close();}
        byte[] data=bytes.toByteArray();c.disconnect();
        String answer=new String(data,StandardCharsets.UTF_8);
        if(status==401 && authenticated && retry && !refresh.isEmpty()){
            JSONObject request=new JSONObject();request.put("refresh_token",refresh);
            try {
                JSONObject newer=new JSONObject(http("/auth/v1/token?grant_type=refresh_token","POST",request,false,false));
                saveTokens(newer);
                return http(path,method,payload,true,false);
            } catch(Exception renewalError) {
                clear();
                throw new Exception("Deine Sitzung ist abgelaufen. Melde dich bitte erneut mit deiner E-Mail-Adresse und deinem Passwort an.");
            }
        }
        if(status>=400)throw new Exception("Serverfehler "+status+": "+answer.substring(0,Math.min(200,answer.length())));
        return answer;
    }
    private void loginScreen(){
        shell("Anmelden");
        heading("Willkommen zurück","Melde dich mit deinem persönlichen Firmen-Zugang an. Ein lokaler Profilwechsel ist nicht möglich.");
        LinearLayout form=card();
        TextView note=text("OBJEKT ALL-IN TEAM · SICHERE ANMELDUNG",12,true);
        note.setTextColor(Color.rgb(143,104,46));
        add(form,note,0);
        EditText mail=input(form,"Deine E-Mail-Adresse","",false);
        mail.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mail.setHint("name@beispiel.de");
        EditText pass=input(form,"Dein Passwort","",true);
        pass.setHint("Passwort eingeben");
        add(form,goldButton("Sicher anmelden",()->{
            String email=mail.getText().toString().trim();
            String password=pass.getText().toString();
            if(email.isEmpty()||password.isEmpty()){
                message("Bitte E-Mail-Adresse und Passwort eingeben.");
                return;
            }
            launch(()->{
                JSONObject request=new JSONObject();
                request.put("email",email);
                request.put("password",password);
                JSONObject reply=new JSONObject(http("/auth/v1/token?grant_type=password","POST",request,false));
                saveTokens(reply);
                JSONArray list=new JSONArray(http("/rest/v1/company_members?select=user_id,display_name,role,active","GET",null,true));
                boolean member=false;
                for(int i=0;i<list.length();i++){
                    JSONObject x=list.optJSONObject(i);
                    if(x!=null && userId.equalsIgnoreCase(x.optString("user_id")) && x.optBoolean("active"))member=true;
                }
                if(!member){
                    clear();
                    throw new Exception("Dieses Benutzerkonto ist nicht für Objekt All-In Services freigeschaltet.");
                }
                return true;
            },unused->{pass.setText("");tab="Übersicht";load();});
        }),18);
        add(form,text("Du bleibst auf diesem Gerät angemeldet, bis du dich selbst abmeldest oder deine Sitzung abläuft.",12,false),10);
        add(main,form,18);
        add(main,text("Verwende dein eigenes Passwort. Änderungen werden deinem bestätigten Benutzerkonto zugeordnet.",13,false),17);
    }
    
    private void message(String m){new AlertDialog.Builder(this).setMessage(m).setPositiveButton("OK",null).show();}
    private void load(){
        if(access.isEmpty()){loginScreen();return;}
        shell("Synchronisierung");
        heading("Daten werden geladen","Deine gemeinsamen Firmendaten werden sicher abgerufen.");
        launch(()->{
            JSONArray foundMembers=new JSONArray(http("/rest/v1/company_members?select=user_id,display_name,role,active&order=display_name.asc","GET",null,true));
            boolean approved=false;
            for(int i=0;i<foundMembers.length();i++){
                JSONObject m=foundMembers.optJSONObject(i);
                if(m!=null && userId.equalsIgnoreCase(m.optString("user_id")) && m.optBoolean("active"))approved=true;
            }
            if(!approved){clear();throw new Exception("Dein Firmenzugang ist nicht mehr aktiv.");}
            JSONArray foundRecords=new JSONArray(http("/rest/v1/records?select=*&order=updated_at.desc&limit=300","GET",null,true));
            JSONArray foundEvents=new JSONArray(http("/rest/v1/audit_events?select=id,record_id,record_title,record_kind,actor_id,action,changed_at&order=changed_at.desc&limit=150","GET",null,true));
            members=foundMembers;records=foundRecords;events=foundEvents;
            lastLoadAt=System.currentTimeMillis();
            return true;
        },ok->render());
    }
    @Override protected void onResume(){
        super.onResume();
        if(!access.isEmpty() && lastLoadAt>0 && !busy &&
            System.currentTimeMillis()-lastLoadAt>60000L)load();
    }
    
    private String memberName(String id){
        for(int i=0;i<members.length();i++){JSONObject j=members.optJSONObject(i);if(j!=null&&id.equalsIgnoreCase(j.optString("user_id")))return j.optString("display_name","Teammitglied");}
        return "Nicht zugewiesen";
    }
    private void render(){
        if(access.isEmpty()){loginScreen();return;}
        shell(tab);
        if(tab.equals("Übersicht")){renderOverview();return;}
        if(tab.equals("Aufträge")){showRecords("Aufträge","Auftrag","Alle Kundenaufträge mit Status und Zuständigkeit.");return;}
        if(tab.equals("Termine")){showRecords("Termine","Termin","Einsätze und Termine im Blick behalten.");return;}
        if(tab.equals("Finanzen")){renderFinance();return;}
        renderMore();
    }
    private int countKind(String category){
        int total=0;
        for(int i=0;i<records.length();i++){
            JSONObject j=records.optJSONObject(i);
            if(j!=null&&category.equals(j.optString("kind")))total++;
        }
        return total;
    }
    private long sum(String category,String field){
        long total=0;
        for(int i=0;i<records.length();i++){
            JSONObject j=records.optJSONObject(i);
            if(j!=null&&category.equals(j.optString("kind")))total+=j.optLong(field,0);
        }
        return total;
    }
    private String money(long cents){
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.GERMANY).format(cents/100d);
    }
    private void metric(LinearLayout box,String label,String value){
        TextView cap=text(label,12,true);cap.setTextColor(MUTED);add(box,cap,0);
        TextView val=text(value,25,true);val.setTextColor(NAVY);add(box,val,6);
    }
    private void renderOverview(){
        heading("Startübersicht","Willkommen, "+memberName(userId)+". Alle gemeinsamen Firmendaten an einem Ort.");
        LinearLayout a=horizontal();
        LinearLayout left=card();metric(left,"Aufträge",String.valueOf(countKind("Auftrag")));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
        a.addView(left,lp);
        LinearLayout right=card();metric(right,"Termine",String.valueOf(countKind("Termin")));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);
        rp.leftMargin=dp(10);a.addView(right,rp);
        add(main,a,23);
        LinearLayout fin=card();
        metric(fin,"Offene Rechnungsbeträge",money(Math.max(0,sum("Rechnung","amount_cents")-sum("Rechnung","paid_cents"))));
        add(main,fin,11);
        TextView title=text("Schnellzugriff",19,true);title.setTextColor(NAVY);add(main,title,24);
        cardLink("+ Auftrag erfassen","Ein neuer Auftrag für das gemeinsame Team",()->{kind="Auftrag";editor(null);});
        cardLink("Aufträge ansehen",countKind("Auftrag")+" Aufträge gespeichert",()->{tab="Aufträge";render();});
        cardLink("Kunden & Adressen",countKind("Kunde")+" Kunden gespeichert",()->{tab="Mehr";subCategory="Kunde";render();});
        cardLink("Änderungsprotokoll","Nachsehen, wer welchen Eintrag geändert hat",this::history);
        add(main,text("Änderungen von Silvan und Lion werden nach dem Speichern protokolliert. Zum Abrufen neuer Änderungen nach unten über „Aktualisieren“ oder beim erneuten Öffnen synchronisieren.",12,false),18);
        add(main,button("⟳ Daten aktualisieren",this::load),14);
    }
    private void showRecords(String title,String category,String subtitle){
        kind=category;
        heading(title,subtitle);
        add(main,goldButton("+ "+(category.equals("Auftrag")?"Neuen Auftrag":category.equals("Termin")?"Neuen Termin":"Neuen Eintrag")+" erstellen",()->editor(null)),18);
        int total=0;
        for(int i=0;i<records.length();i++){
            JSONObject e=records.optJSONObject(i);
            if(e==null||!category.equals(e.optString("kind")))continue;
            total++;
            LinearLayout row=card();
            TextView name=text(e.optString("title","Ohne Titel"),17,true);
            name.setTextColor(NAVY);add(row,name,0);
            String customer=e.optString("customer","");
            String status=e.optString("status","Offen");
            String due=e.optString("due_date","");
            String info=(customer.isEmpty()?"":customer+" · ")+status+(due.isEmpty()||due.equals("null")?"":" · "+displayDate(due));
            TextView line=text(info,13,false);line.setTextColor(MUTED);add(row,line,7);
            TextView owner=text("Zuständig: "+memberName(e.optString("assigned_to",""))+"  |  Bearbeitet: "+memberName(e.optString("updated_by","")),12,false);
            owner.setTextColor(MUTED);add(row,owner,8);
            row.setOnClickListener(v->editor(e));
            add(main,row,12);
        }
        if(total==0){
            LinearLayout empty=card();
            add(empty,text("Noch keine Einträge in diesem Bereich.",15,false),0);
            add(main,empty,18);
        }
        add(main,button("⟳ Aktualisieren",this::load),18);
    }
    private void financePick(String category){
        financeCategory=category;tab="Finanzen";render();
    }
    private void renderFinance(){
        heading("Finanzen","Angebote, Rechnungen und Einnahmen im Überblick.");
        LinearLayout balance=card();
        metric(balance,"Offene Rechnungsbeträge",money(Math.max(0,sum("Rechnung","amount_cents")-sum("Rechnung","paid_cents"))));
        add(main,balance,17);
        cardLink("Angebote · "+countKind("Angebot"),"Erfasste Angebote anzeigen",()->financePick("Angebot"));
        cardLink("Rechnungen · "+countKind("Rechnung"),"Rechnungen und Teilzahlungen anzeigen",()->financePick("Rechnung"));
        cardLink("Einnahmen · "+countKind("Einnahme"),"Erfasste Einnahmen anzeigen",()->financePick("Einnahme"));
        showRecords(financeCategory,financeCategory,"Ausgewählt: "+financeCategory);
    }
    private void morePick(String category){
        tab="Mehr";subCategory=category;render();
    }
    private void renderMore(){
        heading("Weitere Bereiche","Gemeinsame Verwaltung für euren Firmenalltag.");
        cardLink("Aufgaben & Checklisten",countKind("Aufgabe")+" Einträge",()->morePick("Aufgabe"));
        cardLink("Kunden & Adressen",countKind("Kunde")+" Einträge",()->morePick("Kunde"));
        cardLink("Fuhrpark & Fahrzeuge",countKind("Fahrzeug")+" Einträge",()->morePick("Fahrzeug"));
        cardLink("Geräte & Maschinen",countKind("Gerät")+" Einträge",()->morePick("Gerät"));
        cardLink("Änderungsprotokoll","Wer hat wann welchen Eintrag geändert?",this::history);
        if(!subCategory.isEmpty())showRecords(subCategory,subCategory,"Gemeinsamer Team-Bereich");
        TextView me=text("Dein Konto: "+memberName(userId),15,true);me.setTextColor(NAVY);add(main,me,23);
        add(main,text("Die Anmeldung bleibt auf diesem Gerät gespeichert. Abmelden beendet deine Sitzung.",12,false),7);
        add(main,button("Abmelden",()->new AlertDialog.Builder(this).setTitle("Abmelden").setMessage("Möchtest du dich wirklich abmelden?")
            .setPositiveButton("Abmelden",(d,w)->{clear();members=new JSONArray();records=new JSONArray();events=new JSONArray();loginScreen();})
            .setNegativeButton("Abbrechen",null).show()),15);
    }
    
    private static final DateTimeFormatter DATE_DDMMYYYY =
        DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY)
            .withResolverStyle(ResolverStyle.STRICT);

    private String displayDate(String iso) {
        if(iso==null || iso.trim().isEmpty() || "null".equalsIgnoreCase(iso))return "";
        try { return LocalDate.parse(iso).format(DATE_DDMMYYYY); }
        catch(Exception badDate){ return ""; }
    }

    private void chooseDate(EditText field){
        LocalDate initial=LocalDate.now();
        String selected=field.getText().toString().trim();
        if(!selected.isEmpty()){
            try { initial=LocalDate.parse(selected,DATE_DDMMYYYY); }
            catch(Exception ignored){ /* Startet sonst beim heutigen Datum. */ }
        }
        DatePickerDialog calendar=new DatePickerDialog(this, null,
            initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth());
        calendar.setTitle("Termin auswählen");
        calendar.getDatePicker().setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setButton(DialogInterface.BUTTON_POSITIVE,"Speichern",(dialog,which)->{
            android.widget.DatePicker picker=calendar.getDatePicker();
            field.setText(String.format(Locale.GERMANY,"%02d.%02d.%04d",
                picker.getDayOfMonth(),picker.getMonth()+1,picker.getYear()));
        });
        calendar.setButton(DialogInterface.BUTTON_NEGATIVE,"Abbrechen",(dialog,which)->{});
        calendar.show();
    }

    private void editor(JSONObject current){
        LinearLayout layout=vertical();layout.setPadding(dp(18),dp(5),dp(18),dp(5));
        EditText title=input(layout,"Titel *",current==null?"":current.optString("title"),false);
        EditText customer=input(layout,"Kunde",current==null?"":current.optString("customer"),false);
        EditText address=input(layout,"Adresse",current==null?"":current.optString("address"),false);
        EditText phone=input(layout,"Telefon",current==null?"":current.optString("phone"),false);
        EditText details=input(layout,"Beschreibung",current==null?"":current.optString("details"),false);details.setSingleLine(false);details.setMinLines(3);
        String existingDate=current==null || current.isNull("due_date") ? "" : current.optString("due_date","");
        EditText due=input(layout,"Termin · Kalender öffnen (TT.MM.JJJJ)",displayDate(existingDate),false);
        due.setHint("📅 Hier tippen, um Termin auszuwählen");
        due.setFocusable(false);
        due.setClickable(true);
        due.setOnClickListener(v->chooseDate(due));
        TextView removeDate=text("Termin entfernen",12,false);
        removeDate.setTextColor(Color.rgb(125,87,35));
        removeDate.setPadding(dp(4),dp(12),dp(4),dp(5));
        removeDate.setOnClickListener(v->due.setText(""));
        add(layout,removeDate,0);
        EditText amount=input(layout,"Betrag in EUR",current==null?"0":String.format(java.util.Locale.US,"%.2f",current.optLong("amount_cents")/100.0),false);
        EditText paid=input(layout,"Bezahlt in EUR",current==null?"0":String.format(java.util.Locale.US,"%.2f",current.optLong("paid_cents")/100.0),false);
        add(layout,text("Status",12,true),12);
        Spinner statuses=new Spinner(this);statuses.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,STATUSES));
        String chosen=current==null?"Offen":current.optString("status");for(int i=0;i<STATUSES.length;i++)if(STATUSES[i].equals(chosen))statuses.setSelection(i);add(layout,statuses,3);
        add(layout,text("Zuständig",12,true),12);
        String[] memberNames=new String[members.length()+1],ids=new String[members.length()+1];memberNames[0]="Nicht zugewiesen";ids[0]="";
        for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);memberNames[i+1]=m.optString("display_name");ids[i+1]=m.optString("user_id");}
        Spinner assigned=new Spinner(this);assigned.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,memberNames));
        String selected=current==null?"":current.optString("assigned_to","");for(int i=0;i<ids.length;i++)if(ids[i].equalsIgnoreCase(selected))assigned.setSelection(i);
        add(layout,assigned,4);
        ScrollView scroll=new ScrollView(this);scroll.addView(layout);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(current==null?"Neuer Eintrag":"Eintrag bearbeiten").setView(scroll)
            .setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",null).create();
        dialog.setOnShowListener(unused->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String ttl=title.getText().toString().trim();if(ttl.isEmpty()){title.setError("Titel erforderlich");return;}
            String visibleDate=due.getText().toString().trim();
            String dt="";
            if(!visibleDate.isEmpty()){
                try { dt=LocalDate.parse(visibleDate,DATE_DDMMYYYY).toString(); }
                catch(Exception invalidDate){
                    message("Ungültiger Termin. Bitte das Datum über den Kalender auswählen.");
                    return;
                }
            }
            JSONObject payload=new JSONObject();try{
                payload.put("title",ttl);payload.put("kind",current==null?kind:current.optString("kind",kind));
                payload.put("customer",customer.getText().toString());payload.put("address",address.getText().toString());
                payload.put("phone",phone.getText().toString());payload.put("details",details.getText().toString());
                payload.put("status",statuses.getSelectedItem().toString());
                payload.put("due_date",dt.isEmpty()?JSONObject.NULL:dt);
                String uid=ids[assigned.getSelectedItemPosition()];payload.put("assigned_to",uid.isEmpty()?JSONObject.NULL:uid);
                payload.put("amount_cents",Math.round(Double.parseDouble(amount.getText().toString().replace(",","."))*100));
                payload.put("paid_cents",Math.round(Double.parseDouble(paid.getText().toString().replace(",","."))*100));
            }catch(Exception e){message("Bitte gültige Beträge eingeben.");return;}
            launch(()->{
                if(current==null){
                    http("/rest/v1/records","POST",payload,true);
                }else{
                    JSONObject rpc=new JSONObject();
                    rpc.put("p_id",current.getString("id"));rpc.put("p_values",payload);
                    http("/rest/v1/rpc/update_record","POST",rpc,true);
                }
                return true;
            },ok->{dialog.dismiss();load();});
        }));dialog.show();
    }
    private void history(){
        shell("Änderungsprotokoll");
        heading("Wer hat was geändert?","Alle Änderungen werden über die bestätigte Anmeldung erfasst.");
        add(main,goldButton("← Zurück zur Übersicht",()->{tab="Übersicht";render();}),15);
        if(events.length()==0)add(main,text("Noch keine Änderungen vorhanden.",15,false),19);
        for(int i=0;i<events.length();i++){
            JSONObject e=events.optJSONObject(i);if(e==null)continue;
            String action=e.optString("action");
            String german=action.equals("created")?"erstellt":action.equals("deleted")?"gelöscht":"geändert";
            LinearLayout row=card();
            add(row,text(e.optString("record_title")+" · "+german,15,true),0);
            String date=e.optString("changed_at","").replace("T"," ");
            if(date.length()>16)date=date.substring(0,16);
            add(row,text("Von "+memberName(e.optString("actor_id"))+" · "+date,12,false),7);
            add(main,row,12);
        }
        add(main,button("⟳ Änderungen aktualisieren",this::load),21);
    }
    
}
