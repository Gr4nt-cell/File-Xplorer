package com.gr4nt.filexplorer;
import java.io.*;import java.util.concurrent.*;
/** Explicit, user-enabled su transport. No exploit or automatic elevation. */
public final class RootShell {
 public static String quote(String s){if(s.indexOf('\0')>=0)throw new IllegalArgumentException("NUL in path");return "'"+s.replace("'","'\\''")+"'";}
 public static String backingPath(String path,String externalRoot){String root=externalRoot==null?"":externalRoot;while(root.length()>1&&root.endsWith("/"))root=root.substring(0,root.length()-1);String prefix="/storage/emulated/";if(!root.startsWith(prefix))return path;String user=root.substring(prefix.length());if(user.isEmpty()||user.contains("/")||!user.matches("[0-9]+"))return path;if(!path.equals(root)&&!path.startsWith(root+"/"))return path;return "/data/media/"+user+path.substring(root.length());}
 public static String visiblePath(String path,String externalRoot){String root=externalRoot==null?"":externalRoot;while(root.length()>1&&root.endsWith("/"))root=root.substring(0,root.length()-1);String physical=backingPath(root,root);if(physical.equals(root)||!path.equals(physical)&&!path.startsWith(physical+"/"))return path;return root+path.substring(physical.length());}
 public static final class Result{public final int code;public final String text;Result(int c,String t){code=c;text=t;}}
 public static Result run(String command,boolean root,int timeout)throws IOException{
  Process p=new ProcessBuilder(root?new String[]{"su","-c",command}:new String[]{"sh","-c",command}).redirectErrorStream(true).start();
  ByteArrayOutputStream out=new ByteArrayOutputStream();Thread reader=new Thread(()->{try(InputStream in=p.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()<2_000_000)out.write(b,0,Math.min(n,2_000_000-out.size()));}}catch(IOException ignored){}});reader.start();
  try{if(!p.waitFor(timeout,TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("Command timed out");}reader.join(2000);return new Result(p.exitValue(),out.toString("UTF-8"));}catch(InterruptedException e){p.destroyForcibly();Thread.currentThread().interrupt();throw new IOException("Cancelled",e);}
 }
 public static void check(String cmd)throws IOException{Result r=run(cmd,true,60);if(r.code!=0)throw new IOException(r.text.trim());}
}
