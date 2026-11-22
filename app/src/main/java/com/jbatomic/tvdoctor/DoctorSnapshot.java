package com.jbatomic.tvdoctor;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.view.Display;
import android.view.WindowManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

public final class DoctorSnapshot {
    private DoctorSnapshot() {}
    private static void put(JSONObject o,String key,Object value){try{o.put(key,value);}catch(Exception ignored){}}
    public static JSONObject collect(Context context){
        JSONObject o=new JSONObject();
        android.content.SharedPreferences prefs=context.getSharedPreferences("doctor",Context.MODE_PRIVATE);
        put(o,"uiLanguage",prefs.getString("language","fr"));
        String panel=prefs.getString("panel_resolution","auto");
        int panelWidth="4k".equals(panel)?3840:"1080p".equals(panel)?1920:0;
        int panelHeight="4k".equals(panel)?2160:"1080p".equals(panel)?1080:0;
        put(o,"panelWidth",panelWidth);put(o,"panelHeight",panelHeight);
        put(o,"panelResolutionSource",panelWidth>0?"user":"unknown");
        put(o,"appVersion",BuildConfig.VERSION_NAME);
        put(o,"collectedAt",System.currentTimeMillis());
        put(o,"brand",Build.BRAND);put(o,"manufacturer",Build.MANUFACTURER);
        put(o,"model",Build.MODEL);put(o,"device",Build.DEVICE);
        put(o,"product",Build.PRODUCT);put(o,"board",Build.BOARD);
        put(o,"hardware",Build.HARDWARE);put(o,"fingerprint",Build.FINGERPRINT);
        put(o,"buildId",Build.ID);put(o,"buildType",Build.TYPE);
        put(o,"android",Build.VERSION.RELEASE);put(o,"sdk",Build.VERSION.SDK_INT);
        put(o,"securityPatch",Build.VERSION.SECURITY_PATCH);
        put(o,"soc",Build.VERSION.SDK_INT>=31?Build.SOC_MODEL:"Non exposé");
        put(o,"abis",new JSONArray(java.util.Arrays.asList(Build.SUPPORTED_ABIS)));
        put(o,"cpuCores",Runtime.getRuntime().availableProcessors());
        put(o,"uptimeSeconds",SystemClock.elapsedRealtime()/1000);
        put(o,"isTelevision",(context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION);
        try{
            ActivityManager am=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();am.getMemoryInfo(m);
            put(o,"ramTotal",m.totalMem);put(o,"ramAvailable",m.availMem);
            put(o,"ramThreshold",m.threshold);put(o,"ramLow",m.lowMemory);
            put(o,"memoryClassMb",am.getMemoryClass());
        }catch(Exception e){put(o,"ramError",e.getMessage());}
        try{
            StatFs s=new StatFs(Environment.getDataDirectory().getAbsolutePath());
            put(o,"storageTotal",s.getTotalBytes());put(o,"storageAvailable",s.getAvailableBytes());
            put(o,"storageUsed",s.getTotalBytes()-s.getAvailableBytes());
            put(o,"ownCacheBytes",folderSize(context.getCacheDir())+folderSize(context.getExternalCacheDir()));
        }catch(Exception e){put(o,"storageError",e.getMessage());}
        try{
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n=cm.getActiveNetwork();NetworkCapabilities c=cm.getNetworkCapabilities(n);
            String kind=c==null?"Hors ligne":c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet":c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile":"Autre";
            put(o,"network",kind);
            put(o,"internetValidated",c!=null&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            if(c!=null){put(o,"downKbps",c.getLinkDownstreamBandwidthKbps());put(o,"upKbps",c.getLinkUpstreamBandwidthKbps());}
            LinkProperties links=cm.getLinkProperties(n);
            if(links!=null){put(o,"dnsCount",links.getDnsServers().size());put(o,"interfaceName",links.getInterfaceName());}
            Enumeration<NetworkInterface> interfaces=NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()){
                NetworkInterface iface=interfaces.nextElement();
                if(!iface.isUp()||iface.isLoopback())continue;
                Enumeration<java.net.InetAddress> addrs=iface.getInetAddresses();
                while(addrs.hasMoreElements()){
                    java.net.InetAddress ip=addrs.nextElement();
                    if(ip instanceof Inet4Address&&ip.isSiteLocalAddress()){
                        put(o,"ipAddress",ip.getHostAddress());put(o,"macAddress","Masquée par Android");break;
                    }
                }
            }
        }catch(Exception e){put(o,"networkError",e.getMessage());}
        try{
            Display disp=((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point p=new Point();disp.getRealSize(p);
            put(o,"displayWidth",p.x);put(o,"displayHeight",p.y);
            // Android's window/rendering resolution is NOT necessarily the native panel resolution.
            put(o,"displayResolutionKind","Surface de rendu Android, pas résolution native de dalle");
            Display.Mode active=disp.getMode();
            put(o,"activeModeWidth",active.getPhysicalWidth());
            put(o,"activeModeHeight",active.getPhysicalHeight());
            int modeMaxWidth=0,modeMaxHeight=0;
            JSONArray modes=new JSONArray();
            for(Display.Mode mode:disp.getSupportedModes()){
                JSONObject item=new JSONObject();
                put(item,"width",mode.getPhysicalWidth());
                put(item,"height",mode.getPhysicalHeight());
                put(item,"hz",Math.round(mode.getRefreshRate()*100)/100.0);
                modes.put(item);
                long pixels=(long)mode.getPhysicalWidth()*mode.getPhysicalHeight();
                if(pixels>(long)modeMaxWidth*modeMaxHeight){modeMaxWidth=mode.getPhysicalWidth();modeMaxHeight=mode.getPhysicalHeight();}
            }
            put(o,"maxExposedModeWidth",modeMaxWidth);
            put(o,"maxExposedModeHeight",modeMaxHeight);
            put(o,"exposed4kMode",modeMaxWidth>=3840 && modeMaxHeight>=2160);
            put(o,"nativePanelResolution","Non déterminable par l'API Android publique");
            put(o,"supportedDisplayModes",modes);
            put(o,"refreshRateHz",Math.round(disp.getRefreshRate()*100)/100.0);
            put(o,"displayModes",disp.getSupportedModes().length);
            put(o,"displayHdrSupported",disp.getHdrCapabilities()!=null && disp.getHdrCapabilities().getSupportedHdrTypes().length>0);
        }catch(Exception e){put(o,"displayError",e.getMessage());}
        try{
            AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            put(o,"audioOutputCount",audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).length);
            JSONArray devices=new JSONArray();
            for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS))devices.put(d.getProductName().toString()+" · type "+d.getType());
            put(o,"audioOutputs",devices);
        }catch(Exception e){put(o,"audioError",e.getMessage());}
        try{
            MediaCodecList list=new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            int count=0;boolean av1=false,hevc=false,avc=false,vp9=false;
            JSONArray codecNames=new JSONArray();
            for(MediaCodecInfo codec:list.getCodecInfos()){
                if(codec.isEncoder())continue;
                count++;
                for(String t:codec.getSupportedTypes()){
                    if(t.equalsIgnoreCase("video/av01"))av1=true;
                    if(t.equalsIgnoreCase("video/hevc"))hevc=true;
                    if(t.equalsIgnoreCase("video/avc"))avc=true;
                    if(t.equalsIgnoreCase("video/x-vnd.on2.vp9"))vp9=true;
                }
                if(codecNames.length()<25)codecNames.put(codec.getName());
            }
            put(o,"decoderCount",count);put(o,"h264Decoder",avc);
            put(o,"hevcDecoder",hevc);put(o,"av1Decoder",av1);put(o,"vp9Decoder",vp9);
            put(o,"codecNames",codecNames);
        }catch(Exception e){put(o,"codecError",e.getMessage());}
        try{
            BatteryManager b=(BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
            int pct=b.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if(pct>=0&&pct<=100)put(o,"batteryPercent",pct);
        }catch(Exception ignored){}
        return o;
    }
    public static long folderSize(File f){
        if(f==null||!f.exists())return 0;
        if(f.isFile())return f.length();
        long size=0;File[] children=f.listFiles();
        if(children!=null)for(File item:children)size+=folderSize(item);
        return size;
    }
    public static String prettyBytes(long bytes){
        if(bytes<=0)return "0 B";
        String[] units={"B","Ko","Mo","Go","To"};
        double n=bytes;int i=0;
        while(n>=1024&&i<units.length-1){n/=1024;i++;}
        return String.format(Locale.FRANCE,"%.1f %s",n,units[i]);
    }
}
