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
import android.widget.CalendarView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.Toast;
import android.widget.CheckBox;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
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
    private static final int PICK_VEHICLE_IMAGE=5013;
    private static final String DOC_BUCKET="objekt-fahrzeugakten";
    private String selectedVehicleId="",selectedVehicleTitle="";
    private JSONArray vehicleDocuments=new JSONArray();
    private boolean vehicleDocumentsOpen=false;
    private static final String[] TABS={"Übersicht","Aufträge","Termine","Finanzen","Mehr"};
    private static final String PREF="cloud_workspace", KEY_ALIAS="objekt_all_in_cloud_tokens_v1";
    private static final String[] KINDS={"Auftrag","Aufgabe","Termin","Kunde","Angebot","Rechnung","Einnahme","Ausgabe","Fahrzeug","Gerät"};
    private static final String[] STATUSES={"Offen","Geplant","In Arbeit","Erledigt","Abgeschlossen","Bezahlt","Storniert"};
    private final ExecutorService network=Executors.newSingleThreadExecutor();
    private SharedPreferences pref;
    private String base="",pub="",access="",refresh="",userId="",kind="Auftrag";
    private JSONArray records=new JSONArray(),members=new JSONArray(),events=new JSONArray();
    private LinearLayout main;
    private boolean busy=false;
    private String tab="Übersicht", detailKind="", selectedCalendarDate="";
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
                cell.setOnClickListener(v->{tab=item;detailKind="";vehicleDocumentsOpen=false;selectedCalendarDate="";render();});
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
        },ok->{if(vehicleDocumentsOpen)loadVehicleDocuments();else render();});
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
        if(!detailKind.isEmpty()){
            if(detailKind.equals("Finanzübersicht")){shell("Finanzübersicht");renderFinanceSummary();return;}
            if(detailKind.equals("Änderungsprotokoll")){history();return;}
            shell(detailKind);
            add(main,button("← Zurück",()->{detailKind="";render();}),0);
            showRecords(detailKind,detailKind,subtitleForKind(detailKind));
            return;
        }
        shell(tab);
        if(tab.equals("Übersicht")){renderOverview();return;}
        if(tab.equals("Aufträge")){showRecords("Aufträge","Auftrag","Alle Kundenaufträge mit Status und Zuständigkeit.");return;}
        if(tab.equals("Termine")){renderCalendar();return;}
        if(tab.equals("Finanzen")){renderFinance();return;}
        renderMore();
    }
    private String subtitleForKind(String category){
        if(category.equals("Fahrzeug"))return "Fahrzeuge mit Kennzeichen, TÜV und Fahrzeugscheinfotos.";
        if(category.equals("Kunde"))return "Kunden und Kontaktdaten gemeinsam verwalten.";
        if(category.equals("Aufgabe"))return "Aufgaben planen, zuweisen und abhaken.";
        if(category.equals("Gerät"))return "Geräte, Maschinen und Wartungen verwalten.";
        if(category.equals("Ausgabe"))return "Betriebsausgaben mit Betrag erfassen.";
        return "Gemeinsame Einträge erstellen und bearbeiten.";
    }
    private void openCategory(String category){
        detailKind=category;render();
    }
    private String assignmentName(JSONObject record){
        if(record.optBoolean("assigned_both",false))return "Silvan und Lion (beide)";
        return memberName(record.optString("assigned_to",""));
    }
    private Set<String> invoicedOrderIds(){
        Set<String> ids=new HashSet<>();
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);
            if(r!=null&&"Rechnung".equals(r.optString("kind"))){
                String link=r.optString("linked_order_id","");
                if(!link.isEmpty()&&!link.equals("null"))ids.add(link);
            }
        }
        return ids;
    }
    private JSONObject recordById(String id){
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);
            if(r!=null&&id.equalsIgnoreCase(r.optString("id")))return r;
        }
        return null;
    }
    private long pendingCents(){
        long due=0;
        Set<String> invoiced=invoicedOrderIds();
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);
            if(r==null)continue;
            String kind=r.optString("kind"), status=r.optString("status");
            long outstanding=Math.max(0,r.optLong("amount_cents")-r.optLong("paid_cents"));
            if(kind.equals("Auftrag") && status.equals("Abgeschlossen")
               && !invoiced.contains(r.optString("id")))due+=outstanding;
            if(kind.equals("Rechnung") && !status.equals("Bezahlt")){
                String linked=r.optString("linked_order_id","");
                if(linked.isEmpty()||linked.equals("null")){
                    due+=outstanding;
                }else{
                    JSONObject order=recordById(linked);
                    if(order!=null && order.optString("status").equals("Abgeschlossen"))due+=outstanding;
                }
            }
        }
        return due;
    }
    private long incomeCents(){
        long sum=0;
        Set<String> invoiced=invoicedOrderIds();
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);
            if(r==null)continue;
            String type=r.optString("kind"),linked=r.optString("linked_order_id","");
            if(type.equals("Auftrag")){
                // Zahlungen aus Aufträgen, soweit keine eigene Rechnung die Zahlungen führt.
                if(!invoiced.contains(r.optString("id"))){
                    long paid=r.optLong("paid_cents");
                    if("Bezahlt".equals(r.optString("status")))paid=r.optLong("amount_cents");
                    sum+=Math.min(Math.max(0,paid),Math.max(0,r.optLong("amount_cents")));
                }
            }else if(type.equals("Rechnung")){
                sum+=Math.min(Math.max(0,r.optLong("paid_cents")),Math.max(0,r.optLong("amount_cents")));
            }else if(type.equals("Einnahme") && (linked.isEmpty()||linked.equals("null"))){
                sum+=Math.max(0,r.optLong("amount_cents"));
            }
        }
        return sum;
    }
    private long expensesCents(){return Math.max(0,sum("Ausgabe","amount_cents"));}
    
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
        LinearLayout row=horizontal();
        LinearLayout jobs=card();metric(jobs,"Aufträge",String.valueOf(countKind("Auftrag")));
        row.addView(jobs,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout appointments=card();metric(appointments,"Termine",String.valueOf(countKind("Termin")));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);
        rp.leftMargin=dp(10);row.addView(appointments,rp);
        add(main,row,23);
        TextView head=text("Schnellzugriff",19,true);head.setTextColor(NAVY);add(main,head,25);
        cardLink("+ Auftrag erfassen","Einen neuen Kundenauftrag anlegen",()->{kind="Auftrag";editor(null);});
        cardLink("Aufträge ansehen","Aufträge und Zuständigkeiten",()->{tab="Aufträge";detailKind="";render();});
        add(main,button("⟳ Aktualisieren",this::load),21);
    }
    private void renderCalendar(){
        heading("Termine","Alle gemeinsamen Termine im Monatskalender.");
        CalendarView calendar=new CalendarView(this);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setBackground(shape(Color.WHITE,15));
        if(!selectedCalendarDate.isEmpty()){
            try{
                LocalDate date=LocalDate.parse(selectedCalendarDate);
                Calendar cal=Calendar.getInstance();
                cal.set(date.getYear(),date.getMonthValue()-1,date.getDayOfMonth(),12,0);
                calendar.setDate(cal.getTimeInMillis(),false,false);
            }catch(Exception ignored){}
        }
        calendar.setOnDateChangeListener((view,year,month,day)->{
            selectedCalendarDate=String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day);
            render();
        });
        add(main,calendar,17);
        add(main,goldButton("+ Neuen Termin erstellen",()->{kind="Termin";editor(null);}),17);
        if(!selectedCalendarDate.isEmpty()){
            TextView label=text("Am "+displayDate(selectedCalendarDate),17,true);
            add(main,label,21);
        }else add(main,text("Alle Termine",17,true),21);
        int n=0;
        for(int i=0;i<records.length();i++){
            JSONObject e=records.optJSONObject(i);
            if(e==null||!"Termin".equals(e.optString("kind")))continue;
            if(!selectedCalendarDate.isEmpty()&&!selectedCalendarDate.equals(e.optString("due_date")))continue;
            recordCard(e);
            n++;
        }
        if(n==0)add(main,text("Keine Termine für diese Auswahl.",14,false),16);
        if(!selectedCalendarDate.isEmpty())
            add(main,button("Alle Termine anzeigen",()->{selectedCalendarDate="";render();}),13);
    }
    
    private void showRecords(String title,String category,String subtitle){
        kind=category;
        heading(title,subtitle);
        add(main,goldButton("+ Neuen Eintrag erstellen",()->editor(null)),18);
        int count=0;
        for(int i=0;i<records.length();i++){
            JSONObject entry=records.optJSONObject(i);
            if(entry!=null&&category.equals(entry.optString("kind"))){recordCard(entry);count++;}
        }
        if(count==0)add(main,text("Noch keine Einträge. Lege den ersten an.",14,false),19);
        add(main,button("⟳ Aktualisieren",this::load),18);
    }
    private void recordCard(JSONObject entry){
        LinearLayout row=card();
        TextView name=text(entry.optString("title","Ohne Titel"),17,true);
        add(row,name,0);
        String category=entry.optString("kind");
        String status=entry.optString("status","");
        String due=displayDate(entry.optString("due_date",""));
        String customer=entry.optString("customer","");
        String summary=(customer.isEmpty()?"":customer+" · ")+status+(due.isEmpty()?"":" · "+due);
        TextView line=text(summary,13,false);line.setTextColor(MUTED);add(row,line,7);
        if(category.equals("Auftrag")||category.equals("Termin")||category.equals("Aufgabe")){
            TextView who=text("Zuständig: "+assignmentName(entry),12,false);
            who.setTextColor(MUTED);add(row,who,7);
        }
        if(category.equals("Auftrag")||category.equals("Rechnung")||category.equals("Einnahme")||category.equals("Ausgabe")){
            TextView amount=text(money(entry.optLong("amount_cents")),15,true);
            add(row,amount,7);
        }
        if(category.equals("Fahrzeug")){
            JSONObject data=entry.optJSONObject("metadata");
            if(data!=null){TextView plate=text("Kennzeichen: "+data.optString("kennzeichen","–")+"  ·  HU: "+displayDate(data.optString("hu_date","")),13,false);
                add(row,plate,7);}
            row.setOnClickListener(v->vehicleDetail(entry));
        }else if(category.equals("Aufgabe")){
            row.setOnClickListener(v->taskDetail(entry));
        }else row.setOnClickListener(v->editor(entry));
        add(main,row,11);
    }
    private void financePick(String category){detailKind=category;render();}
    private void renderFinance(){
        heading("Finanzen","Wähle einen Bereich, um alle Einträge auf einer eigenen Seite zu sehen.");
        cardLink("Angebote · "+countKind("Angebot"),"Angebote erfassen und bearbeiten",()->financePick("Angebot"));
        cardLink("Rechnungen · "+countKind("Rechnung"),"Rechnungen und Teilzahlungen anzeigen",()->financePick("Rechnung"));
        cardLink("Einnahmen · "+countKind("Einnahme"),"Einnahmen verwalten",()->financePick("Einnahme"));
        cardLink("Ausgaben · "+countKind("Ausgabe"),"Kosten und Ausgaben eintragen",()->financePick("Ausgabe"));
    }
    private void renderFinanceSummary(){
        add(main,button("← Zurück zu Mehr",()->{detailKind="";tab="Mehr";render();}),0);
        heading("Finanzübersicht","Zusammenfassung der erfassten Zahlungen und offenen Forderungen.");
        LinearLayout earnings=card();metric(earnings,"Gesamteinnahmen",money(incomeCents()));add(main,earnings,16);
        LinearLayout expenses=card();metric(expenses,"Gesamtausgaben",money(expensesCents()));add(main,expenses,10);
        LinearLayout pending=card();metric(pending,"Offene Forderungen",money(pendingCents()));add(main,pending,10);
        LinearLayout net=card();metric(net,"Überschuss",money(incomeCents()-expensesCents()));add(main,net,10);
        add(main,text("Geplante Aufträge zählen nicht als offene Forderung. Abgeschlossene Aufträge zählen mit Restbetrag; bezahlte Aufträge nicht mehr. Zahlungen aus Aufträgen und eigenständige Einnahmen werden berücksichtigt. Erfasse denselben Zahlungseingang nicht noch einmal als zusätzliche Einnahme.",12,false),19);
    }
    private void morePick(String category){tab="Mehr";detailKind=category;render();}
    private void renderMore(){
        heading("Weitere Bereiche","Gemeinsame Verwaltung für euren Firmenalltag.");
        cardLink("Aufgaben & Checklisten",countKind("Aufgabe")+" Einträge",()->morePick("Aufgabe"));
        cardLink("Kunden & Adressen",countKind("Kunde")+" Einträge",()->morePick("Kunde"));
        cardLink("Fuhrpark & Fahrzeuge",countKind("Fahrzeug")+" Einträge",()->morePick("Fahrzeug"));
        cardLink("Geräte & Maschinen",countKind("Gerät")+" Einträge",()->morePick("Gerät"));
        cardLink("Finanzübersicht","Einnahmen, Ausgaben, offene Forderungen und Überschuss",()->morePick("Finanzübersicht"));
        cardLink("Änderungsprotokoll","Wer hat welchen Eintrag geändert?",()->morePick("Änderungsprotokoll"));
        add(main,text("Angemeldet als "+memberName(userId),15,true),25);
        add(main,button("Abmelden",()->new AlertDialog.Builder(this)
            .setTitle("Abmelden").setMessage("Möchtest du deine Sitzung beenden?")
            .setPositiveButton("Abmelden",(d,w)->{clear();members=new JSONArray();records=new JSONArray();events=new JSONArray();detailKind="";loginScreen();})
            .setNegativeButton("Abbrechen",null).show()),16);
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



    private void openVehicleDocuments(JSONObject vehicle){
        selectedVehicleId=vehicle.optString("id","");
        selectedVehicleTitle=vehicle.optString("title","Fahrzeug");
        vehicleDocumentsOpen=true;
        loadVehicleDocuments();
    }
    private void loadVehicleDocuments(){
        if(selectedVehicleId.isEmpty())return;
        shell("Fahrzeugdokumente");
        heading("Dokumente werden geladen","Geschützter Speicher für den Fahrzeugschein.");
        launch(()->new JSONArray(http("/rest/v1/record_documents?select=*&record_id=eq."
            +selectedVehicleId+"&order=created_at.desc","GET",null,true)),docs->{
            vehicleDocuments=docs;renderVehicleDocuments();
        });
    }
    private void renderVehicleDocuments(){
        shell("Fahrzeugschein");
        add(main,button("← Zurück zur Fahrzeugakte",()->{
            vehicleDocumentsOpen=false;
            JSONObject vehicle=recordById(selectedVehicleId);
            if(vehicle!=null)vehicleDetail(vehicle);
            else{detailKind="Fahrzeug";render();}
        }),0);
        heading(selectedVehicleTitle,"Fahrzeugschein-Fotos sicher hinterlegen.");
        add(main,goldButton("+ Fahrzeugschein-Foto auswählen",()->{
            Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("image/*");
            picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(picker,PICK_VEHICLE_IMAGE);
        }),20);
        add(main,text("Du kannst mehrere Fotos speichern, zum Beispiel Vorder- und Rückseite. Wenn du ein neues Bild aufnimmst, kannst du es anschließend aus der Galerie auswählen.",12,false),12);
        if(vehicleDocuments.length()==0)
            add(main,text("Noch keine Fahrzeugschein-Fotos gespeichert.",15,false),24);
        for(int i=0;i<vehicleDocuments.length();i++){
            JSONObject doc=vehicleDocuments.optJSONObject(i);
            if(doc==null)continue;
            String path=doc.optString("storage_path","");
            if(path.isEmpty())continue;
            LinearLayout row=card();
            add(row,text("📄 "+doc.optString("file_name","Fahrzeugschein"),16,true),0);
            add(row,text("Hochgeladen am "+doc.optString("created_at","").substring(0,
                Math.min(10,doc.optString("created_at","").length())),12,false),8);
            add(row,button("Foto öffnen",()->showDocumentPhoto(path)),13);
            add(row,button("Foto entfernen",()->new AlertDialog.Builder(this)
                .setTitle("Fahrzeugschein-Foto entfernen?")
                .setMessage("Dieses Bild wird dauerhaft aus dem Team-Speicher gelöscht.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Entfernen",(d,w)->removeVehicleDocument(doc)).show()),9);
            add(main,row,13);
        }
        add(main,button("⟳ Fotos aktualisieren",this::loadVehicleDocuments),15);
    }
    private byte[] imageFromUri(Uri uri) throws Exception{
        BitmapFactory.Options opts=new BitmapFactory.Options();
        opts.inJustDecodeBounds=true;
        try(InputStream probe=getContentResolver().openInputStream(uri)){
            if(probe==null)throw new IOException("Foto kann nicht geöffnet werden");
            BitmapFactory.decodeStream(probe,null,opts);
        }
        if(opts.outWidth<=0||opts.outHeight<=0)throw new IOException("Kein gültiges Bild");
        int sample=1;
        while((opts.outWidth/sample)>1800||(opts.outHeight/sample)>1800)sample*=2;
        BitmapFactory.Options actual=new BitmapFactory.Options();
        actual.inSampleSize=sample;
        Bitmap bitmap;
        try(InputStream stream=getContentResolver().openInputStream(uri)){
            if(stream==null)throw new IOException("Foto kann nicht geöffnet werden");
            bitmap=BitmapFactory.decodeStream(stream,null,actual);
        }
        if(bitmap==null)throw new IOException("Bildformat wird nicht unterstützt.");
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
            bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);
            byte[] data=out.toByteArray();
            if(data.length>8*1024*1024)throw new IOException("Bild ist zu groß. Bitte kleineres Foto wählen.");
            return data;
        }finally {bitmap.recycle();}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_VEHICLE_IMAGE && resultCode==RESULT_OK &&
            data!=null && data.getData()!=null && !selectedVehicleId.isEmpty()){
            Uri uri=data.getData();
            String recordId=selectedVehicleId;
            launch(()->{
                byte[] photo=imageFromUri(uri);
                String filename="fahrzeugschein-"+UUID.randomUUID().toString()+".jpg";
                String path=recordId+"/"+filename;
                binaryRequest("/storage/v1/object/"+DOC_BUCKET+"/"+path,"POST",photo,"image/jpeg",true);
                JSONObject meta=new JSONObject();
                meta.put("record_id",recordId);
                meta.put("storage_path",path);
                meta.put("file_name",filename);
                meta.put("mime_type","image/jpeg");
                meta.put("label","Fahrzeugschein");
                meta.put("uploaded_by",userId);
                try {http("/rest/v1/record_documents","POST",meta,true);}
                catch(Exception registerError){
                    try {binaryRequest("/storage/v1/object/"+DOC_BUCKET+"/"+path,"DELETE",null,null,true);}
                    catch(Exception ignored){}
                    throw registerError;
                }
                return true;
            },saved->loadVehicleDocuments());
        }
    }
    private byte[] binaryRequest(String path,String method,byte[] upload,String mime,boolean retry)throws Exception{
        URL url=new URL(base+path);
        if(!"https".equalsIgnoreCase(url.getProtocol()))throw new IOException("Nur HTTPS erlaubt.");
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(16000);connection.setReadTimeout(35000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("apikey",pub);
        connection.setRequestProperty("Authorization","Bearer "+access);
        connection.setRequestProperty("Accept","application/json");
        if(upload!=null){
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type",mime);
            connection.setFixedLengthStreamingMode(upload.length);
            try(OutputStream os=connection.getOutputStream()){os.write(upload);}
        }
        int status=connection.getResponseCode();
        InputStream in=status>=400?connection.getErrorStream():connection.getInputStream();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        if(in!=null){try(InputStream stream=in){
            byte[] buf=new byte[16384];int n;
            while((n=stream.read(buf))!=-1)bytes.write(buf,0,n);
        }}
        connection.disconnect();
        byte[] body=bytes.toByteArray();
        if(status==401 && retry && !refresh.isEmpty()){
            JSONObject next=new JSONObject();
            next.put("refresh_token",refresh);
            JSONObject tokens=new JSONObject(http("/auth/v1/token?grant_type=refresh_token","POST",next,false));
            saveTokens(tokens);
            return binaryRequest(path,method,upload,mime,false);
        }
        if(status>=400)throw new IOException("Dokumentenserver "+status+": "+
            new String(body,StandardCharsets.UTF_8).substring(0,Math.min(175,body.length)));
        return body;
    }
    private void showDocumentPhoto(String path){
        launch(()->binaryRequest("/storage/v1/object/authenticated/"+DOC_BUCKET+"/"+path,
            "GET",null,null,true),data->{
            Bitmap bmp=BitmapFactory.decodeByteArray(data,0,data.length);
            if(bmp==null){message("Bilddatei kann nicht angezeigt werden.");return;}
            ImageView photo=new ImageView(this);
            photo.setImageBitmap(bmp);
            photo.setAdjustViewBounds(true);
            photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            photo.setPadding(dp(12),dp(12),dp(12),dp(12));
            ScrollView sc=new ScrollView(this);sc.addView(photo);
            new AlertDialog.Builder(this).setTitle("Fahrzeugschein")
                .setView(sc).setPositiveButton("Schließen",null).show();
        });
    }
    private void removeVehicleDocument(JSONObject doc){
        String path=doc.optString("storage_path","");
        String id=doc.optString("id","");
        if(path.isEmpty()||id.isEmpty())return;
        launch(()->{
            binaryRequest("/storage/v1/object/"+DOC_BUCKET+"/"+path,"DELETE",null,null,true);
            http("/rest/v1/record_documents?id=eq."+id,"DELETE",null,true);
            return true;
        },done->loadVehicleDocuments());
    }


    private void taskDetail(JSONObject task){
        shell("Aufgabe");
        add(main,button("← Zurück zu Aufgaben",()->{detailKind="Aufgabe";render();}),0);
        heading(task.optString("title","Aufgabe"),"Checkliste und Bearbeitungsstand.");
        LinearLayout summary=card();
        add(summary,text("Status: "+task.optString("status","Geplant"),15,true),0);
        add(summary,text("Zuständig: "+assignmentName(task),13,false),10);
        String d=displayDate(task.optString("due_date",""));
        if(!d.isEmpty())add(summary,text("Termin: "+d,13,false),8);
        String description=task.optString("details","");
        if(!description.isEmpty())add(summary,text(description,14,false),12);
        add(main,summary,15);
        JSONObject meta=task.optJSONObject("metadata");
        String raw=meta==null?"":meta.optString("checklist","");
        String[] lines=raw.split("\\r?\\n");
        JSONArray done=meta==null?new JSONArray():meta.optJSONArray("checklist_done");
        if(done==null)done=new JSONArray();
        Set<Integer> selected=new HashSet<>();
        for(int i=0;i<done.length();i++)selected.add(done.optInt(i,-1));
        LinearLayout checklist=card();
        add(checklist,text("Checkliste",18,true),0);
        java.util.List<CheckBox> boxes=new java.util.ArrayList<>();
        if(raw.trim().isEmpty())add(checklist,text("Noch keine Checklistenpunkte. Du kannst sie über „Bearbeiten“ ergänzen.",13,false),10);
        for(int i=0;i<lines.length;i++){
            if(lines[i].trim().isEmpty())continue;
            CheckBox box=new CheckBox(this);
            box.setText(lines[i].trim());
            box.setChecked(selected.contains(i));
            box.setTag(i);
            box.setTextColor(NAVY);
            box.setTextSize(15);
            add(checklist,box,10);
            boxes.add(box);
        }
        add(main,checklist,14);
        if(!boxes.isEmpty())add(main,goldButton("Checkliste speichern",()->{
            JSONObject newer=new JSONObject();
            try{
                newer=new JSONObject(meta==null?"{}":meta.toString());
                JSONArray values=new JSONArray();
                for(CheckBox box:boxes)if(box.isChecked())values.put((Integer)box.getTag());
                newer.put("checklist_done",values);
            }catch(Exception ex){message("Checkliste konnte nicht vorbereitet werden.");return;}
            JSONObject updatedMeta=newer;
            launch(()->{
                JSONObject payload=new JSONObject();payload.put("metadata",updatedMeta);
                JSONObject args=new JSONObject();args.put("p_id",task.optString("id"));args.put("p_values",payload);
                http("/rest/v1/rpc/update_record","POST",args,true);
                return true;
            },doneSave->{message("Checkliste gespeichert.");load();});
        }),14);
        add(main,button("Aufgabe bearbeiten",()->editor(task)),16);
    }

    private String dateOrNull(JSONObject record,String key){
        if(record==null || record.isNull(key))return "";
        return record.optString(key,"");
    }
    private String metadataValue(JSONObject record,String key){
        if(record==null)return "";
        JSONObject m=record.optJSONObject("metadata");
        return m==null?"":m.optString(key,"");
    }
    private boolean isCurrentAdmin(){
        for(int i=0;i<members.length();i++){
            JSONObject m=members.optJSONObject(i);
            if(m!=null&&userId.equalsIgnoreCase(m.optString("user_id"))
                &&m.optString("role").equals("admin"))return true;
        }
        return false;
    }
    private void vehicleDetail(JSONObject vehicle){
        shell("Fahrzeugakte");
        add(main,button("← Zurück zum Fuhrpark",()->{detailKind="Fahrzeug";render();}),0);
        heading(vehicle.optString("title","Fahrzeug"),"Fahrzeugdaten und geschützte Dokumente.");
        LinearLayout info=card();
        JSONObject meta=vehicle.optJSONObject("metadata");
        if(meta==null)meta=new JSONObject();
        add(info,text("Kennzeichen: "+meta.optString("kennzeichen","–"),16,true),0);
        add(info,text("Marke: "+meta.optString("marke","–")+"  ·  Modell: "+meta.optString("modell","–"),14,false),10);
        add(info,text("HU/TÜV: "+(meta.optString("hu_date","").isEmpty()?"Nicht eingetragen":displayDate(meta.optString("hu_date"))),14,false),10);
        add(info,text("Fahrgestellnummer: "+meta.optString("vin","–"),14,false),8);
        add(info,text("Standort: "+meta.optString("standort","–"),14,false),8);
        if(!vehicle.optString("details","").isEmpty())add(info,text(vehicle.optString("details"),14,false),10);
        add(main,info,15);
        add(main,goldButton("Fahrzeugschein-Fotos ansehen / hinzufügen",()->openVehicleDocuments(vehicle)),18);
        add(main,button("Fahrzeug bearbeiten",()->editor(vehicle)),12);
        add(main,text("Fahrzeugscheinfotos werden privat im Firmenkonto gespeichert. Zugang nur für freigeschaltete Teammitglieder.",12,false),16);
    }
    private EditText optionalMultiline(LinearLayout layout,String title,String value,int minLines){
        EditText box=input(layout,title,value,false);
        box.setSingleLine(false);
        box.setMinLines(minLines);
        return box;
    }
    private void editor(JSONObject current){
        final String category=current==null?kind:current.optString("kind",kind);
        final boolean job=category.equals("Auftrag");
        final boolean appointment=category.equals("Termin");
        final boolean task=category.equals("Aufgabe");
        final boolean person=category.equals("Kunde");
        final boolean vehicle=category.equals("Fahrzeug");
        final boolean equipment=category.equals("Gerät");
        final boolean invoice=category.equals("Rechnung");
        final boolean offer=category.equals("Angebot");
        final boolean income=category.equals("Einnahme");
        final boolean expense=category.equals("Ausgabe");
        final boolean moneyKind=job||invoice||offer||income||expense;
        final boolean assignable=job||appointment||task;
        LinearLayout layout=vertical();
        layout.setPadding(dp(18),dp(6),dp(18),dp(9));
        String titleLabel=person?"Kundenname *":vehicle?"Fahrzeugbezeichnung *":equipment?"Gerät / Maschine *":
            task?"Aufgabe *":income?"Einnahme *":expense?"Ausgabe *":category+" *";
        EditText title=input(layout,titleLabel,current==null?"":current.optString("title",""),false);
        EditText customer=null;
        if(job||appointment||invoice||offer){
            customer=input(layout,"Kunde / Auftraggeber",current==null?"":current.optString("customer",""),false);
        }
        EditText address=null;
        EditText phone=null;
        if(job||appointment||person||invoice||offer){
            address=input(layout,"Anschrift / Einsatzort",current==null?"":current.optString("address",""),false);
            phone=input(layout,"Telefon",current==null?"":current.optString("phone",""),false);
            phone.setInputType(InputType.TYPE_CLASS_PHONE);
        }
        EditText details=optionalMultiline(layout,"Beschreibung / Notizen",current==null?"":current.optString("details",""),3);
        EditText checklist=null;
        if(task) checklist=optionalMultiline(layout,"Checkliste (eine Aufgabe pro Zeile)",metadataValue(current,"checklist"),3);
        EditText make=null,model=null,plate=null,vin=null,location=null,hu=null,manufacturer=null,serial=null,maintenance=null;
        if(vehicle){
            make=input(layout,"Marke",metadataValue(current,"marke"),false);
            model=input(layout,"Modell",metadataValue(current,"modell"),false);
            plate=input(layout,"Kennzeichen",metadataValue(current,"kennzeichen"),false);
            vin=input(layout,"Fahrgestellnummer (FIN)",metadataValue(current,"vin"),false);
            location=input(layout,"Standort",metadataValue(current,"standort"),false);
            hu=input(layout,"Nächste HU / TÜV · Kalender",displayDate(metadataValue(current,"hu_date")),false);
            hu.setFocusable(false);hu.setClickable(true);
            EditText target=hu;
            hu.setOnClickListener(v->chooseDate(target));
        }
        if(equipment){
            manufacturer=input(layout,"Hersteller",metadataValue(current,"hersteller"),false);
            serial=input(layout,"Seriennummer",metadataValue(current,"seriennummer"),false);
            location=input(layout,"Standort",metadataValue(current,"standort"),false);
            maintenance=input(layout,"Nächste Wartung · Kalender",displayDate(metadataValue(current,"wartung_date")),false);
            maintenance.setFocusable(false);maintenance.setClickable(true);
            EditText target=maintenance;maintenance.setOnClickListener(v->chooseDate(target));
        }
        EditText due=null;
        if(job||appointment||task||invoice||offer||income||expense){
            due=input(layout,(job||appointment||task?"Termin":"Datum")+" · Kalender (TT.MM.JJJJ)",displayDate(dateOrNull(current,"due_date")),false);
            due.setFocusable(false);due.setClickable(true);
            EditText target=due;
            due.setOnClickListener(v->chooseDate(target));
            add(layout,button("Datum entfernen",()->target.setText("")),7);
        }
        EditText amount=null,paid=null;
        if(moneyKind){
            amount=input(layout,"Betrag in Euro",current==null?"0,00":String.format(Locale.GERMANY,"%.2f",current.optLong("amount_cents")/100.0),false);
            amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
            if(job||invoice){
                paid=input(layout,"Bereits bezahlt in Euro",current==null?"0,00":String.format(Locale.GERMANY,"%.2f",current.optLong("paid_cents")/100.0),false);
                paid.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
            }
        }
        Spinner statuses=null;
        if(job||task||appointment||invoice||offer){
            add(layout,text("Status",13,true),16);
            String[] options=job?new String[]{"Geplant","Abgeschlossen","Bezahlt"}:
                task?new String[]{"Geplant","Erledigt"}:
                appointment?new String[]{"Geplant","Abgeschlossen"}:
                invoice?new String[]{"Offen","Bezahlt"}:
                new String[]{"Offen","Abgeschlossen"};
            statuses=new Spinner(this);
            statuses.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,options));
            String old=current==null?"":current.optString("status","");
            if(job){
                if(old.equals("Erledigt"))old="Abgeschlossen";
                if(old.equals("Offen")||old.equals("In Arbeit")||old.equals("Storniert"))old="Geplant";
                if(old.isEmpty())old="Geplant";
            }else if(old.isEmpty()){old="Geplant";}
            for(int i=0;i<options.length;i++)if(options[i].equals(old))statuses.setSelection(i);
            add(layout,statuses,5);
            if(job)add(layout,text("Geplant: 0 € offen. Abgeschlossen: Restbetrag offen. Bezahlt: vollständig bezahlt.",12,false),8);
        }
        Spinner assigned=null;
        String[] assignIds=null;
        if(assignable){
            add(layout,text("Zuständig",13,true),16);
            int n=members.length();
            String[] names=new String[n+2];
            assignIds=new String[n+2];
            names[0]="Nicht zugewiesen";assignIds[0]="";
            for(int i=0;i<n;i++){
                JSONObject member=members.optJSONObject(i);
                names[i+1]=member.optString("display_name","Teammitglied");
                assignIds[i+1]=member.optString("user_id","");
            }
            names[n+1]="Beide (Silvan & Lion)";
            assignIds[n+1]="BOTH";
            assigned=new Spinner(this);
            assigned.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
            if(current!=null&&current.optBoolean("assigned_both",false))assigned.setSelection(n+1);
            else{
                String id=current==null?"":current.optString("assigned_to","");
                for(int i=0;i<assignIds.length;i++)if(assignIds[i].equalsIgnoreCase(id))assigned.setSelection(i);
            }
            add(layout,assigned,6);
        }
        Spinner orderLink=null;
        String[] linkIds=null;
        if(invoice||income){
            add(layout,text(invoice?"Zugehöriger Auftrag (optional, verhindert doppelte offene Beträge)":
                "Zugehöriger Auftrag (optional, damit Einnahmen nicht doppelt zählen)",12,true),17);
            int n=countKind("Auftrag");
            String[] names=new String[n+1];
            linkIds=new String[n+1];
            names[0]="Ohne Auftragszuordnung";linkIds[0]="";
            int x=1;
            for(int i=0;i<records.length();i++){
                JSONObject record=records.optJSONObject(i);
                if(record!=null&&record.optString("kind").equals("Auftrag")){
                    names[x]=record.optString("title","Auftrag");
                    linkIds[x]=record.optString("id");x++;
                }
            }
            orderLink=new Spinner(this);
            orderLink.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
            String previous=current==null?"":current.optString("linked_order_id","");
            for(int i=0;i<linkIds.length;i++)if(linkIds[i].equalsIgnoreCase(previous))orderLink.setSelection(i);
            add(layout,orderLink,5);
        }
        final EditText fCustomer=customer,fAddress=address,fPhone=phone,fChecklist=checklist;
        final EditText fMake=make,fModel=model,fPlate=plate,fVin=vin,fLocation=location,fHu=hu,fManufacturer=manufacturer,
            fSerial=serial,fMaintenance=maintenance,fDue=due,fAmount=amount,fPaid=paid;
        final Spinner fStatuses=statuses,fAssigned=assigned,fOrderLink=orderLink;
        final String[] fAssignIds=assignIds,fLinkIds=linkIds;
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);scroll.addView(layout);
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle(current==null?"Neuer Eintrag: "+category:"Eintrag bearbeiten")
            .setView(scroll)
            .setNegativeButton("Abbrechen",null)
            .setPositiveButton("Speichern",null)
            .create();
        dialog.setOnShowListener(unused->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String newTitle=title.getText().toString().trim();
            if(newTitle.isEmpty()){title.setError("Bezeichnung eingeben");return;}
            String serverDate="";
            if(fDue!=null&&!fDue.getText().toString().trim().isEmpty()){
                try {serverDate=LocalDate.parse(fDue.getText().toString().trim(),DATE_DDMMYYYY).toString();}
                catch(Exception ex){message("Ungültiges Datum, bitte im Kalender wählen.");return;}
            }
            JSONObject md=new JSONObject();
            try{
                if(task)md.put("checklist",fChecklist.getText().toString());
                if(vehicle){
                    md.put("marke",fMake.getText().toString().trim());
                    md.put("modell",fModel.getText().toString().trim());
                    md.put("kennzeichen",fPlate.getText().toString().trim());
                    md.put("vin",fVin.getText().toString().trim());
                    md.put("standort",fLocation.getText().toString().trim());
                    String d=fHu.getText().toString().trim();
                    md.put("hu_date",d.isEmpty()?"":LocalDate.parse(d,DATE_DDMMYYYY).toString());
                }
                if(equipment){
                    md.put("hersteller",fManufacturer.getText().toString().trim());
                    md.put("seriennummer",fSerial.getText().toString().trim());
                    md.put("standort",fLocation.getText().toString().trim());
                    String d=fMaintenance.getText().toString().trim();
                    md.put("wartung_date",d.isEmpty()?"":LocalDate.parse(d,DATE_DDMMYYYY).toString());
                }
            }catch(Exception ex){message("Datum in den Fahrzeug- oder Gerätedaten bitte im Kalender wählen.");return;}
            JSONObject payload=new JSONObject();
            try{
                payload.put("kind",category);
                payload.put("title",newTitle);
                payload.put("details",details.getText().toString());
                payload.put("customer",fCustomer==null?"":fCustomer.getText().toString());
                payload.put("address",fAddress==null?"":fAddress.getText().toString());
                payload.put("phone",fPhone==null?"":fPhone.getText().toString());
                payload.put("due_date",serverDate.isEmpty()?JSONObject.NULL:serverDate);
                payload.put("metadata",md);
                payload.put("status",fStatuses==null?"Erfasst":fStatuses.getSelectedItem().toString());
                long amountCents=moneyToCents(fAmount==null?"0":fAmount.getText().toString());
                long paidCents=moneyToCents(fPaid==null?"0":fPaid.getText().toString());
                if(amountCents<0||paidCents<0||paidCents>amountCents){
                    message("Beträge müssen positiv sein. Der bezahlte Betrag darf nicht höher als die Gesamtsumme sein.");return;
                }
                if(job&&"Bezahlt".equals(payload.getString("status")))paidCents=amountCents;
                payload.put("amount_cents",amountCents);
                payload.put("paid_cents",paidCents);
                String assignment=fAssigned==null?"":fAssignIds[fAssigned.getSelectedItemPosition()];
                payload.put("assigned_both",assignment.equals("BOTH"));
                payload.put("assigned_to",assignment.isEmpty()||assignment.equals("BOTH")?JSONObject.NULL:assignment);
                if(invoice||income){
                    String linked=fLinkIds[fOrderLink.getSelectedItemPosition()];
                    payload.put("linked_order_id",linked.isEmpty()?JSONObject.NULL:linked);
                }
            }catch(Exception ex){message("Bitte gültige Euro-Beträge eingeben.");return;}
            launch(()->{
                if(current==null)http("/rest/v1/records","POST",payload,true);
                else{
                    JSONObject rpc=new JSONObject();
                    rpc.put("p_id",current.getString("id"));
                    rpc.put("p_values",payload);
                    http("/rest/v1/rpc/update_record","POST",rpc,true);
                }
                return true;
            },saved->{dialog.dismiss();load();});
        }));
        dialog.show();
    }
    private long moneyToCents(String value){
        String clean=value.trim().replace(" ","").replace("€","");
        if(clean.contains(",")&&clean.contains("."))clean=clean.replace(".","");
        clean=clean.replace(",",".");
        return new java.math.BigDecimal(clean).movePointRight(2)
            .setScale(0,java.math.RoundingMode.HALF_UP).longValueExact();
    }
    private void history(){
        shell("Änderungsprotokoll");
        heading("Wer hat was geändert?","Alle Änderungen werden über die bestätigte Anmeldung erfasst.");
        add(main,goldButton("← Zurück",()->{detailKind="";render();}),15);
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
