package de.objektallin.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TimePicker;
import org.json.JSONArray;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int NAVY=Color.rgb(7,30,56), NAVY2=Color.rgb(17,55,87);
    private static final int GOLD=Color.rgb(210,163,78), BG=Color.rgb(244,247,251);
    private static final int WHITE=Color.WHITE, INK=Color.rgb(19,43,66), MUTED=Color.rgb(99,118,135);
    private static final int STROKE=Color.rgb(223,231,238), GREEN=Color.rgb(32,126,101);
    private static final String[] MAIN_TABS={"Übersicht","Aufträge","Termine","Finanzen","Mehr"};
    private static final int REQUEST_PHOTO=410, REQUEST_BACKUP=411, REQUEST_RESTORE=412;
    private static final String[] ALL_TYPES={Entry.JOB,Entry.TASK,Entry.APPOINTMENT,Entry.CLIENT,Entry.OFFER,Entry.INVOICE,Entry.INCOME,Entry.VEHICLE,Entry.EQUIPMENT};
    private static final SimpleDateFormat DATE_FORMAT=new SimpleDateFormat("dd.MM.yyyy",Locale.GERMANY);
    private static final SimpleDateFormat DATE_TIME_FORMAT=new SimpleDateFormat("dd.MM.yyyy  HH:mm",Locale.GERMANY);
    private final NumberFormat euros=NumberFormat.getCurrencyInstance(Locale.GERMANY);
    private AppDatabase database;
    private LinearLayout page, root, bottomNav;
    private String screen="Übersicht", profile="Silvan";
    private long photoTargetId=-1;
    private TextView screenTitle;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        database=new AppDatabase(this);
        profile=getPreferences(MODE_PRIVATE).getString("profile","Silvan");
        buildShell(); show("Übersicht");
    }

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable outlined(int color,int radius,int stroke){GradientDrawable g=bg(color,radius);g.setStroke(dp(1),stroke);return g;}
    private LinearLayout vertical(){LinearLayout a=new LinearLayout(this);a.setOrientation(LinearLayout.VERTICAL);return a;}
    private LinearLayout horizontal(){LinearLayout a=new LinearLayout(this);a.setOrientation(LinearLayout.HORIZONTAL);a.setGravity(Gravity.CENTER_VERTICAL);return a;}
    private TextView text(String s,float size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void space(LinearLayout c,int height){View v=new View(this);c.addView(v,lp(1,height));}
    private void pad(View v,int l,int t,int r,int b){v.setPadding(dp(l),dp(t),dp(r),dp(b));}
    private void add(LinearLayout c,View v,int top){LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(top);c.addView(v,p);}
    private TextView label(String t){TextView x=text(t,12,MUTED,true);x.setLetterSpacing(.06f);return x;}
    private TextView action(String text,boolean gold,Runnable click){
        TextView b=text(text,14,gold?NAVY:WHITE,true);b.setGravity(Gravity.CENTER);
        b.setBackground(bg(gold?GOLD:NAVY,13));pad(b,16,13,16,13);
        b.setOnClickListener(v->click.run());return b;
    }
    private LinearLayout card(){LinearLayout c=vertical();c.setBackground(outlined(WHITE,18,STROKE));pad(c,17,17,17,17);return c;}
    private String money(long cents){return euros.format(cents/100d);}
    private String date(long millis,boolean time){return millis<=0?"Kein Datum":(time?DATE_TIME_FORMAT:DATE_FORMAT).format(new Date(millis));}
    private String symbol(String type){switch(type){case Entry.JOB:return "▣";case Entry.APPOINTMENT:return "▤";case Entry.TASK:return "✓";case Entry.INVOICE:return "€";case Entry.OFFER:return "✦";case Entry.INCOME:return "+";case Entry.CLIENT:return "♙";case Entry.VEHICLE:return "▰";case Entry.EQUIPMENT:return "⚙";default:return "•";}}
    private boolean financial(String type){return type.equals(Entry.OFFER)||type.equals(Entry.INVOICE)||type.equals(Entry.INCOME);}
    private boolean open(Entry e){return !(e.status.equals("Erledigt")||e.status.equals("Bezahlt")||e.status.equals("Abgeschlossen")||e.status.equals("Storniert"));}

    private void buildShell(){
        root=vertical();root.setBackgroundColor(BG);setContentView(root);
        LinearLayout header=horizontal();header.setBackgroundColor(NAVY);pad(header,16,8,15,10);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.logo_original);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo,lp(64,62));
        LinearLayout headTexts=vertical();
        TextView brand=text("OBJEKT ALL-IN",17,WHITE,true);brand.setLetterSpacing(.04f);headTexts.addView(brand);
        TextView sub=text("S E R V I C E S",10,GOLD,true);headTexts.addView(sub);
        LinearLayout.LayoutParams ht=lp(0,-2);ht.weight=1;ht.leftMargin=dp(7);header.addView(headTexts,ht);
        TextView who=text("● "+profile,12,WHITE,true);who.setBackground(bg(NAVY2,99));pad(who,11,9,11,9);
        who.setOnClickListener(v->chooseProfile());header.addView(who,lp(-2,-2));
        root.addView(header,lp(-1,78));
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.setVerticalScrollBarEnabled(false);
        page=vertical();pad(page,18,22,18,28);sc.addView(page);
        LinearLayout.LayoutParams cp=lp(-1,0);cp.weight=1;root.addView(sc,cp);
        bottomNav=horizontal();bottomNav.setBackgroundColor(WHITE);pad(bottomNav,3,6,3,5);
        root.addView(bottomNav,lp(-1,72));
    }
    private void chooseProfile(){
        String[] users={"Silvan (lokales Profil)","Lion (lokales Profil)"};
        new AlertDialog.Builder(this).setTitle("Arbeitsprofil wählen")
           .setSingleChoiceItems(users,profile.equals("Lion")?1:0,(d,which)->{
               profile=which==0?"Silvan":"Lion";
               getPreferences(MODE_PRIVATE).edit().putString("profile",profile).apply();
               d.dismiss();buildShell();show(screen);
           }).setNegativeButton("Abbrechen",null).show();
    }
    private void title(String name,String help){
        LinearLayout top=horizontal();
        LinearLayout block=vertical();
        block.addView(text(name,25,NAVY,true));
        if(help!=null){TextView x=text(help,13,MUTED,false);add(block,x,4);}
        LinearLayout.LayoutParams wp=lp(0,-2);wp.weight=1;top.addView(block,wp);
        page.addView(top);
        space(page,18);
    }
    private void drawNav(){
        bottomNav.removeAllViews();
        for(String name:MAIN_TABS){
            boolean active=screen.equals(name)||(screen.equals(Entry.JOB)&&name.equals("Aufträge"))||
                    ((screen.equals(Entry.INVOICE)||screen.equals(Entry.OFFER)||screen.equals(Entry.INCOME))&&name.equals("Finanzen"))||
                    ((screen.equals(Entry.TASK)||screen.equals(Entry.CLIENT)||screen.equals(Entry.VEHICLE)||screen.equals(Entry.EQUIPMENT))&&name.equals("Mehr"));
            LinearLayout box=vertical();box.setGravity(Gravity.CENTER);pad(box,2,4,2,2);
            TextView icon=text(navIcon(name),23,active?NAVY:GOLD,true);icon.setGravity(Gravity.CENTER);box.addView(icon);
            TextView small=text(name,10,active?NAVY:MUTED,active);small.setGravity(Gravity.CENTER);box.addView(small);
            LinearLayout.LayoutParams bp=lp(0,-1);bp.weight=1;bottomNav.addView(box,bp);
            box.setOnClickListener(v->show(name));
        }
    }
    private String navIcon(String tab){switch(tab){case "Übersicht":return "⌂";case "Aufträge":return "▣";case "Termine":return "▤";case "Finanzen":return "€";default:return "☰";}}
    private void show(String destination){
        screen=destination;page.removeAllViews();drawNav();
        if(destination.equals("Übersicht")){showHome();return;}
        if(destination.equals("Aufträge")){listScreen(Entry.JOB);return;}
        if(destination.equals("Termine")){listScreen(Entry.APPOINTMENT);return;}
        if(destination.equals("Finanzen")){showFinance();return;}
        if(destination.equals("Mehr")){showMore();return;}
        listScreen(destination);
    }
    private LinearLayout topCTA(String txt,Runnable click){
        LinearLayout row=horizontal();TextView button=action("+  "+txt,true,click);row.addView(button,lp(-1,-2));return row;
    }
    private void showHome(){
        title("Guten Tag, "+profile+"!","Deine Firma. Alles an einem Ort.");
        LinearLayout brandCard=vertical();brandCard.setBackground(bg(NAVY,20));pad(brandCard,20,22,20,20);
        TextView eyebrow=text("FIRMENÜBERSICHT",11,GOLD,true);eyebrow.setLetterSpacing(.16f);brandCard.addView(eyebrow);
        add(brandCard,text("Objekte im Blick.\nAufträge im Griff.",24,WHITE,true),8);
        add(brandCard,text("Alle wichtigen Vorgänge auf deinem Handy. Daten sind auf diesem Gerät gespeichert.",13,Color.rgb(202,217,228),false),12);
        add(brandCard,action("+ Neuen Auftrag erfassen",true,()->edit(new Entry())),17);
        page.addView(brandCard);
        List<Entry> data=database.all();
        int activeJobs=0,openTasks=0,appointments=0;
        long outstanding=0,revenue=0;Calendar now=Calendar.getInstance();
        for(Entry e:data){
            if(e.type.equals(Entry.JOB)&&open(e))activeJobs++;
            if(e.type.equals(Entry.TASK)&&open(e))openTasks++;
            if(e.type.equals(Entry.APPOINTMENT)&&e.dueDate>=System.currentTimeMillis()-3600000L)appointments++;
            if(e.type.equals(Entry.INVOICE)) outstanding+=Math.max(0,e.amountCents-e.paidCents);
            if(e.type.equals(Entry.INCOME)){
                Calendar when=Calendar.getInstance();when.setTimeInMillis(e.dueDate>0?e.dueDate:e.updatedAt);
                if(now.get(Calendar.YEAR)==when.get(Calendar.YEAR)&&now.get(Calendar.MONTH)==when.get(Calendar.MONTH))revenue+=e.amountCents;
            }
        }
        space(page,20);add(page,text("Auf einen Blick",19,NAVY,true),0);
        LinearLayout r1=horizontal();add(page,r1,13);
        metric(r1,"Aktive Aufträge",String.valueOf(activeJobs),"▣",()->show("Aufträge"));
        metric(r1,"Offene Aufgaben",String.valueOf(openTasks),"✓",()->show(Entry.TASK));
        LinearLayout r2=horizontal();add(page,r2,10);
        metric(r2,"Offene Rechnungen",money(outstanding),"€",()->show(Entry.INVOICE));
        metric(r2,"Einnahmen diesen Monat",money(revenue),"↗",()->show(Entry.INCOME));
        space(page,22);add(page,text("Schnellzugriff",19,NAVY,true),0);
        quick(Entry.APPOINTMENT,"Termin eintragen","▤");quick(Entry.CLIENT,"Kunden anlegen","♙");
        quick(Entry.OFFER,"Angebot erfassen","✦");quick(Entry.INVOICE,"Rechnung erfassen","€");
        space(page,20);add(page,text("Aktuelle Aufträge",19,NAVY,true),0);
        List<Entry> jobs=database.byType(Entry.JOB);int shown=0;
        for(Entry e:jobs){if(open(e)){recordCard(e);if(++shown>=3)break;}}
        if(shown==0)empty("Noch keine offenen Aufträge", "Tippe oben auf „Neuen Auftrag erfassen“.");
        add(page,action("Alle Aufträge ansehen",false,()->show("Aufträge")),13);
    }
    private void metric(LinearLayout row,String name,String value,String glyph,Runnable click){
        LinearLayout box=card();LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=row.getChildCount()==0?0:dp(10);
        row.addView(box,p);
        box.addView(text(glyph,21,GOLD,true));add(box,text(value,value.length()>10?16:24,NAVY,true),6);
        add(box,text(name,12,MUTED,false),5);box.setOnClickListener(v->click.run());
    }
    private void quick(String type,String name,String glyph){
        LinearLayout row=horizontal();row.setBackground(outlined(WHITE,14,STROKE));pad(row,13,13,13,13);
        TextView icon=text(glyph,22,GOLD,true);icon.setGravity(Gravity.CENTER);row.addView(icon,lp(32,36));
        TextView x=text(name,15,INK,true);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=dp(12);row.addView(x,p);
        row.addView(text("+",24,NAVY,true));row.setOnClickListener(v->edit(newWithType(type)));
        add(page,row,9);
    }
    private Entry newWithType(String t){Entry e=new Entry();e.type=t;e.owner=profile;e.status=t.equals(Entry.VEHICLE)||t.equals(Entry.EQUIPMENT)?"Aktiv":t.equals(Entry.OFFER)?"Entwurf":t.equals(Entry.INVOICE)?"Offen":t.equals(Entry.INCOME)?"Gebucht":"Offen";return e;}

    private void listScreen(String type){
        title(heading(type),"Verwalte alle "+heading(type).toLowerCase(Locale.GERMANY)+" an einem Ort.");
        add(page,topCTA(plusName(type),()->edit(newWithType(type))),0);
        List<Entry> all=database.byType(type);
        space(page,15);add(page,label(all.size()+" EINTRÄGE"),0);
        if(all.isEmpty()){empty("Noch keine Einträge", "Lege deinen ersten Eintrag über die goldene Schaltfläche an.");return;}
        for(Entry e:all)recordCard(e);
    }
    private String heading(String type){switch(type){case Entry.JOB:return "Aufträge";case Entry.TASK:return "Aufgaben";case Entry.APPOINTMENT:return "Termine";case Entry.CLIENT:return "Kunden";case Entry.OFFER:return "Angebote";case Entry.INVOICE:return "Rechnungen";case Entry.INCOME:return "Einnahmen";case Entry.VEHICLE:return "Fuhrpark";case Entry.EQUIPMENT:return "Geräte & Maschinen";default:return type;}}
    private String plusName(String t){switch(t){case Entry.JOB:return "Neuen Auftrag";case Entry.APPOINTMENT:return "Neuen Termin";case Entry.CLIENT:return "Neuen Kunden";case Entry.INVOICE:return "Neue Rechnung";case Entry.OFFER:return "Neues Angebot";default:return "Neuen Eintrag";}}
    private void recordCard(Entry e){
        LinearLayout c=card();LinearLayout row=horizontal();
        TextView glyph=text(symbol(e.type),20,NAVY,true);glyph.setGravity(Gravity.CENTER);glyph.setBackground(bg(Color.rgb(238,244,249),10));row.addView(glyph,lp(42,42));
        LinearLayout ts=vertical();LinearLayout.LayoutParams tp=lp(0,-2);tp.weight=1;tp.leftMargin=dp(11);row.addView(ts,tp);
        ts.addView(text(e.title.isEmpty()?e.type:e.title,15,NAVY,true));
        String sub=!e.customer.isEmpty()?e.customer:(!e.address.isEmpty()?e.address:e.type);
        TextView sm=text(sub,12,MUTED,false);sm.setMaxLines(2);add(ts,sm,2);
        c.addView(row);
        LinearLayout meta=horizontal();
        TextView status=text(e.status,11,statusColor(e.status),true);
        status.setBackground(bg(statusBackground(e.status),99));pad(status,9,5,9,5);meta.addView(status);
        if(e.dueDate>0){TextView d=text(date(e.dueDate,e.type.equals(Entry.APPOINTMENT)),11,MUTED,true);LinearLayout.LayoutParams px=lp(-2,-2);px.leftMargin=dp(10);meta.addView(d,px);}
        add(c,meta,12);
        if(financial(e.type)){String amount=money(e.amountCents);if(e.type.equals(Entry.INVOICE))amount+=" · offen "+money(Math.max(0,e.amountCents-e.paidCents));add(c,text(amount,13,NAVY,true),10);}
        if(e.type.equals(Entry.JOB)&&!e.address.isEmpty())add(c,text("⌖  "+e.address,12,MUTED,false),8);
        c.setOnClickListener(v->details(e.id));add(page,c,10);
    }
    private int statusColor(String s){return (s.equals("Erledigt")||s.equals("Bezahlt")||s.equals("Aktiv")||s.equals("Gebucht"))?GREEN:Color.rgb(135,94,40);}
    private int statusBackground(String s){return (s.equals("Erledigt")||s.equals("Bezahlt")||s.equals("Aktiv")||s.equals("Gebucht"))?0xFFE6F5EE:0xFFFFF2DD;}
    private void empty(String message,String sub){LinearLayout c=card();pad(c,22,29,22,29);TextView icon=text("▧",42,GOLD,true);icon.setGravity(Gravity.CENTER);c.addView(icon);TextView t=text(message,17,NAVY,true);t.setGravity(Gravity.CENTER);add(c,t,10);TextView x=text(sub,13,MUTED,false);x.setGravity(Gravity.CENTER);add(c,x,8);add(page,c,15);}
    private void showFinance(){
        title("Finanzen","Angebote, Rechnungen und Einnahmen im Überblick.");
        long offers=0,inv=0,paid=0,income=0;for(Entry e:database.all()){
            if(e.type.equals(Entry.OFFER))offers+=e.amountCents;
            if(e.type.equals(Entry.INVOICE)){inv+=e.amountCents;paid+=e.paidCents;}
            if(e.type.equals(Entry.INCOME))income+=e.amountCents;
        }
        LinearLayout a=card();a.addView(label("OFFENE RECHNUNGSBETRÄGE"));add(a,text(money(Math.max(0,inv-paid)),29,NAVY,true),8);
        add(a,text("Erfasste Rechnungen: "+money(inv)+"  ·  Teilzahlungen: "+money(paid),12,MUTED,false),5);page.addView(a);
        add(page,text("Erfasste Einnahmen: "+money(income),14,INK,true),14);
        space(page,17);quick(Entry.OFFER,"Angebote", "✦");quick(Entry.INVOICE,"Rechnungen", "€");quick(Entry.INCOME,"Einnahmen", "+");
        add(page,text("Hinweis: Diese Übersicht ersetzt keine Buchhaltungssoftware. Rechnungen werden hier nur dokumentiert.",12,MUTED,false),19);
    }
    private void showMore(){
        title("Weitere Bereiche","Alles Weitere für den Firmenalltag.");
        add(page,action("☁ Gemeinsamer Team-Arbeitsraum (Login)",true,()->startActivity(new Intent(this,CloudWorkspaceActivity.class))),0);
        add(page,text("Getrennte Anmeldung für Silvan und Lion, gemeinsame Aufträge und Änderungsprotokoll. Benötigt die Online-Datenbank.",12,MUTED,false),8);
        quick(Entry.TASK,"Aufgaben & Checklisten","✓");
        quick(Entry.CLIENT,"Kunden & Adressen","♙");
        quick(Entry.VEHICLE,"Fuhrpark & Fahrzeugakten","▰");
        quick(Entry.EQUIPMENT,"Geräte & Maschinen","⚙");
        space(page,19);add(page,text("Datensicherung",18,NAVY,true),0);
        LinearLayout info=card();info.addView(text("Offline und auf diesem Gerät",15,NAVY,true));
        add(info,text("Die App speichert alle Daten lokal. Um sie auf ein anderes Handy zu übertragen, kannst du eine Sicherungsdatei inklusive Fotos erstellen.",13,MUTED,false),10);
        add(info,action("Datensicherung exportieren",true,this::startBackup),16);
        add(info,action("Datensicherung importieren",false,this::startRestore),10);
        add(info,text("Der Import ersetzt die bestehenden App-Daten auf diesem Gerät. Vorher bitte eine Sicherung erstellen.",12,MUTED,false),11);
        add(page,info,13);
        add(page,text("Profil: "+profile+" · Zum Wechseln oben auf den Namen tippen. Profile sind keine getrennten Zugänge.",12,MUTED,false),17);
        add(page,text("Version 1.0 · Objekt All-In Services · Native Android-App",11,MUTED,false),10);
    }

    private EditText field(LinearLayout host,String name,String val,int inputType,boolean multi){
        TextView l=label(name);add(host,l,13);
        EditText e=new EditText(this);e.setText(val);e.setTextColor(INK);e.setTextSize(15);e.setHintTextColor(MUTED);
        e.setSingleLine(!multi);e.setInputType(inputType);if(multi){e.setMinLines(3);e.setGravity(Gravity.TOP);}
        e.setBackground(outlined(WHITE,9,STROKE));pad(e,12,12,12,12);add(host,e,6);return e;
    }
    private String[] statuses(String type){
        if(type.equals(Entry.JOB))return new String[]{"Anfrage","Geplant","In Arbeit","Erledigt","Storniert"};
        if(type.equals(Entry.TASK))return new String[]{"Offen","In Arbeit","Erledigt"};
        if(type.equals(Entry.APPOINTMENT))return new String[]{"Geplant","Erledigt","Abgesagt"};
        if(type.equals(Entry.OFFER))return new String[]{"Entwurf","Versendet","Angenommen","Abgelehnt"};
        if(type.equals(Entry.INVOICE))return new String[]{"Offen","Teilbezahlt","Bezahlt"};
        if(type.equals(Entry.VEHICLE)||type.equals(Entry.EQUIPMENT))return new String[]{"Aktiv","Wartung","Außer Betrieb"};
        if(type.equals(Entry.INCOME))return new String[]{"Gebucht"};
        return new String[]{"Aktiv"};
    }
    private Spinner pickStatus(LinearLayout host,Entry e){
        add(host,label("STATUS"),14);
        Spinner s=new Spinner(this);String[] opts=statuses(e.type);
        ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts);
        s.setAdapter(ad);int selected=Arrays.asList(opts).indexOf(e.status);s.setSelection(selected<0?0:selected);
        s.setBackground(outlined(WHITE,9,STROKE));pad(s,8,5,8,5);add(host,s,6);return s;
    }
    private void dateSelector(LinearLayout host,Entry e,TextView button){
        Calendar cal=Calendar.getInstance();if(e.dueDate>0)cal.setTimeInMillis(e.dueDate);
        new DatePickerDialog(this,(DatePicker view,int year,int month,int day)->{
            cal.set(Calendar.YEAR,year);cal.set(Calendar.MONTH,month);cal.set(Calendar.DAY_OF_MONTH,day);
            if(e.type.equals(Entry.APPOINTMENT)){
                new TimePickerDialog(this,(TimePicker v,int h,int m)->{
                    cal.set(Calendar.HOUR_OF_DAY,h);cal.set(Calendar.MINUTE,m);cal.set(Calendar.SECOND,0);
                    e.dueDate=cal.getTimeInMillis();button.setText("▤  "+date(e.dueDate,true));
                },cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show();
            }else{cal.set(Calendar.HOUR_OF_DAY,12);cal.set(Calendar.MINUTE,0);e.dueDate=cal.getTimeInMillis();button.setText("▤  "+date(e.dueDate,false));}
        },cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show();
    }
    private long parseCents(String value){
        String s=value.trim().replace("€","").replace(" ","").replace("\u00a0","");
        if(s.isEmpty())return 0;
        if(s.contains(","))s=s.replace(".","").replace(',','.');
        else if(s.indexOf('.')!=s.lastIndexOf('.'))s=s.replace(".","");
        java.math.BigDecimal v=new java.math.BigDecimal(s).movePointRight(2);
        return v.setScale(0,java.math.RoundingMode.HALF_UP).longValueExact();
    }
    private void edit(Entry e){
        if(e.owner.isEmpty())e.owner=profile;
        LinearLayout form=vertical();pad(form,18,6,18,18);
        EditText title=field(form,e.type.equals(Entry.CLIENT)?"NAME *":e.type.equals(Entry.VEHICLE)?"FAHRZEUG *":"BEZEICHNUNG *",e.title,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,false);
        EditText client=null,phone=null,address=null,owner=null,amount=null,paid=null,details=null;
        boolean person=!e.type.equals(Entry.TASK)&&!e.type.equals(Entry.EQUIPMENT)&&!e.type.equals(Entry.VEHICLE)&&!e.type.equals(Entry.CLIENT);
        if(person)client=field(form,"KUNDE / AUFTRAGGEBER",e.customer,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS,false);
        if(e.type.equals(Entry.CLIENT)||e.type.equals(Entry.JOB)||e.type.equals(Entry.APPOINTMENT)){
            phone=field(form,"TELEFON",e.phone,InputType.TYPE_CLASS_PHONE,false);
            address=field(form,"ADRESSE / EINSATZORT",e.address,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS,false);
        }
        if(financial(e.type)||e.type.equals(Entry.JOB)){
            amount=field(form,"BETRAG IN EURO",e.amountCents==0?"":String.format(Locale.GERMANY,"%.2f",e.amountCents/100d),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED,false);
            if(e.type.equals(Entry.INVOICE))paid=field(form,"BEREITS BEZAHLT IN EURO",e.paidCents==0?"":String.format(Locale.GERMANY,"%.2f",e.paidCents/100d),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED,false);
        }
        if(!e.type.equals(Entry.CLIENT)){
            add(form,label(e.type.equals(Entry.APPOINTMENT)?"TERMIN / UHRZEIT":e.type.equals(Entry.VEHICLE)?"NÄCHSTE PRÜFUNG / HU":e.type.equals(Entry.INCOME)?"BUCHUNGSDATUM":"DATUM / FRIST"),14);
            TextView d=action("▤  "+(e.dueDate>0?date(e.dueDate,e.type.equals(Entry.APPOINTMENT)):"Datum wählen"),false,()->{});
            d.setOnClickListener(v->dateSelector(form,e,d));add(form,d,6);
        }
        Spinner status=pickStatus(form,e);
        if(!e.type.equals(Entry.CLIENT)&&!e.type.equals(Entry.INCOME))owner=field(form,"ZUSTÄNDIG",e.owner,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS,false);
        details=field(form,"NOTIZEN / BESCHREIBUNG",e.details,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,true);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle((e.id==0?"Neu: ":"Bearbeiten: ")+e.type)
                .setView(scroll).setNegativeButton("Abbrechen",(d,w)->{})
                .setPositiveButton("Speichern",null).create();
        final EditText cf=client,pf=phone,af=address,of=owner,mf=amount,zf=paid,df=details;
        dialog.setOnShowListener(ignored->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(NAVY);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                if(title.getText().toString().trim().isEmpty()){title.setError("Bitte eine Bezeichnung eingeben");title.requestFocus();return;}
                try{
                    e.title=title.getText().toString().trim();if(cf!=null)e.customer=cf.getText().toString().trim();
                    if(pf!=null)e.phone=pf.getText().toString().trim();if(af!=null)e.address=af.getText().toString().trim();
                    if(of!=null)e.owner=of.getText().toString().trim();if(mf!=null)e.amountCents=parseCents(mf.getText().toString());
                    if(zf!=null)e.paidCents=parseCents(zf.getText().toString());
                    if(e.amountCents<0||e.paidCents<0||(e.type.equals(Entry.INVOICE)&&e.paidCents>e.amountCents))throw new IllegalArgumentException("Bitte gültige Beträge verwenden. Der bezahlte Betrag darf nicht größer als die Rechnung sein.");
                    e.status=status.getSelectedItem().toString();if(e.type.equals(Entry.INVOICE)){if(e.amountCents>0&&e.paidCents>=e.amountCents)e.status="Bezahlt";else if(e.paidCents>0)e.status="Teilbezahlt";}
                    e.details=df.getText().toString().trim();e.id=database.save(e);
                    dialog.dismiss();show(screen);toast("Eintrag gespeichert");
                }catch(Exception ex){new AlertDialog.Builder(this).setMessage(ex.getMessage()).setPositiveButton("OK",null).show();}
            });
        });dialog.show();
    }

    private void details(long id){
        Entry e=database.get(id);if(e==null){toast("Eintrag nicht gefunden");return;}
        LinearLayout contents=vertical();pad(contents,20,5,20,15);
        add(contents,text(e.title,21,NAVY,true),4);
        add(contents,text(e.type+" · "+e.status,12,statusColor(e.status),true),5);
        if(!e.customer.isEmpty())detailLine(contents,"Kunde",e.customer);
        if(!e.phone.isEmpty()){
            TextView call=text("☎  "+e.phone,15,NAVY,true);add(contents,call,10);call.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+e.phone.replace(" ","")));startActivity(i);});
        }
        if(!e.address.isEmpty())detailLine(contents,"Ort",e.address);
        if(e.dueDate>0)detailLine(contents,"Datum",date(e.dueDate,e.type.equals(Entry.APPOINTMENT)));
        if(financial(e.type)||e.type.equals(Entry.JOB))detailLine(contents,"Betrag",money(e.amountCents));
        if(e.type.equals(Entry.INVOICE)){detailLine(contents,"Bezahlt",money(e.paidCents));detailLine(contents,"Offen",money(Math.max(0,e.amountCents-e.paidCents)));}
        if(!e.owner.isEmpty())detailLine(contents,"Zuständig",e.owner);
        if(!e.details.isEmpty())detailLine(contents,"Notizen",e.details);
        if(e.type.equals(Entry.JOB)){
            add(contents,text("Auftragsfotos",16,NAVY,true),17);
            JSONArray photos;
            try{photos=new JSONArray(e.photoUris);}catch(Exception ex){photos=new JSONArray();}
            for(int i=0;i<photos.length();i++){
                String name=photos.optString(i);File f=new File(getPhotosDir(),name);
                if(!f.isFile())continue;
                ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
                Bitmap bitmap=BitmapFactory.decodeFile(f.getAbsolutePath(),options);
                if(bitmap!=null)iv.setImageBitmap(bitmap);iv.setBackground(bg(BG,10));
                LinearLayout.LayoutParams imageLp=lp(-1,142);imageLp.topMargin=dp(9);contents.addView(iv,imageLp);
            }
            add(contents,action("+ Foto aus Galerie hinzufügen",true,()->{photoTargetId=e.id;selectPhoto();}),13);
        }
        ScrollView sc=new ScrollView(this);sc.addView(contents);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Eintrag ansehen").setView(sc)
              .setNeutralButton("Löschen",(d,w)->confirmDelete(e.id))
              .setNegativeButton("Schließen",(d,w)->{})
              .setPositiveButton("Bearbeiten",(d,w)->edit(e)).create();dialog.show();
    }
    private void detailLine(LinearLayout c,String label,String value){add(c,text(label.toUpperCase(Locale.GERMANY),10,MUTED,true),14);add(c,text(value,15,INK,false),3);}
    private void confirmDelete(long id){new AlertDialog.Builder(this).setTitle("Eintrag löschen?").setMessage("Der Eintrag wird dauerhaft entfernt. Eine Löschung kann nur aus einer vorherigen Sicherung rückgängig gemacht werden.")
            .setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(d,w)->{database.delete(id);show(screen);toast("Eintrag gelöscht");}).show();}
    private void selectPhoto(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQUEST_PHOTO);}
    private File getPhotosDir(){File dir=new File(getFilesDir(),"photos");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Foto-Verzeichnis konnte nicht angelegt werden.");return dir;}
    private void importPhoto(Uri uri){
        Entry e=database.get(photoTargetId);if(e==null)throw new IllegalArgumentException("Auftrag nicht gefunden");
        String filename="auftrag_"+e.id+"_"+System.currentTimeMillis()+".jpg";
        File dest=new File(getPhotosDir(),filename);
        try(InputStream in=getContentResolver().openInputStream(uri)){
            if(in==null)throw new IllegalStateException("Bild konnte nicht geöffnet werden");
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeStream(in,null,options);
            int biggest=Math.max(options.outWidth,options.outHeight);
            int sample=1;while(biggest/sample>1800)sample*=2;
            try(InputStream second=getContentResolver().openInputStream(uri)){
                options.inJustDecodeBounds=false;options.inSampleSize=sample;
                Bitmap b=BitmapFactory.decodeStream(second,null,options);
                if(b==null)throw new IllegalArgumentException("Bildformat nicht unterstützt");
                try(FileOutputStream out=new FileOutputStream(dest)){if(!b.compress(Bitmap.CompressFormat.JPEG,86,out))throw new IllegalStateException("Bild konnte nicht gespeichert werden");}
                b.recycle();
            }
            JSONArray a;try{a=new JSONArray(e.photoUris);}catch(Exception ex){a=new JSONArray();}
            a.put(filename);e.photoUris=a.toString();database.save(e);toast("Foto zum Auftrag gespeichert");
        }catch(Exception ex){dest.delete();throw new IllegalArgumentException(ex.getMessage());}
    }
    private void startBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,"Objekt-All-In-Sicherung-"+DATE_FORMAT.format(new Date())+".zip");startActivityForResult(i,REQUEST_BACKUP);}
    private void startRestore(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");startActivityForResult(i,REQUEST_RESTORE);}
    private void createBackup(Uri uri) throws Exception{
        try(OutputStream output=getContentResolver().openOutputStream(uri);ZipOutputStream out=new ZipOutputStream(output)){
            out.putNextEntry(new ZipEntry("backup.json"));out.write(database.exportJson().getBytes(StandardCharsets.UTF_8));out.closeEntry();
            File[] photos=getPhotosDir().listFiles();if(photos!=null){
                for(File photo:photos){if(!photo.isFile()||!photo.getName().endsWith(".jpg"))continue;
                    out.putNextEntry(new ZipEntry("photos/"+photo.getName()));
                    try(FileInputStream in=new FileInputStream(photo)){byte[] buf=new byte[16384];int n;while((n=in.read(buf))>=0){out.write(buf,0,n);}}
                    out.closeEntry();
                }
            }
        }
    }
    private void restoreBackup(Uri uri) throws Exception{
        String json=null;File dir=getPhotosDir();File temp=new File(getCacheDir(),"photo_restore_"+System.currentTimeMillis());
        if(!temp.mkdirs())throw new IllegalStateException("Temporäres Verzeichnis kann nicht erstellt werden.");
        try(InputStream raw=getContentResolver().openInputStream(uri);ZipInputStream zip=new ZipInputStream(raw)){
            ZipEntry e;int files=0;long total=0;
            while((e=zip.getNextEntry())!=null){
                if(++files>10000)throw new IllegalArgumentException("Zu viele Dateien im Backup");
                ByteArrayOutputStream mem=new ByteArrayOutputStream();byte[] bytes=new byte[16384];int read;
                if(e.getName().equals("backup.json")){
                    while((read=zip.read(bytes))!=-1){mem.write(bytes,0,read);if(mem.size()>20_000_000)throw new IllegalArgumentException("Sicherungsdatei zu groß");}
                    json=mem.toString("UTF-8");
                }else if(e.getName().startsWith("photos/")&&e.getName().endsWith(".jpg")){
                    String name=e.getName().substring("photos/".length());
                    if(name.contains("/")||name.contains("\\")||name.contains("..")||name.isEmpty())throw new IllegalArgumentException("Ungültiger Fotodateiname");
                    try(FileOutputStream out=new FileOutputStream(new File(temp,name))){
                        long fileSize=0;while((read=zip.read(bytes))!=-1){out.write(bytes,0,read);fileSize+=read;total+=read;
                           if(fileSize>20_000_000||total>500_000_000)throw new IllegalArgumentException("Fotosicherung zu groß");}
                    }
                }else{while(zip.read(bytes)!=-1){/* ignore unexpected files */}}
                zip.closeEntry();
            }
        }catch(Exception ex){deleteTree(temp);throw ex;}
        if(json==null){deleteTree(temp);throw new IllegalArgumentException("backup.json fehlt");}
        // Validate JSON before replacing anything.
        org.json.JSONObject check=new org.json.JSONObject(json);
        if(!"objekt-all-in-backup-v1".equals(check.optString("format"))){deleteTree(temp);throw new IllegalArgumentException("Ungültige Sicherungsdatei");}
        File[] stored=dir.listFiles();if(stored!=null){for(File f:stored){if(f.isFile()&&f.getName().endsWith(".jpg"))f.delete();}}
        File[] imports=temp.listFiles();if(imports!=null){for(File f:imports){File target=new File(dir,f.getName());if(!f.renameTo(target))copyFile(f,target);}}
        database.importReplacing(json);deleteTree(temp);
    }
    private void deleteTree(File dir){File[] fs=dir.listFiles();if(fs!=null)for(File f:fs){if(f.isDirectory())deleteTree(f);else f.delete();}dir.delete();}
    private void copyFile(File src,File dest)throws Exception{try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dest)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{
            if(request==REQUEST_PHOTO){importPhoto(uri);}
            else if(request==REQUEST_BACKUP){createBackup(uri);toast("Sicherung mit Fotos gespeichert");}
            else if(request==REQUEST_RESTORE){
                new AlertDialog.Builder(this).setTitle("Daten wirklich ersetzen?")
                   .setMessage("Der Import ersetzt alle bisherigen Aufträge, Termine, Finanzen und Fotos auf diesem Gerät. Sichere deine bisherigen Daten vorher.")
                   .setNegativeButton("Abbrechen",null)
                   .setPositiveButton("Importieren",(d,w)->{
                       try{restoreBackup(uri);show(screen);toast("Sicherung importiert");}
                       catch(Exception ex){new AlertDialog.Builder(this).setTitle("Import fehlgeschlagen").setMessage(ex.getMessage()).setPositiveButton("OK",null).show();}
                   }).show();
            }
        }catch(Exception ex){new AlertDialog.Builder(this).setTitle("Aktion fehlgeschlagen").setMessage(ex.getMessage()).setPositiveButton("OK",null).show();}
    }
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
    @Override public void onBackPressed(){if(!screen.equals("Übersicht"))show("Übersicht");else super.onBackPressed();}
}
