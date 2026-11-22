package com.jbatomic.tvdoctor;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;

/** Grants read-only temporary access to files selected in Doctor's built-in explorer. */
public final class DoctorSharedFileProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private File safe(Uri uri)throws FileNotFoundException{
        try{
            String relative=uri.getQueryParameter("path");
            if(relative==null||relative.isEmpty()||relative.startsWith("/")||relative.contains(".."))
                throw new SecurityException("Invalid path");
            return DoctorFiles.publicFolder(new File(DoctorFiles.sharedBase(),relative));
        }catch(Exception e){throw new FileNotFoundException("File unavailable");}
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException("Read-only");
        File file=safe(uri);
        if(!file.isFile())throw new FileNotFoundException("Not a file");
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri){
        try{
            String name=safe(uri).getName();
            String extension=MimeTypeMap.getFileExtensionFromUrl(Uri.encode(name));
            String mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(java.util.Locale.ROOT));
            return mime==null?"application/octet-stream":mime;
        }catch(Exception e){return "application/octet-stream";}
    }
    @Override public Cursor query(Uri uri,String[] p,String s,String[] a,String order){return null;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException();}
}
