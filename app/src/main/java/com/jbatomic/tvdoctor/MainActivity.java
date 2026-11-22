package com.jbatomic.tvdoctor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String PREFS = "doctor";
    private static final int BG = 0xff071322, PANEL = 0xff12263b, TEXT = 0xffe9f6ff, MUTED = 0xffa0c0d2, CYAN = 0xff00dce9;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DoctorWebServer web;
    private TextView status, info, link, updates;
    private EditText portField;
    private Button toggle, refresh, check;
    private boolean checking;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().getDecorView().setBackgroundColor(BG);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        web=new DoctorWebServer(this);
        createLayout();
        showDiagnostic();
        // Access is opt-in. Restarting the application never silently exposes the TV over LAN.
        updateWebState();
        long last=prefs.getLong("lastUpdateCheck",0);
        if(System.currentTimeMillis()-last>12L*60*60*1000) checkUpdates(false);
    }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(radius);return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setPadding(0,6,0,6);
        return t;
    }
    private Button button(String label){
        Button b=new Button(this);
        b.setText(label);b.setTextColor(TEXT);b.setAllCaps(false);b.setTextSize(15);
        b.setBackground(background(0xff1e425d,14));
        b.setPadding(16,8,16,8);
        b.setFocusable(true);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,54);
        p.rightMargin=12;p.topMargin=10;
        b.setLayoutParams(p);return b;
    }
    private void createLayout(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36,28,36,26);
        scroll.addView(root);
        TextView brand=text("◈  JB ATOMIC INDUSTRIES  /  ANDROID TV",13,CYAN,true);
        root.addView(brand);
        TextView title=text("ATOMIC TV Doctor",32,TEXT,true);
        root.addView(title);
        root.addView(text("Diagnostic système • portail PC • mises à jour officielles",16,MUTED,false));
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(22,18,22,16);panel.setBackground(background(PANEL,18));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=24;root.addView(panel,pp);
        panel.addView(text("ÉTAT DE L’APPAREIL",14,CYAN,true));
        info=text("Analyse…",16,TEXT,false);panel.addView(info);
        refresh=button("↻  Actualiser le diagnostic");panel.addView(refresh);
        refresh.setOnClickListener(v->showDiagnostic());
        LinearLayout webPanel=new LinearLayout(this);webPanel.setOrientation(LinearLayout.VERTICAL);
        webPanel.setPadding(22,18,22,16);webPanel.setBackground(background(PANEL,18));
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(-1,-2);wp.topMargin=16;root.addView(webPanel,wp);
        webPanel.addView(text("PORTAIL WEB LOCAL",14,CYAN,true));
        webPanel.addView(text("Le serveur est désactivé par défaut. Activez-le uniquement sur un réseau privé.",14,MUTED,false));
        LinearLayout settings=new LinearLayout(this);settings.setOrientation(LinearLayout.HORIZONTAL);
        settings.setGravity(Gravity.CENTER_VERTICAL);
        settings.addView(text("Port HTTP :  ",16,TEXT,true));
        portField=new EditText(this);
        portField.setSingleLine(true);portField.setInputType(InputType.TYPE_CLASS_NUMBER);
        portField.setText(String.valueOf(prefs.getInt("port",8766)));
        portField.setTextColor(TEXT);portField.setTextSize(18);
        portField.setSelectAllOnFocus(true);portField.setHintTextColor(MUTED);
        settings.addView(portField,new LinearLayout.LayoutParams(170,58));
        webPanel.addView(settings);
        toggle=button("Activer le portail");webPanel.addView(toggle);
        toggle.setOnClickListener(v->toggleWeb());
        status=text("",15,MUTED,false);webPanel.addView(status);
        link=text("",14,CYAN,false);link.setTextIsSelectable(true);webPanel.addView(link);
        LinearLayout updatesPanel=new LinearLayout(this);updatesPanel.setOrientation(LinearLayout.VERTICAL);
        updatesPanel.setPadding(22,18,22,16);updatesPanel.setBackground(background(PANEL,18));
        LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(-1,-2);up.topMargin=16;root.addView(updatesPanel,up);
        updatesPanel.addView(text("MISES À JOUR",14,CYAN,true));
        updates=text("Version "+BuildConfig.VERSION_NAME+" • canal GitHub Releases",15,TEXT,false);
        updatesPanel.addView(updates);
        check=button("Vérifier les mises à jour");updatesPanel.addView(check);
        check.setOnClickListener(v->checkUpdates(true));
        root.addView(text("JB Atomic Industries · https://jbatomic.com",13,MUTED,false));
        setContentView(scroll);
    }
    private void showDiagnostic(){
        JSONObject d=DoctorSnapshot.collect(this);
        String output=d.optString("manufacturer")+" "+d.optString("model")+"\n"+
            "Android "+d.optString("android")+" (API "+d.optInt("sdk")+")  •  "+d.optInt("cpuCores")+" cœurs\n"+
            "RAM : "+DoctorSnapshot.prettyBytes(d.optLong("ramAvailable"))+" disponibles / "+DoctorSnapshot.prettyBytes(d.optLong("ramTotal"))+"\n"+
            "Stockage : "+DoctorSnapshot.prettyBytes(d.optLong("storageAvailable"))+" libres / "+DoctorSnapshot.prettyBytes(d.optLong("storageTotal"))+"\n"+
            "Réseau : "+d.optString("network")+"  •  Internet : "+(d.optBoolean("internetValidated")?"oui":"non")+"\n"+
            "Décodage H.264 : "+yes(d,"h264Decoder")+"  • HEVC : "+yes(d,"hevcDecoder")+"  • AV1 : "+yes(d,"av1Decoder");
        info.setText(output);
    }
    private String yes(JSONObject o,String k){return o.optBoolean(k)?"oui":"non";}
    private void toggleWeb(){
        if(web.running()){web.stop();updateWebState();return;}
        int port;
        try {port=Integer.parseInt(portField.getText().toString());} catch(Exception e){status.setText("Port invalide (1024 à 65535).");return;}
        if(port<1024 || port>65535){status.setText("Port autorisé : 1024 à 65535.");return;}
        prefs.edit().putInt("port",port).apply();
        try {web.start(port);} catch(IOException e){status.setText("Impossible de démarrer : "+e.getMessage());return;}
        updateWebState();
    }
    private String localIp(){
        try {
            java.util.Enumeration<java.net.NetworkInterface> list=java.net.NetworkInterface.getNetworkInterfaces();
            while(list.hasMoreElements()){
                java.net.NetworkInterface iface=list.nextElement();
                if(!iface.isUp()||iface.isLoopback())continue;
                java.util.Enumeration<java.net.InetAddress> ips=iface.getInetAddresses();
                while(ips.hasMoreElements()){
                    java.net.InetAddress address=ips.nextElement();
                    if(address instanceof java.net.Inet4Address && address.isSiteLocalAddress())return address.getHostAddress();
                }
            }
        } catch(Exception ignored) {}
        return "IP_DE_LA_TV";
    }
    private void updateWebState(){
        boolean active=web.running();
        toggle.setText(active?"Désactiver le portail":"Activer le portail");
        portField.setEnabled(!active);
        status.setText(active?"Portail actif sur le réseau local · HTTP non chiffré":"Portail désactivé");
        link.setText(active?"Sur votre PC, ouvrez :\nhttp://"+localIp()+":"+web.port()+"/?key="+web.token()+"\n\nConservez ce lien privé : il donne accès aux informations de la TV.":"");
    }
    private void checkUpdates(boolean interactive){
        if(checking)return;
        checking=true;check.setEnabled(false);
        if(interactive)updates.setText("Recherche de la dernière version…");
        new Thread(()->{
            try {
                URL u=new URL("https://api.github.com/repos/"+BuildConfig.GITHUB_REPOSITORY+"/releases/latest");
                HttpURLConnection conn=(HttpURLConnection)u.openConnection();
                conn.setConnectTimeout(7000);conn.setReadTimeout(7000);
                conn.setRequestProperty("Accept","application/vnd.github+json");
                conn.setRequestProperty("User-Agent","AtomicTVDoctor/"+BuildConfig.VERSION_NAME);
                if(conn.getResponseCode()!=200) throw new IOException("Release indisponible ("+conn.getResponseCode()+")");
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream in=conn.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(bytes.size()+n>256000)throw new IOException("Réponse trop grande");bytes.write(buf,0,n);}}
                JSONObject release=new JSONObject(bytes.toString("UTF-8"));
                String tag=release.optString("tag_name");
                String newer=tag.startsWith("v")?tag.substring(1):tag;
                String current=BuildConfig.VERSION_NAME;
                String asset="";
                JSONArray items=release.optJSONArray("assets");
                if(items!=null)for(int i=0;i<items.length();i++){
                    JSONObject entry=items.optJSONObject(i);
                    if(entry!=null && entry.optString("name").equals("ATOMIC-TV-Doctor-AndroidTV.apk"))asset=entry.optString("browser_download_url");
                }
                final String download=asset, version=newer;
                prefs.edit().putLong("lastUpdateCheck",System.currentTimeMillis()).apply();
                handler.post(()->{
                    checking=false;check.setEnabled(true);
                    if(compareVersions(version,current)>0 && download.startsWith("https://github.com/"+BuildConfig.GITHUB_REPOSITORY+"/releases/download/")){
                        updates.setText("Version "+version+" disponible (installable après confirmation).");
                        check.setText("Télécharger et installer la v"+version);
                        check.setOnClickListener(v->downloadUpdate(download));
                    } else {
                        updates.setText("Version "+current+" · "+(compareVersions(version,current)>0?"APK officiel introuvable.":"Application à jour."));
                        check.setText("Vérifier les mises à jour");
                    }
                });
            } catch(Exception e) {handler.post(()->{checking=false;check.setEnabled(true);if(interactive)updates.setText("Vérification impossible : "+e.getMessage());});}
        },"doctor-updates").start();
    }
    private int compareVersions(String a,String b){
        try{
            String[] x=a.split("\\."),y=b.split("\\.");
            for(int i=0;i<3;i++){int xx=i<x.length?Integer.parseInt(x[i]):0,yy=i<y.length?Integer.parseInt(y[i]):0;if(xx!=yy)return Integer.compare(xx,yy);}
        }catch(Exception ignored){}
        return 0;
    }
    private void downloadUpdate(String remote){
        check.setEnabled(false);
        updates.setText("Téléchargement sécurisé depuis GitHub…");
        new Thread(()->{
            try{
                URL url=new URL(remote);
                if(!url.getHost().equalsIgnoreCase("github.com") || !url.getProtocol().equals("https"))throw new IOException("URL invalide");
                HttpURLConnection conn=(HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(10000);conn.setReadTimeout(20000);conn.setInstanceFollowRedirects(true);
                if(conn.getResponseCode()!=200)throw new IOException("HTTP "+conn.getResponseCode());
                File target=new File(getCacheDir(),"doctor-update.apk");
                long count=0;
                try(InputStream in=conn.getInputStream();FileOutputStream out=new FileOutputStream(target)){
                    byte[] buffer=new byte[16384];int n;
                    while((n=in.read(buffer))!=-1){count+=n;if(count>150_000_000)throw new IOException("APK trop volumineux");out.write(buffer,0,n);}
                }
                if(count<10000)throw new IOException("APK incomplet");
                handler.post(()->{
                    check.setEnabled(true);
                    updates.setText("APK téléchargé. Android doit confirmer l'installation.");
                    if(android.os.Build.VERSION.SDK_INT>=26 && !getPackageManager().canRequestPackageInstalls()){
                        Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));
                        startActivity(settings);
                        updates.append("\nAutorisez les installations pour cette application, puis relancez.");
                        return;
                    }
                    Intent install=new Intent(Intent.ACTION_VIEW);
                    install.setDataAndType(Uri.parse("content://com.jbatomic.tvdoctor.installer/update.apk"),"application/vnd.android.package-archive");
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(install);
                });
            }catch(Exception e){handler.post(()->{check.setEnabled(true);updates.setText("Téléchargement impossible : "+e.getMessage());});}
        },"doctor-download").start();
    }
    @Override protected void onDestroy(){web.stop();super.onDestroy();}
}
