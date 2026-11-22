package com.jbatomic.tvdoctor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;

public final class MainActivity extends Activity {
    private static final int BG=0xff071321, PANEL=0xff12253b, TEXT=0xffedf8ff, MUTED=0xffa2bacb, CYAN=0xff00e0ed;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private DoctorWebServer web;
    private LinearLayout root, page, nav;
    private TextView status, link, updates;
    private EditText portField;
    private Button toggle, check;
    private boolean checking=false;
    private JSONObject snapshot;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        prefs=getSharedPreferences("doctor",MODE_PRIVATE);
        web=new DoctorWebServer(this);
        showPage(0);
        if(System.currentTimeMillis()-prefs.getLong("lastUpdateCheck",0)>12L*60*60*1000)checkUpdates(false);
    }
    private GradientDrawable bg(int color,int stroke){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(16);
        d.setStroke(stroke==CYAN?3:1,stroke);
        return d;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private void focus(View v){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},bg(0xff205069,CYAN));
        states.addState(new int[]{},bg(0xff19364b,0xff36536a));
        v.setBackground(states);v.setFocusable(true);
        v.setOnFocusChangeListener((view,focused)->{
            view.animate().scaleX(focused?1.035f:1f).scaleY(focused?1.035f:1f).setDuration(110).start();
            view.setElevation(focused?dp(12):0);
        });
    }
    private Button button(String label){
        Button b=new Button(this);
        b.setText(label);b.setTextSize(13);b.setTextColor(TEXT);b.setAllCaps(false);
        b.setPadding(dp(10),dp(4),dp(10),dp(4));b.setMinHeight(dp(40));focus(b);
        return b;
    }
    private LinearLayout vertical(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private LinearLayout row(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;
    }
    private void showPage(int selected){
        root=vertical();root.setPadding(dp(22),dp(14),dp(22),dp(12));root.setBackgroundColor(BG);
        setContentView(root);
        LinearLayout header=row();
        TextView title=text("◈  ATOMIC  TV Doctor",24,TEXT,true);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(45),1));
        TextView version=text("JB ATOMIC INDUSTRIES    ·    v"+BuildConfig.VERSION_NAME,12,CYAN,true);
        header.addView(version);
        root.addView(header);
        nav=row();
        String[] tabs={"Tableau de bord","Maintenance","Diagnostic","Portail PC","Mises à jour"};
        for(int i=0;i<tabs.length;i++){
            final int n=i;Button b=button(tabs[i]);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));
            nav.addView(b,p);b.setOnClickListener(v->showPage(n));
            if(i==selected)b.setTextColor(CYAN);
        }
        root.addView(nav);
        page=vertical();
        LinearLayout.LayoutParams content=new LinearLayout.LayoutParams(-1,0,1);content.topMargin=dp(10);
        root.addView(page,content);
        if(selected==0) dashboard();
        else if(selected==1) maintenance();
        else if(selected==2) diagnostic();
        else if(selected==3) portal();
        else updatesPage();
    }
    private LinearLayout card(String heading){
        LinearLayout c=vertical();
        c.setPadding(dp(17),dp(13),dp(17),dp(12));
        c.setBackground(bg(PANEL,0xff2d435a));
        TextView h=text(heading.toUpperCase(java.util.Locale.FRANCE),12,CYAN,true);
        c.addView(h);return c;
    }
    private void addCard(LinearLayout row,LinearLayout c){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
        p.setMargins(dp(5),dp(5),dp(5),dp(5));row.addView(c,p);
    }
    private TextView metric(LinearLayout parent,String v){
        TextView t=text(v,21,TEXT,true);t.setPadding(0,dp(6),0,dp(2));parent.addView(t);return t;
    }
    private void caption(LinearLayout parent,String v){parent.addView(text(v,12,MUTED,false));}
    private void refresh(){snapshot=DoctorSnapshot.collect(this);}
    private void dashboard(){
        refresh();
        JSONObject d=snapshot;
        long total=d.optLong("ramTotal"),available=d.optLong("ramAvailable");
        long storage=d.optLong("storageTotal"),free=d.optLong("storageAvailable");
        LinearLayout first=row();page.addView(first,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout device=card("Appareil");
        metric(device,d.optString("model","TV Android"));
        caption(device,"Android "+d.optString("android")+"  •  "+d.optInt("cpuCores")+" cœurs  •  API "+d.optInt("sdk"));
        caption(device,"Carte : "+d.optString("board"));
        addCard(first,device);
        LinearLayout memory=card("Mémoire RAM");
        LinearLayout gaugeLine=row();memory.addView(gaugeLine);
        DoctorGauge ramGauge=new DoctorGauge(this);
        ramGauge.setPercent(total>0?100f*(total-available)/total:0);
        gaugeLine.addView(ramGauge,new LinearLayout.LayoutParams(dp(100),dp(100)));
        LinearLayout memoryText=vertical();gaugeLine.addView(memoryText);
        metric(memoryText,DoctorSnapshot.prettyBytes(available));
        caption(memoryText,"libres sur "+DoctorSnapshot.prettyBytes(total));
        addCard(first,memory);
        LinearLayout second=row();page.addView(second,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout disk=card("Stockage");
        LinearLayout diskLine=row();disk.addView(diskLine);
        DoctorGauge diskGauge=new DoctorGauge(this);
        diskGauge.setPercent(storage>0?100f*(storage-free)/storage:0);
        diskLine.addView(diskGauge,new LinearLayout.LayoutParams(dp(100),dp(100)));
        LinearLayout diskText=vertical();diskLine.addView(diskText);
        metric(diskText,DoctorSnapshot.prettyBytes(free));
        caption(diskText,"libres sur "+DoctorSnapshot.prettyBytes(storage));
        addCard(second,disk);
        LinearLayout network=card("Réseau");
        metric(network,d.optString("network"));
        caption(network,d.optString("ipAddress","IP non disponible"));
        caption(network,"Internet : "+(d.optBoolean("internetValidated")?"Connecté":"Non confirmé"));
        addCard(second,network);
        LinearLayout third=row();page.addView(third,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout codecs=card("Décodage vidéo");
        metric(codecs,"H.264 "+flag(d,"h264Decoder")+"    HEVC "+flag(d,"hevcDecoder"));
        caption(codecs,"AV1 "+flag(d,"av1Decoder")+"   ·   VP9 "+flag(d,"vp9Decoder")+"   ·   "+d.optInt("decoderCount")+" décodeurs");
        addCard(third,codecs);
        LinearLayout display=card("Affichage & système");
        metric(display,d.optInt("displayWidth")+" × "+d.optInt("displayHeight"));
        caption(display,d.optDouble("refreshRateHz")+" Hz  •  HDR détecté : "+flag(d,"displayHdrSupported"));
        caption(display,"Démarrée depuis "+(d.optLong("uptimeSeconds")/3600)+" h");
        addCard(third,display);
        LinearLayout actions=row();actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(44));ap.topMargin=dp(6);page.addView(actions,ap);
        action(actions,"↻ Actualiser",()->showPage(0));
        action(actions,"⚙ Maintenance",()->showPage(1));
        action(actions,"≡ Détails",()->showPage(2));
        action(actions,"◉ Portail PC",()->showPage(3));
    }
    private String flag(JSONObject d,String key){return d.optBoolean(key)?"✓":"—";}
    private void action(LinearLayout container,String title,Runnable work){
        Button b=button(title);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
        p.setMargins(dp(4),0,dp(4),0);container.addView(b,p);b.setOnClickListener(v->work.run());
    }
    private void maintenance(){
        TextView heading=text("Maintenance et gestion des applications",21,TEXT,true);page.addView(heading);
        caption(page,"Actions Android réelles, sans promesse de « boost » artificiel. Chaque opération sensible nécessite ton accord.");
        LinearLayout grid=row();LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,1);p.topMargin=dp(16);page.addView(grid,p);
        LinearLayout storage=card("Stockage & caches");addCard(grid,storage);
        metric(storage,"Libérer de l'espace");
        caption(storage,"Cache de TV Doctor : "+DoctorSnapshot.prettyBytes(DoctorSnapshot.folderSize(getCacheDir())+DoctorSnapshot.folderSize(getExternalCacheDir())));
        Button clean=button("Nettoyer le cache de TV Doctor");
        storage.addView(clean);clean.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Nettoyage du cache").setMessage("Supprimer uniquement les fichiers temporaires de TV Doctor ? Les données des autres applications ne seront pas touchées.").setNegativeButton("Annuler",null).setPositiveButton("Nettoyer",(d,w)->{long bytes=DoctorMaintenance.clearOwnCache(this);new AlertDialog.Builder(this).setMessage("Nettoyé : "+DoctorSnapshot.prettyBytes(bytes)).setPositiveButton("OK",(x,y)->showPage(1)).show();}).show());
        Button storageSettings=button("Gérer le stockage Android");storage.addView(storageSettings);
        storageSettings.setOnClickListener(v->openSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        LinearLayout apps=card("Applications installées");addCard(grid,apps);
        metric(apps,"Gérer les applications");
        caption(apps,"Désinstallation, permissions, stockage et cache par application via les paramètres système.");
        Button manage=button("Ouvrir la liste des applications");apps.addView(manage);
        manage.setOnClickListener(v->openSettings(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        Button own=button("Informations de TV Doctor");apps.addView(own);
        own.setOnClickListener(v->{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()));try{startActivity(i);}catch(Exception e){message(e.getMessage());}});
        LinearLayout bottom=row();LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,0,1);bp.topMargin=dp(10);page.addView(bottom,bp);
        LinearLayout net=card("Réseau et diagnostic");addCard(bottom,net);
        metric(net,"Contrôler la connexion");
        caption(net,"Accès direct aux réglages réseau et à la collecte des informations.");
        Button network=button("Réglages réseau");net.addView(network);
        network.setOnClickListener(v->openSettings(Settings.ACTION_WIFI_SETTINGS));
        LinearLayout optim=card("Performances");addCard(bottom,optim);
        metric(optim,"Diagnostic, pas de miracle");
        caption(optim,"Android gère la RAM et le cache automatiquement. Éviter les fermetures forcées de services système.");
        Button refresh=button("Actualiser les mesures");optim.addView(refresh);refresh.setOnClickListener(v->showPage(0));
    }
    private void message(String msg){new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK",null).show();}
    private void openSettings(String action){try{startActivity(new Intent(action));}catch(Exception e){message("Réglage indisponible sur cette TV.");}}
    private void diagnostic(){
        refresh();JSONObject d=snapshot;
        ScrollView sc=new ScrollView(this);
        page.addView(sc,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout content=vertical();sc.addView(content);
        TextView heading=text("Rapport technique détaillé",22,TEXT,true);content.addView(heading);
        caption(content,"Toutes les valeurs proviennent des API système disponibles, sans privilèges root.");
        String[][] groups={
            {"Système","manufacturer","model","device","product","board","hardware","soc","android","sdk","securityPatch","buildId","buildType","fingerprint","abis","cpuCores","uptimeSeconds"},
            {"Mémoire et stockage","ramTotal","ramAvailable","ramThreshold","ramLow","memoryClassMb","storageTotal","storageUsed","storageAvailable","ownCacheBytes"},
            {"Réseau","network","ipAddress","interfaceName","internetValidated","downKbps","upKbps","dnsCount"},
            {"Affichage et audio","displayWidth","displayHeight","refreshRateHz","displayModes","displayHdrSupported","audioOutputCount","audioOutputs"},
            {"Décodage","decoderCount","h264Decoder","hevcDecoder","vp9Decoder","av1Decoder","codecNames"}
        };
        for(String[] group:groups){
            LinearLayout block=card(group[0]);LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,-2);margin.topMargin=dp(9);content.addView(block,margin);
            for(int i=1;i<group.length;i++){
                String key=group[i];Object value=d.opt(key);
                if(value!=null){TextView line=text(key+" : "+value.toString(),13,TEXT,false);line.setPadding(0,dp(2),0,dp(2));block.addView(line);}
            }
        }
    }
    private void portal(){
        LinearLayout block=card("Portail web local pour PC");
        page.addView(block,new LinearLayout.LayoutParams(-1,-2));
        metric(block,"Connexion à distance sur ton réseau privé");
        caption(block,"Interface web accessible depuis un PC, un téléphone ou une tablette. Port modifiable, jeton privé.");
        LinearLayout line=row();block.addView(line);
        line.addView(text("Port HTTP   ",15,TEXT,true));
        portField=new EditText(this);portField.setSingleLine(true);
        portField.setInputType(InputType.TYPE_CLASS_NUMBER);
        portField.setText(String.valueOf(prefs.getInt("port",8766)));portField.setSelectAllOnFocus(true);
        portField.setTextColor(TEXT);portField.setTextSize(18);
        focus(portField);line.addView(portField,new LinearLayout.LayoutParams(dp(155),dp(48)));
        toggle=button("Activer le portail");block.addView(toggle,new LinearLayout.LayoutParams(dp(230),dp(48)));
        toggle.setOnClickListener(v->toggleWeb());
        status=text("",15,MUTED,false);block.addView(status);
        link=text("",13,CYAN,false);link.setTextIsSelectable(true);block.addView(link);
        updateWebState();
    }
    private void toggleWeb(){
        if(web.running()){web.stop();updateWebState();return;}
        int port;
        try{port=Integer.parseInt(portField.getText().toString());}catch(Exception e){status.setText("Port invalide.");return;}
        if(port<1024||port>65535){status.setText("Port autorisé : 1024 à 65535.");return;}
        prefs.edit().putInt("port",port).apply();
        try{web.start(port);}catch(IOException e){status.setText("Erreur : "+e.getMessage());return;}
        updateWebState();
    }
    private void updateWebState(){
        if(toggle==null||status==null||link==null||portField==null)return;
        boolean running=web.running();
        toggle.setText(running?"Désactiver le portail":"Activer le portail");
        portField.setEnabled(!running);
        status.setText(running?"Portail activé (HTTP sur réseau local)":"Portail désactivé");
        String ip=DoctorSnapshot.collect(this).optString("ipAddress","IP_DE_LA_TV");
        link.setText(running?"Adresse privée à saisir sur le PC :\nhttp://"+ip+":"+web.port()+"/?key="+web.token():"");
    }
    private void updatesPage(){
        LinearLayout block=card("Mises à jour officielles");page.addView(block,new LinearLayout.LayoutParams(-1,-2));
        metric(block,"Version "+BuildConfig.VERSION_NAME);
        updates=text("Dernières versions recherchées dans GitHub Releases",15,TEXT,false);
        block.addView(updates);
        check=button("Vérifier les mises à jour");block.addView(check,new LinearLayout.LayoutParams(dp(270),dp(48)));
        check.setOnClickListener(v->checkUpdates(true));
    }
    private void checkUpdates(boolean interactive){
        if(checking)return;
        checking=true;if(check!=null)check.setEnabled(false);
        if(interactive&&updates!=null)updates.setText("Recherche de la dernière version…");
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
                    checking=false;if(check!=null)check.setEnabled(true);
                    if(compareVersions(version,current)>0 && download.startsWith("https://github.com/"+BuildConfig.GITHUB_REPOSITORY+"/releases/download/")){
                        if(updates!=null)updates.setText("Version "+version+" disponible (installable après confirmation).");
                        if(check!=null)check.setText("Télécharger et installer la v"+version);
                        if(check!=null)check.setOnClickListener(v->downloadUpdate(download));
                    } else {
                        if(updates!=null)updates.setText("Version "+current+" · "+(compareVersions(version,current)>0?"APK officiel introuvable.":"Application à jour."));
                        if(check!=null)check.setText("Vérifier les mises à jour");
                    }
                });
            } catch(Exception e) {handler.post(()->{checking=false;if(check!=null)check.setEnabled(true);if(interactive)if(updates!=null)updates.setText("Vérification impossible : "+e.getMessage());});}
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
        if(updates!=null)updates.setText("Téléchargement sécurisé depuis GitHub…");
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
                    if(check!=null)check.setEnabled(true);
                    if(updates!=null)updates.setText("APK téléchargé. Android doit confirmer l'installation.");
                    if(android.os.Build.VERSION.SDK_INT>=26 && !getPackageManager().canRequestPackageInstalls()){
                        Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));
                        startActivity(settings);
                        if(updates!=null)updates.append("\nAutorisez les installations pour cette application, puis relancez.");
                        return;
                    }
                    Intent install=new Intent(Intent.ACTION_VIEW);
                    install.setDataAndType(Uri.parse("content://com.jbatomic.tvdoctor.installer/update.apk"),"application/vnd.android.package-archive");
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(install);
                });
            }catch(Exception e){handler.post(()->{if(check!=null)check.setEnabled(true);if(updates!=null)updates.setText("Téléchargement impossible : "+e.getMessage());});}
        },"doctor-download").start();
    }
    @Override protected void onDestroy(){web.stop();super.onDestroy();}
}
