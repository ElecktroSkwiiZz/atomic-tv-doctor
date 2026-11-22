package com.jbatomic.tvdoctor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BG=0xff080f20, PANEL=0xff14233a, TEXT=0xfff1f6ff;
    private static final int MUTED=0xff9fb3cb, CYAN=0xff39d9ee, BLUE=0xff6fa7ff;
    private static final int PURPLE=0xffad93ff, GREEN=0xff69e0b5, ORANGE=0xffffb273;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private DoctorWebServer web;
    private LinearLayout page;
    private TextView status, link, updates;
    private EditText portField;
    private Button toggle, check;
    private boolean checking=false;
    private Boolean rootAuthorized=null;
    private boolean rootChecking=false;
    private JSONObject snapshot=new JSONObject();

    private static final int DASH=0, APPS=1, CLEAN=2, TECH=3, PORTAL=4, RELEASES=5;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("doctor",MODE_PRIVATE);
        web=new DoctorWebServer(this);
        showPage(DASH);
        if(System.currentTimeMillis()-prefs.getLong("lastUpdateCheck",0)>12L*60*60*1000)checkUpdates(false);
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable surface(int[] shades,int border,float radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,shades);
        d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);
        return d;
    }
    private TextView label(String value,int size,int color,boolean strong) {
        TextView v=new TextView(this);
        v.setText(value);v.setTextSize(size);v.setTextColor(color);
        v.setFontFeatureSettings("kern");v.setIncludeFontPadding(true);
        if(strong)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return v;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void padding(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    private void focus(View v){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},surface(new int[]{0xff1c4a6d,0xff1e3e6c},CYAN,12));
        states.addState(new int[]{},surface(new int[]{0xff1a3557,0xff182c49},0xff335377,12));
        v.setBackground(states);v.setFocusable(true);v.setFocusableInTouchMode(false);
        v.setOnFocusChangeListener((view,on)->{
            view.animate().scaleX(on?1.016f:1f).scaleY(on?1.016f:1f).setDuration(110).start();
            view.setElevation(on?dp(12):0);
        });
    }
    private Button button(String title){
        Button b=new Button(this);
        b.setText(title);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(TEXT);b.setMinHeight(0);b.setMinimumHeight(0);
        padding(b,7,0);focus(b);return b;
    }
    private void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void title(LinearLayout parent,String headline,String description){
        TextView t=label(headline,20,TEXT,true);parent.addView(t);
        if(description!=null){TextView d=label(description,12,MUTED,false);parent.addView(d);}
        gap(parent,9);
    }
    private LinearLayout panel(String name,int accent){
        LinearLayout c=column();padding(c,16,12);
        c.setBackground(surface(new int[]{PANEL,0xff111e32},0xff304663,16));
        TextView head=label(name.toUpperCase(Locale.FRANCE),11,accent,true);
        head.setLetterSpacing(.11f);c.addView(head);
        gap(c,5);return c;
    }
    private void metric(LinearLayout parent,String content,int accent){
        TextView t=label(content,21,accent,true);t.setMaxLines(2);parent.addView(t);
    }
    private void sub(LinearLayout parent,String value){
        TextView t=label(value,12,MUTED,false);t.setMaxLines(3);parent.addView(t);
    }
    private void place(LinearLayout line,LinearLayout card){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
        p.setMargins(dp(5),dp(4),dp(5),dp(4));line.addView(card,p);
    }
    private void showPage(int n){
        LinearLayout root=column();root.setBackgroundColor(BG);
        padding(root,24,11);
        setContentView(root);

        LinearLayout header=row();
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_doctor);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams image=new LinearLayout.LayoutParams(dp(39),dp(39));
        image.rightMargin=dp(10);header.addView(icon,image);
        TextView brand=label("ATOMIC",22,TEXT,true);header.addView(brand);
        TextView product=label(" TV Doctor",21,CYAN,true);header.addView(product);
        View spacer=new View(this);header.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        TextView publisher=label("JB ATOMIC INDUSTRIES    /    V "+BuildConfig.VERSION_NAME,10,MUTED,true);
        header.addView(publisher);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(41)));

        LinearLayout navigation=row();
        String[] tabs={"Vue d'ensemble","Applications","Maintenance","Technique","Portail PC","Mises à jour"};
        for(int i=0;i<tabs.length;i++){
            int dest=i;Button b=button(tabs[i]);
            b.setTextColor(i==n?CYAN:TEXT);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(41),1);
            p.setMargins(dp(3),0,dp(3),0);navigation.addView(b,p);
            b.setOnClickListener(v->showPage(dest));
            if(i==n)b.setContentDescription("Onglet actif : "+tabs[i]);
        }
        LinearLayout.LayoutParams navLp=new LinearLayout.LayoutParams(-1,dp(41));
        navLp.topMargin=dp(6);root.addView(navigation,navLp);
        gap(root,7);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(true);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        page=column();page.setClipChildren(false);page.setClipToPadding(false);
        page.setPadding(dp(2),dp(2),dp(2),dp(8));
        scroll.addView(page,new ScrollView.LayoutParams(-1,-2));

        if(n==DASH)dashboard();
        else if(n==APPS)applications();
        else if(n==CLEAN)maintenance();
        else if(n==TECH)technical();
        else if(n==PORTAL)portal();
        else releases();
    }
    private void snapshot(){snapshot=DoctorSnapshot.collect(this);}
    private String bytes(long b){return DoctorSnapshot.prettyBytes(b);}
    private String v(String key){return snapshot.optString(key,"Indisponible");}
    private String yes(String key){return snapshot.optBoolean(key)?"Oui":"Non";}
    private LinearLayout gaugeCard(String label,long value,long total,int tint){
        LinearLayout card=panel(label,tint);
        LinearLayout content=row();content.setGravity(Gravity.CENTER_VERTICAL);card.addView(content);
        DoctorGauge ring=new DoctorGauge(this);
        ring.setPercent(total>0?100f*value/total:0);
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(79),dp(79));
        gp.rightMargin=dp(12);content.addView(ring,gp);
        LinearLayout details=column();content.addView(details);
        metric(details,bytes(total-value)+" libres",TEXT);
        sub(details,"Capacité : "+bytes(total));
        sub(details,Math.round(100d*value/Math.max(1,total))+" % utilisés");
        return card;
    }
    private void dashboard(){
        snapshot();
        LinearLayout intro=row();
        TextView heading=label("Vue d'ensemble",20,TEXT,true);
        intro.addView(heading,new LinearLayout.LayoutParams(0,dp(33),1));
        TextView stamp=label("MESURES LOCALES  •  "+v("network"),11,GREEN,true);
        intro.addView(stamp);
        page.addView(intro);
        LinearLayout line1=row();
        page.addView(line1,new LinearLayout.LayoutParams(-1,dp(139)));
        LinearLayout hardware=panel("Système",CYAN);
        metric(hardware,v("model"),TEXT);
        sub(hardware,"Android "+v("android")+"  •  API "+snapshot.optInt("sdk"));
        sub(hardware,snapshot.optInt("cpuCores")+" cœurs  •  "+v("board"));
        place(line1,hardware);
        long ram=snapshot.optLong("ramTotal"),free=snapshot.optLong("ramAvailable");
        place(line1,gaugeCard("Mémoire RAM",ram-free,ram,PURPLE));
        long disk=snapshot.optLong("storageTotal"),available=snapshot.optLong("storageAvailable");
        place(line1,gaugeCard("Stockage interne",disk-available,disk,GREEN));

        LinearLayout line2=row();
        page.addView(line2,new LinearLayout.LayoutParams(-1,dp(139)));
        LinearLayout display=panel("Vidéo et affichage",BLUE);
        metric(display,snapshot.optInt("displayWidth")+" × "+snapshot.optInt("displayHeight"),TEXT);
        sub(display,"Surface Android  •  "+snapshot.optDouble("refreshRateHz")+" Hz");
        String modes=snapshot.optBoolean("exposed4kMode")?"Mode 4K exposé":"Dalle 4K : non déterminée";
        sub(display,modes+"  •  HDR : "+yes("displayHdrSupported"));
        place(line2,display);
        LinearLayout net=panel("Réseau",GREEN);
        metric(net,v("network"),TEXT);
        sub(net,"IP locale : "+v("ipAddress"));
        sub(net,"Internet : "+(snapshot.optBoolean("internetValidated")?"actif":"non confirmé"));
        place(line2,net);
        LinearLayout codecs=panel("Décodage multimédia",PURPLE);
        metric(codecs,"H.264 "+flag("h264Decoder")+"   HEVC "+flag("hevcDecoder"),TEXT);
        sub(codecs,"AV1 "+flag("av1Decoder")+"   VP9 "+flag("vp9Decoder"));
        sub(codecs,snapshot.optInt("decoderCount")+" décodeurs déclarés par Android");
        place(line2,codecs);

        gap(page,6);
        LinearLayout actions=row();page.addView(actions,new LinearLayout.LayoutParams(-1,dp(44)));
        navButton(actions,"↻  Actualiser",()->showPage(DASH));
        navButton(actions,"◈  Applications",()->showPage(APPS));
        navButton(actions,"✦  Maintenance",()->showPage(CLEAN));
        navButton(actions,"≡  Rapport",()->showPage(TECH));
        navButton(actions,"◉  Portail PC",()->showPage(PORTAL));
        gap(page,7);
        TextView hint=label("Les jauges indiquent la RAM et le stockage utilisés. Les modes vidéo Android ne prouvent pas la résolution native de la dalle.",11,MUTED,false);
        page.addView(hint);
    }
    private String flag(String key){return snapshot.optBoolean(key)?"✓":"—";}
    private void navButton(LinearLayout line,String text,Runnable work){
        Button b=button(text);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
        lp.setMargins(dp(4),0,dp(4),0);line.addView(b,lp);b.setOnClickListener(v->work.run());
    }
    private void confirm(String title,String warning,Runnable action){
        new AlertDialog.Builder(this).setTitle(title).setMessage(warning)
            .setNegativeButton("Annuler",null)
            .setPositiveButton("Confirmer",(dialog,which)->action.run()).show();
    }
    private void toast(String text){new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK",null).show();}
    private void doClean(){
        confirm("Nettoyer le cache de Doctor",
            "Seuls les fichiers temporaires appartenant à ATOMIC TV Doctor seront supprimés. Les autres applications et leurs données restent intactes.",
            ()->{long count=DoctorMaintenance.clearOwnCache(this);toast(bytes(count)+" supprimés.");});
    }
    private void badge(LinearLayout parent){
        LinearLayout badge=row();
        ImageView icon=new ImageView(this);
        icon.setImageResource(R.drawable.ic_root_required);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(20),dp(20));
        ip.rightMargin=dp(6);badge.addView(icon,ip);
        TextView text=label("ROOT REQUIS",10,ORANGE,true);badge.addView(text);
        parent.addView(badge);
    }
    private void checkRoot(TextView output){
        if(rootChecking)return;
        rootChecking=true;output.setText("Recherche de l'autorisation ROOT…");
        new Thread(()->{
            DoctorRoot.Result result=DoctorRoot.check();
            handler.post(()->{
                rootChecking=false;rootAuthorized=result.success;
                output.setText(result.success?"ROOT AUTORISÉ  ✓":"ROOT non disponible ou refusé");
            });
        },"doctor-check-root").start();
    }
    private void rootAction(String title,String note,java.util.concurrent.Callable<DoctorRoot.Result> work,Runnable onSuccess){
        if(!Boolean.TRUE.equals(rootAuthorized)){
            toast("Cette fonction nécessite les droits ROOT. Vérifie d'abord les autorisations ROOT sur ce téléviseur. Les fonctions standards restent utilisables.");
            return;
        }
        confirm(title,note+"\n\nCette action est exécutée avec les droits ROOT, uniquement sur ta confirmation.",()->{
            new Thread(()->{
                DoctorRoot.Result result;
                try{result=work.call();}catch(Exception e){result=new DoctorRoot.Result(false,e.getMessage());}
                final DoctorRoot.Result finished=result;
                handler.post(()->{
                    toast((finished.success?"Opération terminée.":"Opération refusée ou échouée.")+"\n"+finished.output);
                    if(finished.success && onSuccess!=null)onSuccess.run();
                });
            },"doctor-root-operation").start();
        });
    }
    private void maintenance(){
        snapshot();
        title(page,"Maintenance","Outils intégrés, résultats mesurables et opérations à confirmer. Pas de booster fictif.");
        LinearLayout first=row();page.addView(first,new LinearLayout.LayoutParams(-1,dp(151)));
        LinearLayout normal=panel("NETTOYAGE STANDARD",GREEN);
        metric(normal,"Nettoyage sûr",TEXT);
        sub(normal,"Cache de Doctor : "+bytes(snapshot.optLong("ownCacheBytes")));
        gap(normal,6);
        Button clean=button("Nettoyer les fichiers temporaires");normal.addView(clean,new LinearLayout.LayoutParams(-1,dp(43)));
        clean.setOnClickListener(v->doClean());
        place(first,normal);
        LinearLayout apps=panel("APPLICATIONS",CYAN);
        metric(apps,"Gérer depuis Doctor",TEXT);
        sub(apps,"Ouvrir, examiner et désinstaller une application.");
        gap(apps,6);
        Button show=button("Gestionnaire des applications");apps.addView(show,new LinearLayout.LayoutParams(-1,dp(43)));
        show.setOnClickListener(v->showPage(APPS));
        place(first,apps);
        LinearLayout report=panel("RAPPORT ET DIAGNOSTIC",BLUE);
        metric(report,"Analyse détaillée",TEXT);
        sub(report,"CPU, Android, stockage, réseau, codecs, vidéo, audio.");
        gap(report,6);
        Button export=button("Exporter le rapport JSON");report.addView(export,new LinearLayout.LayoutParams(-1,dp(43)));
        export.setOnClickListener(v->shareReport());
        place(first,report);

        gap(page,12);
        LinearLayout headline=row();
        TextView h=label("Outils avancés",19,TEXT,true);headline.addView(h);
        View filler=new View(this);headline.addView(filler,new LinearLayout.LayoutParams(0,1,1));
        TextView rootStatus=label(rootAuthorized==null?"ROOT non vérifié":rootAuthorized?"ROOT AUTORISÉ":"ROOT INDISPONIBLE",12,ORANGE,true);
        headline.addView(rootStatus);
        page.addView(headline);
        sub(page,"Ces commandes n'apparaissent que dans Doctor et nécessitent une validation explicite. Elles ne modifient pas les fichiers personnels.");
        gap(page,9);
        LinearLayout advanced=row();page.addView(advanced,new LinearLayout.LayoutParams(-1,dp(163)));
        LinearLayout test=panel("ACCÈS PRIVILÉGIÉ",ORANGE);
        badge(test);gap(test,5);
        sub(test,"Détecte et demande l'autorisation auprès de ton gestionnaire ROOT (ex. Magisk).");
        gap(test,8);
        Button verify=button("Vérifier les droits ROOT");test.addView(verify,new LinearLayout.LayoutParams(-1,dp(42)));
        verify.setOnClickListener(v->checkRoot(rootStatus));
        place(advanced,test);
        LinearLayout cache=panel("CACHES ANDROID",ORANGE);
        badge(cache);gap(cache,5);
        sub(cache,"Demande au gestionnaire de paquets Android de purger les caches récupérables, sans effacer les données.");
        gap(cache,8);
        Button trim=button("Purger les caches système");cache.addView(trim,new LinearLayout.LayoutParams(-1,dp(42)));
        trim.setOnClickListener(v->rootAction("Purger les caches Android","Commande : pm trim-caches 16G. Le gain dépend de l'espace récupérable ; certains caches seront reconstruits.",DoctorRoot::trimCaches,()->showPage(CLEAN)));
        place(advanced,cache);
        LinearLayout stop=panel("CONTRÔLE D'APPLICATIONS",ORANGE);
        badge(stop);gap(stop,5);
        sub(stop,"Arrêter ou geler une application utilisateur. Bouton de réactivation prévu pour les apps gelées.");
        gap(stop,8);
        Button manage=button("Choisir une application");stop.addView(manage,new LinearLayout.LayoutParams(-1,dp(42)));
        manage.setOnClickListener(v->showPage(APPS));
        place(advanced,stop);
    }
    private void shareReport(){
        snapshot();
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("application/json");
        send.putExtra(Intent.EXTRA_TEXT,snapshot.toString(2));
        try{startActivity(Intent.createChooser(send,"Exporter le diagnostic"));}catch(Exception e){toast("Aucune application ne prend en charge le partage sur cette TV.");}
    }
    private static final class AppEntry{
        String label,pkg;boolean system;
        AppEntry(String l,String p,boolean s){label=l;pkg=p;system=s;}
    }
    private void applications(){
        title(page,"Gestionnaire d'applications","Actions depuis Doctor. Désinstallation soumise à la confirmation Android ; opérations ROOT signalées.");
        LinearLayout searchRow=row();page.addView(searchRow,new LinearLayout.LayoutParams(-1,dp(45)));
        TextView status=label("Chargement des applications accessibles…",12,MUTED,false);
        searchRow.addView(status,new LinearLayout.LayoutParams(0,-2,1));
        Button root=button("Vérifier ROOT");
        searchRow.addView(root,new LinearLayout.LayoutParams(dp(160),dp(41)));
        root.setOnClickListener(v->checkRoot(status));

        Set<String> frozen=prefs.getStringSet("frozen_packages",Collections.emptySet());
        if(!frozen.isEmpty()){
            LinearLayout frozenCard=panel("APPLICATIONS GELÉES — RÉACTIVATION",ORANGE);
            for(String pkg:new HashSet<>(frozen)){
                Button restore=button("ROOT requis  •  Réactiver "+pkg);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(40));p.topMargin=dp(6);
                frozenCard.addView(restore,p);
                restore.setOnClickListener(v->rootAction("Réactiver "+pkg,"Cette opération restaure l'application gelée.",()->DoctorRoot.unfreeze(pkg),()->{
                    Set<String> changed=new HashSet<>(prefs.getStringSet("frozen_packages",Collections.emptySet()));
                    changed.remove(pkg);prefs.edit().putStringSet("frozen_packages",changed).apply();showPage(APPS);
                }));
            }
            page.addView(frozenCard);gap(page,8);
        }
        PackageManager pm=getPackageManager();
        Map<String,AppEntry> found=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
            Intent filter=new Intent(Intent.ACTION_MAIN);filter.addCategory(category);
            for(ResolveInfo ri:pm.queryIntentActivities(filter,0)){
                if(ri.activityInfo==null)continue;
                String pkg=ri.activityInfo.packageName;
                if(pkg==null||pkg.equals(getPackageName()))continue;
                boolean sys=(ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0;
                found.put(pkg,new AppEntry(String.valueOf(ri.loadLabel(pm)),pkg,sys));
            }
        }
        status.setText(found.size()+" applications visibles par Android • Liste des lanceurs");
        int shown=0;
        for(AppEntry entry:found.values()){
            if(shown++>=140)break;
            LinearLayout item=row();item.setPadding(dp(12),dp(5),dp(12),dp(5));
            item.setBackground(surface(new int[]{0xff14233c,0xff101b30},0xff2b3f5d,12));
            LinearLayout details=column();item.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            TextView name=label(entry.label,14,TEXT,true);
            details.addView(name);
            TextView pkg=label(entry.pkg+(entry.system?"  ·  système":"  ·  utilisateur"),10,MUTED,false);
            details.addView(pkg);
            Button open=button("Ouvrir");item.addView(open,new LinearLayout.LayoutParams(dp(88),dp(41)));
            open.setOnClickListener(v->{
                Intent intent=pm.getLaunchIntentForPackage(entry.pkg);
                if(intent==null)toast("Aucune activité de lancement accessible.");
                else try{startActivity(intent);}catch(Exception e){toast("Ouverture non autorisée.");}
            });
            if(!entry.system){
                Button uninstall=button("Supprimer");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(100),dp(41));
                bp.leftMargin=dp(7);item.addView(uninstall,bp);
                uninstall.setOnClickListener(v->confirm("Désinstaller "+entry.label,"Android demandera une confirmation avant la désinstallation.",()->{
                    try{Intent i=new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+entry.pkg));startActivity(i);}
                    catch(Exception e){toast("Désinstallation impossible sur cette TV.");}
                }));
                Button stop=button("ROOT · Arrêter");
                LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(122),dp(41));sp.leftMargin=dp(7);
                item.addView(stop,sp);stop.setTextColor(ORANGE);
                stop.setOnClickListener(v->rootAction("Arrêter "+entry.label,"Force-stop d'une application utilisateur. Les tâches en cours seront interrompues.",()->DoctorRoot.forceStop(this,entry.pkg),null));
                Button freeze=button("ROOT · Geler");
                LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(110),dp(41));fp.leftMargin=dp(7);
                item.addView(freeze,fp);freeze.setTextColor(ORANGE);
                freeze.setOnClickListener(v->rootAction("Geler "+entry.label,"L'application sera désactivée pour l'utilisateur 0. Ne l'utilise pas sur des services importants.",()->DoctorRoot.freeze(this,entry.pkg),()->{
                    Set<String> updated=new HashSet<>(prefs.getStringSet("frozen_packages",Collections.emptySet()));
                    updated.add(entry.pkg);prefs.edit().putStringSet("frozen_packages",updated).apply();showPage(APPS);
                }));
            }
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(66));p.topMargin=dp(7);
            page.addView(item,p);
        }
        if(shown==0){sub(page,"Aucune application trouvée. La visibilité Android peut limiter la liste.");}
    }
    private void field(LinearLayout p,String name,String value){
        LinearLayout line=row();line.setPadding(0,dp(4),0,dp(4));
        TextView key=label(name,11,MUTED,false);line.addView(key,new LinearLayout.LayoutParams(0,-2,0.85f));
        TextView valueText=label(value,12,TEXT,true);valueText.setGravity(Gravity.END);
        valueText.setMaxLines(3);
        line.addView(valueText,new LinearLayout.LayoutParams(0,-2,1.15f));
        p.addView(line);
        View rule=new View(this);rule.setBackgroundColor(0xff263852);p.addView(rule,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private void technical(){
        snapshot();
        title(page,"Diagnostic technique","Valeurs interprétées et unités correctes ; aucune supposition sur la définition native de la dalle.");
        LinearLayout leftRight=row();
        page.addView(leftRight,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout system=panel("SYSTÈME ET SOC",CYAN);
        field(system,"Modèle",v("manufacturer")+" "+v("model"));
        field(system,"Android",v("android")+" / API "+snapshot.optInt("sdk"));
        field(system,"Correctif",v("securityPatch"));
        field(system,"SoC / carte",v("soc")+" / "+v("board"));
        field(system,"CPU",snapshot.optInt("cpuCores")+" cœurs  •  "+v("hardware"));
        field(system,"ABI",String.valueOf(snapshot.opt("abis")));
        field(system,"Version firmware",v("buildId"));
        field(system,"Temps allumé",(snapshot.optLong("uptimeSeconds")/3600)+" h");
        leftRight.addView(system,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout mem=panel("RAM ET STOCKAGE",PURPLE);
        field(mem,"Mémoire installée",bytes(snapshot.optLong("ramTotal")));
        field(mem,"RAM disponible",bytes(snapshot.optLong("ramAvailable")));
        field(mem,"Seuil Android",bytes(snapshot.optLong("ramThreshold")));
        field(mem,"RAM basse",snapshot.optBoolean("ramLow")?"Oui":"Non");
        field(mem,"Mémoire par app",snapshot.optInt("memoryClassMb")+" Mo");
        field(mem,"Stockage total",bytes(snapshot.optLong("storageTotal")));
        field(mem,"Stockage libre",bytes(snapshot.optLong("storageAvailable")));
        field(mem,"Cache Doctor",bytes(snapshot.optLong("ownCacheBytes")));
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1);right.leftMargin=dp(12);
        leftRight.addView(mem,right);
        gap(page,11);
        LinearLayout row2=row();page.addView(row2);
        LinearLayout video=panel("AFFICHAGE ET AUDIO",BLUE);
        field(video,"Surface Android",snapshot.optInt("displayWidth")+" × "+snapshot.optInt("displayHeight"));
        field(video,"Mode Android actif",snapshot.optInt("activeModeWidth")+" × "+snapshot.optInt("activeModeHeight"));
        field(video,"Fréquence",v("refreshRateHz")+" Hz");
        field(video,"Mode max exposé",snapshot.optInt("maxExposedModeWidth")+" × "+snapshot.optInt("maxExposedModeHeight"));
        field(video,"Dalle native","Inconnue : Android ne l'expose pas nécessairement");
        field(video,"HDR API",yes("displayHdrSupported"));
        field(video,"Sorties audio",String.valueOf(snapshot.opt("audioOutputs")));
        row2.addView(video,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout network=panel("RÉSEAU & CODECS",GREEN);
        field(network,"Type",v("network"));
        field(network,"IP locale",v("ipAddress"));
        field(network,"Interface",v("interfaceName"));
        field(network,"Internet",snapshot.optBoolean("internetValidated")?"Validé":"Non confirmé");
        field(network,"Estimation lien ↓",snapshot.optLong("downKbps")+" kbit/s (pas un speedtest)");
        field(network,"Estimation lien ↑",snapshot.optLong("upKbps")+" kbit/s (pas un speedtest)");
        field(network,"H.264 / HEVC",flag("h264Decoder")+" / "+flag("hevcDecoder"));
        field(network,"VP9 / AV1",flag("vp9Decoder")+" / "+flag("av1Decoder"));
        LinearLayout.LayoutParams nr=new LinearLayout.LayoutParams(0,-2,1);nr.leftMargin=dp(12);row2.addView(network,nr);
        gap(page,11);
        Button export=button("Partager le rapport JSON complet");page.addView(export,new LinearLayout.LayoutParams(-1,dp(45)));
        export.setOnClickListener(v->shareReport());
    }
    private void shareReport(){
        snapshot();
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("application/json");
        send.putExtra(Intent.EXTRA_TEXT,snapshot.toString());
        try{startActivity(Intent.createChooser(send,"Exporter le diagnostic"));}
        catch(Exception e){toast("Aucune application de partage disponible sur ce téléviseur.");}
    }
    private void portal(){
        title(page,"Portail PC","Tableau de bord web en réseau local. Port modifiable et URL privée renouvelée à chaque activation.");
        LinearLayout box=panel("CONNEXION LOCALE",CYAN);
        page.addView(box);
        LinearLayout input=row();gap(box,10);box.addView(input);
        input.addView(label("Port HTTP",14,TEXT,true));
        portField=new EditText(this);portField.setSingleLine(true);portField.setTextSize(18);
        portField.setInputType(InputType.TYPE_CLASS_NUMBER);
        portField.setTextColor(TEXT);portField.setSelectAllOnFocus(true);
        portField.setText(String.valueOf(prefs.getInt("port",8766)));
        portField.setBackground(surface(new int[]{0xff1b3453,0xff1b3453},CYAN,12));
        portField.setPadding(dp(14),0,dp(14),0);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(145),dp(49));pp.leftMargin=dp(16);
        input.addView(portField,pp);
        gap(box,12);
        toggle=button("Activer le portail PC");
        box.addView(toggle,new LinearLayout.LayoutParams(dp(260),dp(46)));
        toggle.setOnClickListener(v->toggleWeb());
        gap(box,10);
        status=label("",15,ORANGE,true);box.addView(status);
        link=label("",14,GREEN,false);link.setTextIsSelectable(true);box.addView(link);
        gap(box,11);
        sub(box,"Le portail HTTP ne doit être activé que sur un réseau privé de confiance. N'expose pas ce port sur Internet.");
        updateWebState();
    }
    private void toggleWeb(){
        if(web.running()){web.stop();updateWebState();return;}
        int port;
        try{port=Integer.parseInt(portField.getText().toString().trim());}catch(Exception e){toast("Port invalide.");return;}
        if(port<1024||port>65535){toast("Le port doit être compris entre 1024 et 65535.");return;}
        prefs.edit().putInt("port",port).apply();
        try{web.start(port);}catch(IOException e){toast("Impossible d'activer : "+e.getMessage());return;}
        updateWebState();
    }
    private void updateWebState(){
        if(toggle==null||status==null||link==null||portField==null)return;
        boolean active=web.running();
        toggle.setText(active?"Désactiver le portail":"Activer le portail PC");
        portField.setEnabled(!active);
        status.setText(active?"● PORTAIL ACTIF":"○ PORTAIL ARRÊTÉ");
        String ip=DoctorSnapshot.collect(this).optString("ipAddress","IP_NON_DISPONIBLE");
        link.setText(active?"Ouvrir sur le PC (URL privée) :\nhttp://"+ip+":"+web.port()+"/?key="+web.token():"Le serveur ne reçoit aucune connexion.");
    }
    private void releases(){
        title(page,"Mises à jour","Versions officielles publiées sur GitHub Releases.");
        LinearLayout c=panel("CANAL STABLE",PURPLE);page.addView(c);
        metric(c,"Version installée : "+BuildConfig.VERSION_NAME,TEXT);
        gap(c,10);
        updates=label("Vérifie si une version signée plus récente est disponible.",14,MUTED,false);
        c.addView(updates);gap(c,10);
        check=button("Vérifier les mises à jour");c.addView(check,new LinearLayout.LayoutParams(dp(300),dp(47)));
        check.setOnClickListener(v->checkUpdates(true));
        gap(c,8);
        sub(c,"L'installation nécessite une confirmation Android. La signature de la version publiée doit correspondre à celle de l'application installée.");
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
        if(check!=null)check.setEnabled(false);
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
    @Override protected void onDestroy(){if(web!=null)web.stop();super.onDestroy();}
}
