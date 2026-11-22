package com.jbatomic.tvdoctor;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.content.Intent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DoctorFiles {
    private DoctorFiles(){}
    public static final class Entry {
        public final String id,name,mime;
        public final long size,lastModified;
        public final boolean directory,canDelete;
        public Entry(String id,String name,String mime,long size,long modified,int flags) {
            this.id=id;this.name=name;this.mime=mime;this.size=size;lastModified=modified;
            directory=DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
            canDelete=(flags & DocumentsContract.Document.FLAG_SUPPORTS_DELETE)!=0;
        }
    }
    public static List<Entry> list(Context context,Uri tree,String parentId) throws Exception {
        String[] projection={
          DocumentsContract.Document.COLUMN_DOCUMENT_ID,
          DocumentsContract.Document.COLUMN_DISPLAY_NAME,
          DocumentsContract.Document.COLUMN_MIME_TYPE,
          DocumentsContract.Document.COLUMN_SIZE,
          DocumentsContract.Document.COLUMN_LAST_MODIFIED,
          DocumentsContract.Document.COLUMN_FLAGS
        };
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parentId);
        ArrayList<Entry> entries=new ArrayList<>();
        ContentResolver resolver=context.getContentResolver();
        try(Cursor c=resolver.query(children,projection,null,null,null)) {
            if(c==null)throw new SecurityException("Ce dossier n'est pas accessible.");
            while(c.moveToNext() && entries.size()<500){
                String id=c.getString(0);
                String name=c.getString(1);
                String mime=c.getString(2);
                long size=c.isNull(3)?-1:c.getLong(3);
                long modified=c.isNull(4)?0:c.getLong(4);
                int flags=c.isNull(5)?0:c.getInt(5);
                entries.add(new Entry(id,name==null?"Sans nom":name,mime,size,modified,flags));
            }
        }
        Collections.sort(entries, (a,b)->{
            if(a.directory!=b.directory)return a.directory?-1:1;
            if(!a.directory&&a.size!=b.size)return Long.compare(b.size,a.size);
            return a.name.compareToIgnoreCase(b.name);
        });
        return entries;
    }
    public static Uri document(Uri tree,String id){
        return DocumentsContract.buildDocumentUriUsingTree(tree,id);
    }
    public static boolean delete(Context ctx,Uri tree,String rootId,Entry e)throws Exception {
        if(e==null||e.id.equals(rootId)||!e.canDelete)return false;
        return DocumentsContract.deleteDocument(ctx.getContentResolver(),document(tree,e.id));
    }
}
