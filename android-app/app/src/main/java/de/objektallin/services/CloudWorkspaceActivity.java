package de.objektallin.services;

import android.app.Activity;
import android.app.AlertDialog;
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
    private static final String PREF="cloud_workspace", KEY_ALIAS="objekt_all_in_cloud_tokens_v1";
    private static final String[] KINDS={"Auftrag","Aufgabe","Termin","Kunde","Angebot","Rechnung","Einnahme","Fahrzeug","Gerät"};
    private static final String[] STATUSES={"Offen","Geplant","In Arbeit","Erledigt","Abgeschlossen","Bezahlt","Storniert"};
    private final ExecutorService network=Executors.newSingleThreadExecutor();
    private SharedPreferences pref;
    private String base="",pub="",access="",refresh="",userId="",kind="Auftrag";
    private JSONArray records=new JSONArray(),members=new JSONArray(),events=new JSONArray();
    private LinearLayout main;
    private boolean busy=false;

    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        pref=getSharedPreferences(PREF,MODE_PRIVATE);
        base=pref.getString("url","");pub=pref.getString("key","");
        readTokens();
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
    private void shell(String heading){
        LinearLayout root=vertical();root.setBackgroundColor(Color.rgb(247,249,252));setContentView(root);
        LinearLayout top=vertical();top.setPadding(dp(18),dp(19),dp(18),dp(18));top.setBackgroundColor(NAVY);
        TextView title=text("OBJEKT ALL-IN  ·  CLOUD",14,true);title.setTextColor(GOLD);top.addView(title);
        TextView name=text(heading,23,true);name.setTextColor(Color.WHITE);add(top,name,7);
        root.addView(top);
        ScrollView sc=new ScrollView(this);main=vertical();main.setPadding(dp(17),dp(17),dp(17),dp(55));sc.addView(main);root.addView(sc);
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
            catch(Exception ex){String message=ex.getMessage();runOnUiThread(()->{busy=false;if(!isFinishing())new AlertDialog.Builder(this).setTitle("Verbindung / Berechtigung").setMessage(message).setPositiveButton("OK",null).show();});}
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
            JSONObject newer=new JSONObject(http("/auth/v1/token?grant_type=refresh_token","POST",request,false,false));
            saveTokens(newer);
            return http(path,method,payload,true,false);
        }
        if(status>=400)throw new Exception("Serverfehler "+status+": "+answer.substring(0,Math.min(200,answer.length())));
        return answer;
    }
    private void loginScreen(){
        shell("Persönlich anmelden");
        add(main,text("Jeder von euch meldet sich mit seinem eigenen Benutzerkonto an. Der Arbeitsraum ist nur für freigeschaltete Mitglieder erreichbar.",14,false),3);
        EditText url=input(main,"Supabase-Projekt URL (https://...)",base,false);
        EditText key=input(main,"Öffentlicher Publishable Key",pub,false);
        EditText mail=input(main,"E-Mail-Adresse", "",false);mail.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText pass=input(main,"Passwort", "",true);
        add(main,button("Sicher anmelden",()->{
            String u=url.getText().toString().trim().replaceAll("/+$","");String k=key.getText().toString().trim();
            if(!u.startsWith("https://")||k.isEmpty()){message("Bitte gültige HTTPS-Adresse und öffentlichen Schlüssel eintragen.");return;}
            base=u;pub=k;pref.edit().putString("url",u).putString("key",k).apply();
            String email=mail.getText().toString().trim(),password=pass.getText().toString();
            if(email.isEmpty()||password.isEmpty()){message("E-Mail und Passwort fehlen.");return;}
            launch(()->{
                JSONObject request=new JSONObject();request.put("email",email);request.put("password",password);
                JSONObject r=new JSONObject(http("/auth/v1/token?grant_type=password","POST",request,false));saveTokens(r);
                String memberResponse=http("/rest/v1/company_members?select=user_id,display_name,role,active","GET",null,true);
                JSONArray list=new JSONArray(memberResponse);
                boolean approved=false;
                for(int i=0;i<list.length();i++)if(userId.equalsIgnoreCase(list.getJSONObject(i).optString("user_id")) && list.getJSONObject(i).optBoolean("active"))approved=true;
                if(!approved){clear();throw new Exception("Dieses Benutzerkonto ist noch nicht als Firmenmitglied freigegeben.");}
                return true;
            },unused->load());
        }),22);
        add(main,text("Keine Passwörter im Chat oder GitHub veröffentlichen. Nur Publishable Key, niemals Service-Role-Key verwenden.",12,false),10);
    }
    private void message(String m){new AlertDialog.Builder(this).setMessage(m).setPositiveButton("OK",null).show();}
    private void load(){
        shell("Team-Arbeitsraum");add(main,text("Aktualisiere gemeinsame Aufträge …",15,false),5);
        launch(()->{
            members=new JSONArray(http("/rest/v1/company_members?select=user_id,display_name,role,active&order=display_name.asc","GET",null,true));
            records=new JSONArray(http("/rest/v1/records?select=*&order=updated_at.desc&limit=300","GET",null,true));
            events=new JSONArray(http("/rest/v1/audit_events?select=id,record_id,record_title,record_kind,actor_id,action,changed_at&order=changed_at.desc&limit=150","GET",null,true));
            return true;
        },ok->render());
    }
    private String memberName(String id){
        for(int i=0;i<members.length();i++){JSONObject j=members.optJSONObject(i);if(j!=null&&id.equalsIgnoreCase(j.optString("user_id")))return j.optString("display_name","Teammitglied");}
        return "Nicht zugewiesen";
    }
    private void render(){
        shell("Gemeinsame Aufträge");
        add(main,text("Angemeldet als: "+memberName(userId),16,true),4);
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);
        Button reload=button("⟳ Aktualisieren",this::load);bar.addView(reload,new LinearLayout.LayoutParams(0,-2,1));
        Button log=button("Änderungen",this::history);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.leftMargin=dp(8);bar.addView(log,bp);add(main,bar,14);
        TextView heading=text("Bereich auswählen",15,true);add(main,heading,18);
        Spinner chooser=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,KINDS);chooser.setAdapter(adapter);
        for(int i=0;i<KINDS.length;i++)if(kind.equals(KINDS[i]))chooser.setSelection(i);
        add(main,chooser,6);
        add(main,button("Bereich anzeigen",()->{kind=chooser.getSelectedItem().toString();render();}),10);
        add(main,button("+ Neuen Eintrag erstellen",()->editor(null)),8);
        int count=0;
        for(int i=0;i<records.length();i++){
            JSONObject entry=records.optJSONObject(i);
            if(entry==null||!kind.equals(entry.optString("kind")))continue;
            count++;
            LinearLayout row=vertical();row.setPadding(dp(16),dp(14),dp(16),dp(14));row.setBackground(shape(Color.WHITE,12));
            add(row,text(entry.optString("title"),16,true),0);
            add(row,text(entry.optString("customer","")+" · "+entry.optString("status")+" · "+memberName(entry.optString("assigned_to")),12,false),5);
            add(row,text("Zuletzt bearbeitet von "+memberName(entry.optString("updated_by")),12,false),5);
            row.setOnClickListener(v->editor(entry));add(main,row,10);
        }
        if(count==0)add(main,text("Noch keine Einträge in diesem Bereich.",14,false),19);
        add(main,button("Abmelden",()->new AlertDialog.Builder(this).setMessage("Wirklich abmelden?").setPositiveButton("Abmelden",(d,w)->{clear();loginScreen();}).setNegativeButton("Abbrechen",null).show()),27);
        add(main,text("Die bisherige Offline-Datenbank bleibt separat. Einträge hier werden online gespeichert und bei Lion auf dem iPhone sichtbar, sobald er aktualisiert.",12,false),12);
    }
    private void editor(JSONObject current){
        LinearLayout layout=vertical();layout.setPadding(dp(18),dp(5),dp(18),dp(5));
        EditText title=input(layout,"Titel *",current==null?"":current.optString("title"),false);
        EditText customer=input(layout,"Kunde",current==null?"":current.optString("customer"),false);
        EditText address=input(layout,"Adresse",current==null?"":current.optString("address"),false);
        EditText phone=input(layout,"Telefon",current==null?"":current.optString("phone"),false);
        EditText details=input(layout,"Beschreibung",current==null?"":current.optString("details"),false);details.setSingleLine(false);details.setMinLines(3);
        EditText due=input(layout,"Termin (JJJJ-MM-TT)",current==null?"":current.optString("due_date",""),false);
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
            String dt=due.getText().toString().trim();if(!dt.isEmpty()&&!dt.matches("\\d{4}-\\d{2}-\\d{2}")){due.setError("Bitte JJJJ-MM-TT eingeben");return;}
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
        shell("Wer hat was geändert?");add(main,button("← Zurück zu Aufträgen",this::render),0);
        for(int i=0;i<events.length();i++){
            JSONObject e=events.optJSONObject(i);if(e==null)continue;
            String action=e.optString("action");String german=action.equals("created")?"erstellt":action.equals("deleted")?"gelöscht":"geändert";
            add(main,text(e.optString("record_title")+" · "+german,15,true),17);
            add(main,text("Von "+memberName(e.optString("actor_id"))+" · "+e.optString("changed_at").replace("T"," ").substring(0,Math.min(16,e.optString("changed_at").length())),12,false),3);
        }
    }
}
