package com.jbatomic.tvdoctor;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Optional root maintenance. No arbitrary shell or user-provided command strings.
 * Every privileged action must originate in a TV-side confirmation dialog.
 */
public final class DoctorRoot {
    private static final Pattern PACKAGE = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+");
    private DoctorRoot() {}

    public static final class Result {
        public final boolean success;
        public final String output;
        Result(boolean ok, String message) { success=ok;output=message; }
    }

    private static Result shell(String command, int timeoutSeconds) {
        Process process=null;
        try {
            process=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
            if(!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Result(false,"Délai dépassé ou autorisation root non accordée.");
            }
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=process.getInputStream()) {
                byte[] buffer=new byte[1024]; int n;
                while((n=in.read(buffer))!=-1 && bytes.size()<8192) bytes.write(buffer,0,n);
            }
            String output=new String(bytes.toByteArray(), StandardCharsets.UTF_8).trim();
            return new Result(process.exitValue()==0,output.length()==0?"Code de sortie "+process.exitValue():output);
        } catch(Exception e) {
            return new Result(false,"ROOT indisponible : "+e.getClass().getSimpleName());
        } finally {
            if(process!=null)process.destroy();
        }
    }

    public static Result check() {
        Result result=shell("id -u",8);
        return new Result(result.success && "0".equals(result.output),result.success&&"0".equals(result.output)?"Autorisation root active":"ROOT non autorisé ou absent");
    }

    private static boolean allowedPackage(Context ctx,String pkg) {
        if(pkg==null||!PACKAGE.matcher(pkg).matches()||pkg.equals(ctx.getPackageName()))return false;
        try {
            ApplicationInfo info=ctx.getPackageManager().getApplicationInfo(pkg,0);
            return (info.flags & (ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0;
        } catch(PackageManager.NameNotFoundException e){return false;}
    }

    public static Result forceStop(Context ctx,String packageName) {
        if(!allowedPackage(ctx,packageName))return new Result(false,"Application système ou non autorisée.");
        return shell("am force-stop --user 0 "+packageName,10);
    }

    public static Result freeze(Context ctx,String packageName) {
        if(!allowedPackage(ctx,packageName))return new Result(false,"Seules les applications utilisateur peuvent être gelées.");
        return shell("pm disable-user --user 0 "+packageName,12);
    }

    public static Result unfreeze(String packageName) {
        if(packageName==null||!PACKAGE.matcher(packageName).matches())return new Result(false,"Nom de paquet invalide.");
        return shell("pm enable --user 0 "+packageName,12);
    }

    /**
     * Uses Android package manager cache maintenance (not pm clear).
     * May free little or nothing; never deletes an application's user data.
     */
    public static Result trimCaches() {
        return shell("pm trim-caches 16G",35);
    }
}
