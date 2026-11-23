package com.jbatomic.tvdoctor;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Scoped-storage compatible gallery of indexed shared media.
 * This does not claim full filesystem access or reveal private app data.
 */
public final class DoctorMedia {
    private DoctorMedia(){}

    public static final class Item {
        public final Uri uri;
        public final String name,folder,mime;
        public final long size,modified;
        Item(Uri uri,String name,String folder,String mime,long size,long modified){
            this.uri=uri;this.name=name;this.folder=folder;this.mime=mime;
            this.size=size;this.modified=modified;
        }
    }

    public static List<Item> list(Context context,boolean largestFirst,int limit) throws Exception {
        Uri collection=MediaStore.Files.getContentUri("external");
        ArrayList<String> projection=new ArrayList<>();
        projection.add(MediaStore.Files.FileColumns._ID);
        projection.add(MediaStore.Files.FileColumns.DISPLAY_NAME);
        projection.add(MediaStore.Files.FileColumns.SIZE);
        projection.add(MediaStore.Files.FileColumns.MIME_TYPE);
        projection.add(MediaStore.Files.FileColumns.DATE_MODIFIED);
        if(Build.VERSION.SDK_INT>=29) projection.add(MediaStore.MediaColumns.RELATIVE_PATH);
        String where=MediaStore.Files.FileColumns.MEDIA_TYPE+" IN ("
            +MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE+","
            +MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO+","
            +MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO+")";
        String sort=largestFirst?MediaStore.Files.FileColumns.SIZE+" DESC":
            MediaStore.Files.FileColumns.DISPLAY_NAME+" COLLATE NOCASE ASC";
        ArrayList<Item> result=new ArrayList<>();
        ContentResolver resolver=context.getContentResolver();
        try(Cursor cursor=resolver.query(collection,projection.toArray(new String[0]),where,null,sort)){
            if(cursor==null)throw new IllegalStateException("Catalogue multimédia inaccessible sur cette TV");
            int indexId=cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            int indexName=cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int indexSize=cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int indexMime=cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
            int indexModified=cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int indexFolder=Build.VERSION.SDK_INT>=29?
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH):-1;
            while(cursor.moveToNext() && result.size()<Math.max(1,Math.min(500,limit))){
                long id=cursor.getLong(indexId);
                String name=cursor.isNull(indexName)?"Sans nom":cursor.getString(indexName);
                String folder=indexFolder<0||cursor.isNull(indexFolder)?"":cursor.getString(indexFolder);
                String mime=cursor.isNull(indexMime)?"application/octet-stream":cursor.getString(indexMime);
                long size=cursor.isNull(indexSize)?-1:cursor.getLong(indexSize);
                long date=cursor.isNull(indexModified)?0:cursor.getLong(indexModified);
                result.add(new Item(ContentUris.withAppendedId(collection,id),name,folder,mime,size,date));
            }
        }
        return result;
    }
}
