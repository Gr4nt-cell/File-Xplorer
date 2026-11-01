package com.gr4nt.filexplorer;
import java.io.*;import java.nio.file.*;
public final class FileSafety {
 public static File child(File dir,String entry)throws IOException{
  if(entry.indexOf('\0')>=0||entry.startsWith("/")||entry.startsWith("\\")||entry.matches("^[A-Za-z]:.*"))throw new IOException("Unsafe archive path");
  String normalized=entry.replace('\\','/');File out=new File(dir,normalized);String root=dir.getCanonicalPath()+File.separator;
  if(!out.getCanonicalPath().startsWith(root))throw new IOException("Archive entry escapes extraction folder");
  Path p=out.toPath();Path base=dir.getCanonicalFile().toPath();for(Path part=base;part!=null&&p.startsWith(base)&&!part.equals(p);){Path rel=base.relativize(p);Path walk=base;for(Path segment:rel){walk=walk.resolve(segment);if(Files.isSymbolicLink(walk))throw new IOException("Extraction through a symlink is refused");}break;}
  return out;
 }
 public static OutputStream newOutput(File file)throws IOException{return Files.newOutputStream(file.toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);}
 public static void rejectDescendant(File source,File target)throws IOException{String s=source.getCanonicalPath(),t=target.getCanonicalPath();if(t.equals(s)||t.startsWith(s+File.separator))throw new IOException("Cannot copy a folder into itself");}
 public static void transfer(InputStream in,OutputStream out,long limit,java.util.concurrent.atomic.AtomicBoolean cancel)throws IOException{byte[] b=new byte[65536];long total=0;int n;while((n=in.read(b))!=-1){if(cancel.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");total+=n;if(total>limit)throw new IOException("Configured extraction limit exceeded");out.write(b,0,n);}}
}
