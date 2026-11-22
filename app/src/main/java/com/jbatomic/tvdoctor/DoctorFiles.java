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
    public static final class LocalEntry {
        public final java.io.File file;
        public final String name;
        public final boolean directory;
        public final long size;
        LocalEntry(java.io.File f){
            file=f;name=f.getName();directory=f.isDirectory();size=directory?-1:f.length();
        }
    }
    private static java.io.File sharedRoot() throws java.io.IOException {
        return android.os.Environment.getExternalStorageDirectory().getCanonicalFile();
    }
    public static java.io.File publicFolder(java.io.File directory)throws java.io.IOException {
        java.io.File root=sharedRoot();
        java.io.File value=directory.getCanonicalFile();
        String base=root.getCanonicalPath();
        String path=value.getCanonicalPath();
        if(!path.equals(base)&&!path.startsWith(base+java.io.File.separator))
            throw new SecurityException("Chemin hors du stockage partagé.");
        String relative=path.substring(base.length());
        if(relative.equals("/Android")||relative.startsWith("/Android/"))
            throw new SecurityException("Répertoire Android système non autorisé.");
        return value;
    }
    public static java.io.File sharedBase()throws java.io.IOException{return sharedRoot();}
    public static List<LocalEntry> listLocal(java.io.File requested)throws Exception{
        java.io.File folder=publicFolder(requested);
        if(!folder.isDirectory())throw new java.io.IOException("Ce dossier est inaccessible.");
        java.io.File[] files=folder.listFiles();
        if(files==null)throw new java.io.IOException("Impossible de lire le dossier.");
        ArrayList<LocalEntry> result=new ArrayList<>();
        for(java.io.File file:files){
            try{
                publicFolder(file);
                if(result.size()<500)result.add(new LocalEntry(file));
            }catch(Exception ignored){}
        }
        Collections.sort(result,(a,b)->{
            if(a.directory!=b.directory)return a.directory?-1:1;
            if(!a.directory&&a.size!=b.size)return Long.compare(b.size,a.size);
            return a.name.compareToIgnoreCase(b.name);
        });
        return result;
    }
    /** Bounded traversal: protects low-memory Android TVs from excessive filesystem scans. */
    public static List<LocalEntry> largestFiles()throws Exception{
        java.io.File root=sharedRoot();
        java.util.ArrayDeque<java.io.File> queue=new java.util.ArrayDeque<>();
        queue.add(root);
        java.util.PriorityQueue<LocalEntry> top=new java.util.PriorityQueue<>(
            40,java.util.Comparator.comparingLong(entry->entry.size));
        int visited=0;
        while(!queue.isEmpty()&&visited<4500){
            java.io.File folder=queue.removeFirst();
            java.io.File[] files=folder.listFiles();
            if(files==null)continue;
            for(java.io.File file:files){
                if(++visited>4500)break;
                try{
                    java.io.File safe=publicFolder(file);
                    if(safe.isDirectory()){
                        if(queue.size()<1200)queue.addLast(safe);
                    }else if(safe.isFile()&&safe.length()>0){
                        LocalEntry entry=new LocalEntry(safe);
                        if(top.size()<35)top.offer(entry);
                        else if(entry.size>top.peek().size){top.poll();top.offer(entry);}
                    }
                }catch(Exception ignored){}
            }
        }
        ArrayList<LocalEntry> result=new ArrayList<>(top);
        result.sort((a,b)->Long.compare(b.size,a.size));
        return result;
    }
    /** Only individual files and empty folders can be deleted. Never recursively delete. */
    public static boolean deleteLocal(java.io.File selected)throws Exception{
        java.io.File file=publicFolder(selected);
        if(file.equals(sharedRoot()))return false;
        return file.exists()&&file.delete();
    }
    public static boolean delete(Context ctx,Uri tree,String rootId,Entry e)throws Exception {
        if(e==null||e.id.equals(rootId)||!e.canDelete)return false;
        return DocumentsContract.deleteDocument(ctx.getContentResolver(),document(tree,e.id));
    }
}
