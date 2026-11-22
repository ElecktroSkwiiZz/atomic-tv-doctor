package com.jbatomic.tvdoctor;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Visible launchable applications. Android may hide services and non-launcher packages. */
public final class DoctorApps {
    private DoctorApps(){}
    public static JSONArray list(Context context){
        PackageManager pm=context.getPackageManager();
        Map<String,JSONObject> values=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
            Intent filter=new Intent(Intent.ACTION_MAIN);filter.addCategory(category);
            for(ResolveInfo ri:pm.queryIntentActivities(filter,0)){
                if(ri.activityInfo==null || ri.activityInfo.packageName==null)continue;
                try{
                    String pkg=ri.activityInfo.packageName;
                    JSONObject item=new JSONObject();
                    item.put("packageName",pkg);
                    item.put("label",String.valueOf(ri.loadLabel(pm)));
                    item.put("system",(ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0);
                    values.put(pkg,item);
                }catch(Exception ignored){}
            }
        }
        JSONArray arr=new JSONArray();
        for(JSONObject o:values.values()){
            if(arr.length()>=160)break;
            arr.put(o);
        }
        return arr;
    }
}
