package com.jbatomic.tvdoctor;

import android.content.Context;
import java.io.File;

public final class DoctorMaintenance {
    private DoctorMaintenance(){}
    // Only app-owned cache. Never traverse arbitrary storage, system directories, or other applications.
    public static long clearOwnCache(Context ctx){
        long total=0;
        total+=deleteChildren(ctx.getCacheDir());
        total+=deleteChildren(ctx.getExternalCacheDir());
        return total;
    }
    private static long deleteChildren(File folder){
        if(folder==null||!folder.isDirectory())return 0;
        long deleted=0;File[] files=folder.listFiles();
        if(files!=null)for(File f:files)deleted+=delete(f);
        return deleted;
    }
    private static long delete(File file){
        if(file==null||!file.exists())return 0;
        long sum=0;
        if(file.isDirectory()){
            File[] items=file.listFiles();
            if(items!=null)for(File child:items)sum+=delete(child);
        }else sum=file.length();
        return file.delete()?sum:0;
    }
}
