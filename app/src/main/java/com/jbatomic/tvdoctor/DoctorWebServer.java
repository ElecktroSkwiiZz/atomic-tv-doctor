package com.jbatomic.tvdoctor;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DoctorWebServer {
    private static final int MAX_HEADER = 8192;
    private final Context context;
    private final ExecutorService workers = Executors.newFixedThreadPool(3);
    private ServerSocket server;
    private Thread acceptThread;
    private String token;

    public DoctorWebServer(Context context) { this.context = context.getApplicationContext(); }
    public synchronized boolean running() { return server != null && !server.isClosed(); }
    public synchronized int port() { return running() ? server.getLocalPort() : -1; }
    public synchronized String token() { return token; }

    public synchronized void start(int requestedPort) throws IOException {
        if(requestedPort < 1024 || requestedPort > 65535) throw new IOException("Choose port 1024–65535");
        stop();
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        StringBuilder hex = new StringBuilder();
        for(byte b:random) hex.append(String.format(Locale.US, "%02x", b & 0xff));
        token = hex.toString();
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress("0.0.0.0", requestedPort), 8);
        final ServerSocket current = server;
        acceptThread = new Thread(() -> {
            while(!current.isClosed()) {
                try {
                    Socket s = current.accept();
                    s.setSoTimeout(3500);
                    workers.submit(() -> handle(s));
                } catch(IOException e) { if(!current.isClosed()) android.util.Log.w("TVDoctor", "Web socket", e); }
            }
        }, "doctor-http-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public synchronized void stop() {
        if(server != null) {try{server.close();}catch(IOException ignored){}}
        server=null; token=null;
    }

    private void handle(Socket socket) {
        try(Socket s=socket) {
            InputStream in=s.getInputStream();
            OutputStream out=s.getOutputStream();
            ByteArrayOutputStream header=new ByteArrayOutputStream();
            int c;
            while((c=in.read())!=-1) {
                if(header.size()>=MAX_HEADER) {send(out,431,"text/plain","Header too long");return;}
                header.write(c);
                byte[] h=header.toByteArray();
                int n=h.length;
                if(n>=4 && h[n-4]==13 && h[n-3]==10 && h[n-2]==13 && h[n-1]==10)break;
            }
            String request=header.toString("ISO-8859-1");
            String[] lines=request.split("\r\n");
            if(lines.length==0) {send(out,400,"text/plain","Bad request");return;}
            String[] first=lines[0].split(" ");
            if(first.length!=3 || (!"GET".equals(first[0]) && !"POST".equals(first[0]))) {send(out,405,"text/plain","GET or POST only");return;}
            String host="";
            String origin="";
            String requestedAction="";
            for(String line:lines) {
                String low=line.toLowerCase(Locale.US);
                if(low.startsWith("host:"))host=line.substring(5).trim();
                else if(low.startsWith("origin:"))origin=line.substring(7).trim();
                else if(low.startsWith("x-atomic-action:"))requestedAction=line.substring(16).trim();
            }
            // Reject non-IP literal Host headers to mitigate browser DNS rebinding.
            if(!validHost(host)) {send(out,403,"text/plain","Invalid host");return;}
            URI url;
            try {url=new URI(first[1]);} catch(Exception e){send(out,400,"text/plain","Bad URL");return;}
            String path=url.getPath();
            if(path==null) path="/";
            if(path.equals("/favicon.svg")) {asset(out,"doctor-icon.svg","image/svg+xml");return;}
            if(path.equals("/doctor.css")) {asset(out,"doctor.css","text/css; charset=utf-8");return;}
            if(path.equals("/doctor.js")) {asset(out,"doctor.js","application/javascript; charset=utf-8");return;}
            String provided="";
            String query=url.getRawQuery();
            if(query!=null) for(String term:query.split("&")) if(term.startsWith("key="))provided=term.substring(4);
            String currentToken;
            synchronized(this) { currentToken=token; }
            if(currentToken==null || !constantEqual(currentToken,provided)) {
                send(out,403,"text/html; charset=utf-8","<h1>Access denied</h1><p>Use the private URL shown on your TV.</p>");return;
            }
            if(path.equals("/api/clean-cache") && "POST".equals(first[0])) {
                String expectedOrigin="http://"+host;
                if(!expectedOrigin.equalsIgnoreCase(origin)||!"clean-own-cache".equals(requestedAction)) {send(out,403,"text/plain","Action forbidden");return;}
                long bytes=DoctorMaintenance.clearOwnCache(context);
                send(out,200,"application/json; charset=utf-8","{\"cleanedBytes\":"+bytes+"}");return;
            }
            if(!"GET".equals(first[0])){send(out,405,"text/plain","Only GET for this route");return;}
            if(path.equals("/api/apps")){
                send(out,200,"application/json; charset=utf-8",DoctorApps.list(context).toString());return;
            }
            if(path.equals("/api/files")){
                org.json.JSONObject result=new org.json.JSONObject();
                String saved=context.getSharedPreferences("doctor",Context.MODE_PRIVATE).getString("folder_uri",null);
                try{
                    if(saved==null){result.put("authorized",false);result.put("items",new org.json.JSONArray());}
                    else{
                        android.net.Uri folder=android.net.Uri.parse(saved);
                        java.util.List<DoctorFiles.Entry> list=DoctorFiles.list(context,folder,android.provider.DocumentsContract.getTreeDocumentId(folder));
                        org.json.JSONArray items=new org.json.JSONArray();
                        for(DoctorFiles.Entry f:list){
                            org.json.JSONObject item=new org.json.JSONObject();
                            item.put("name",f.name);
                            item.put("directory",f.directory);
                            item.put("size",f.size);
                            items.put(item);
                        }
                        result.put("authorized",true);result.put("items",items);
                    }
                }catch(Exception e){result.put("authorized",false);result.put("error","Accès au dossier indisponible");result.put("items",new org.json.JSONArray());}
                send(out,200,"application/json; charset=utf-8",result.toString());return;
            }
            if(path.equals("/api/status")) {
                send(out,200,"application/json; charset=utf-8",DoctorSnapshot.collect(context).toString());return;
            }
            if(path.equals("/")) {asset(out,"doctor.html","text/html; charset=utf-8");return;}
            send(out,404,"text/plain","Not found");
        } catch(Exception e) {android.util.Log.w("TVDoctor","Request failed",e);}
    }

    private boolean validHost(String host) {
        if(host==null || host.isEmpty())return false;
        String value=host;
        int colon=value.lastIndexOf(':');
        if(colon>=0)value=value.substring(0,colon);
        if(value.equals("localhost") || value.equals("127.0.0.1")) return true;
        if(value.length()>15 || !value.matches("[0-9.]+"))return false;
        try {
            InetAddress address=InetAddress.getByName(value);
            return address instanceof Inet4Address && (address.isSiteLocalAddress() || address.isLoopbackAddress());
        } catch(Exception e){return false;}
    }
    private static boolean constantEqual(String a,String b){
        byte[] x=a.getBytes(StandardCharsets.US_ASCII),y=b.getBytes(StandardCharsets.US_ASCII);
        int diff=x.length^y.length;
        for(int i=0;i<Math.max(x.length,y.length);i++)diff|=(i<x.length?x[i]:0)^(i<y.length?y[i]:0);
        return diff==0;
    }
    private void asset(OutputStream out,String name,String mime) throws IOException {
        try(InputStream asset=context.getAssets().open(name); ByteArrayOutputStream b=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096]; int n;
            while((n=asset.read(buffer))!=-1)b.write(buffer,0,n);
            sendBytes(out,200,mime,b.toByteArray());
        }
    }
    private void send(OutputStream out,int status,String mime,String value)throws IOException{
        sendBytes(out,status,mime,value.getBytes(StandardCharsets.UTF_8));
    }
    private void sendBytes(OutputStream out,int status,String mime,byte[] data)throws IOException{
        String reason=status==200?"OK":status==403?"Forbidden":status==404?"Not Found":status==405?"Method Not Allowed":status==431?"Request Header Fields Too Large":"Bad Request";
        String headers="HTTP/1.1 "+status+" "+reason+"\r\nContent-Type: "+mime+"\r\nContent-Length: "+data.length+"\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'\r\nConnection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));out.write(data);out.flush();
    }
}
