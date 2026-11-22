package com.jbatomic.tvdoctor;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import org.json.JSONObject;
import java.io.File;
import java.util.Locale;

public final class DoctorSnapshot {
    private DoctorSnapshot() {}

    public static JSONObject collect(Context context) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("brand", Build.BRAND);
            obj.put("manufacturer", Build.MANUFACTURER);
            obj.put("model", Build.MODEL);
            obj.put("device", Build.DEVICE);
            obj.put("android", Build.VERSION.RELEASE);
            obj.put("sdk", Build.VERSION.SDK_INT);
            obj.put("hardware", Build.HARDWARE);
            obj.put("soc", Build.VERSION.SDK_INT >= 31 ? Build.SOC_MODEL : "Unavailable");
            obj.put("uptimeSeconds", SystemClock.elapsedRealtime() / 1000);
            obj.put("appVersion", BuildConfig.VERSION_NAME);
            obj.put("cpuCores", Runtime.getRuntime().availableProcessors());
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mem);
            obj.put("ramTotal", mem.totalMem);
            obj.put("ramAvailable", mem.availMem);
            obj.put("ramLow", mem.lowMemory);
            File data = Environment.getDataDirectory();
            StatFs stat = new StatFs(data.getAbsolutePath());
            obj.put("storageTotal", stat.getTotalBytes());
            obj.put("storageAvailable", stat.getAvailableBytes());
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            obj.put("network", nc == null ? "Offline" : nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ? "Ethernet" : nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi" : nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "Cellular" : "Other");
            obj.put("internetValidated", nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            android.media.MediaCodecList codecs = new android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS);
            int count = 0;
            boolean av1 = false, hevc = false, avc = false;
            for(android.media.MediaCodecInfo codec : codecs.getCodecInfos()) {
                if (codec.isEncoder()) continue;
                count++;
                for(String type : codec.getSupportedTypes()) {
                    if(type.equalsIgnoreCase("video/av01")) av1 = true;
                    if(type.equalsIgnoreCase("video/hevc")) hevc = true;
                    if(type.equalsIgnoreCase("video/avc")) avc = true;
                }
            }
            obj.put("decoderCount", count);
            obj.put("av1Decoder", av1);
            obj.put("hevcDecoder", hevc);
            obj.put("h264Decoder", avc);
            obj.put("collectedAt", System.currentTimeMillis());
        } catch (Exception e) { try {obj.put("error", e.toString());} catch(Exception ignored) {} }
        return obj;
    }

    public static String prettyBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "GB", "TB"};
        double n = bytes;
        int unit = 0;
        while(n >= 1024 && unit < units.length - 1) { n /= 1024.0; unit++; }
        return String.format(Locale.US, "%.1f %s", n, units[unit]);
    }
}
