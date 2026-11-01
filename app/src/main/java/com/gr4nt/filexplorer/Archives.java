package com.gr4nt.filexplorer;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.compress.archivers.*;import org.apache.commons.compress.archivers.tar.*;import org.apache.commons.compress.archivers.sevenz.*;import org.apache.commons.compress.compressors.*;
import net.lingala.zip4j.ZipFile;import net.lingala.zip4j.model.*;import net.lingala.zip4j.model.enums.*;

public final class Archives {
 public static final long MAX_EXTRACTED=32L*1024*1024*1024;public static final int MAX_ENTRIES=200000;
 public static final class Entry{public final String name;public final long size;public final boolean directory;public Entry(String n,long s,boolean d){name=n;size=s;directory=d;}}
 public static boolean zip(File f){String s=f.getName().toLowerCase(Locale.ROOT);return s.matches(".*\\.(zip|apk|jar|aab|apks|xapk)$");}
 public static List<Entry> list(File f,String password,AtomicBoolean cancel)throws IOException{
  List<Entry> result=new ArrayList<>();String n=f.getName().toLowerCase(Locale.ROOT);
  if(zip(f)){try(ZipFile z=new ZipFile(f,password.toCharArray())){for(FileHeader h:z.getFileHeaders()){if(cancel.get())break;result.add(new Entry(h.getFileName(),h.getUncompressedSize(),h.isDirectory()));}}}
  else if(n.endsWith(".7z")){try(SevenZFile z=new SevenZFile(f,password.toCharArray())){SevenZArchiveEntry e;while(!cancel.get()&&(e=z.getNextEntry())!=null){result.add(new Entry(e.getName(),e.getSize(),e.isDirectory()));if(result.size()>MAX_ENTRIES)throw new IOException("Too many archive entries");}}}
  else if(n.endsWith(".iso")){for(IsoImage.Entry e:IsoImage.list(f))result.add(new Entry(e.name,e.length,e.dir));}
  else if(single(n)){result.add(new Entry(singleName(f),-1,false));}
  else if(n.endsWith(".rar")){try(com.github.junrar.Archive rar=new com.github.junrar.Archive(f)){for(com.github.junrar.rarfile.FileHeader h:rar.getFileHeaders())result.add(new Entry(h.getFileNameString(),h.getFullUnpackSize(),h.isDirectory()));}catch(Exception e){throw new IOException("RAR4 reader: "+e.getMessage(),e);}}
  else try(InputStream in=decoded(f);ArchiveInputStream<?> a=archive(in,f)){ArchiveEntry e;while(!cancel.get()&&(e=a.getNextEntry())!=null){result.add(new Entry(e.getName(),e.getSize(),e.isDirectory()));if(result.size()>MAX_ENTRIES)throw new IOException("Too many entries");}}
  return result;
 }
 public static void extract(File f,File folder,String password,AtomicBoolean cancel)throws IOException{
  if(!folder.mkdir())throw new IOException("Extraction needs a new, empty destination folder");long[] total={0};
  try{
   String n=f.getName().toLowerCase(Locale.ROOT);
   if(zip(f))try(ZipFile z=new ZipFile(f,password.toCharArray())){for(FileHeader h:z.getFileHeaders()){if(cancel.get())throw new InterruptedIOException("Cancelled");File target=target(folder,h.getFileName(),h.isDirectory());if(h.isDirectory())continue;if((h.getExternalFileAttributes()[3]&0xF0)==0xA0)throw new IOException("Archive symlinks are not extracted");try(InputStream in=z.getInputStream(h);OutputStream out=FileSafety.newOutput(target)){copy(in,out,total,cancel);}}}
   else if(n.endsWith(".7z"))try(SevenZFile z=new SevenZFile(f,password.toCharArray())){SevenZArchiveEntry e;byte[] b=new byte[65536];while((e=z.getNextEntry())!=null){File target=target(folder,e.getName(),e.isDirectory());if(e.isDirectory())continue;try(OutputStream out=FileSafety.newOutput(target)){int k;while((k=z.read(b))!=-1){if(cancel.get())throw new InterruptedIOException("Cancelled");total[0]+=k;if(total[0]>MAX_EXTRACTED)throw new IOException("Extraction limit exceeded");out.write(b,0,k);}}}}
   else if(n.endsWith(".iso"))IsoImage.extract(f,folder,cancel);
   else if(single(n))try(InputStream in=decoded(f);OutputStream out=FileSafety.newOutput(target(folder,singleName(f),false))){copy(in,out,total,cancel);}
   else if(n.endsWith(".rar"))try(com.github.junrar.Archive rar=new com.github.junrar.Archive(f)){for(com.github.junrar.rarfile.FileHeader e:rar.getFileHeaders()){File target=target(folder,e.getFileNameString(),e.isDirectory());if(e.isDirectory())continue;try(InputStream in=rar.getInputStream(e);OutputStream out=FileSafety.newOutput(target)){copy(in,out,total,cancel);}}}catch(Exception e){throw new IOException(e);}
   else try(InputStream in=decoded(f);ArchiveInputStream<?> a=archive(in,f)){ArchiveEntry e;while((e=a.getNextEntry())!=null){if(e instanceof TarArchiveEntry&&(((TarArchiveEntry)e).isSymbolicLink()||((TarArchiveEntry)e).isLink()))throw new IOException("Archive links are not extracted");if(!a.canReadEntryData(e))throw new IOException("Unsupported entry compression");File target=target(folder,e.getName(),e.isDirectory());if(!e.isDirectory())try(OutputStream out=FileSafety.newOutput(target)){copy(a,out,total,cancel);}}}
  }catch(IOException e){throw e;}
 }
 static File target(File folder,String name,boolean dir)throws IOException{File out=FileSafety.child(folder,name);if(out.exists()&&!(dir&&out.isDirectory()))throw new IOException("Duplicate archive entry: "+name);File p=dir?out:out.getParentFile();if(!p.isDirectory()&&!p.mkdirs())throw new IOException("Could not create directory");return out;}
 static void copy(InputStream in,OutputStream out,long[] total,AtomicBoolean cancel)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(cancel.get())throw new InterruptedIOException("Cancelled");total[0]+=n;if(total[0]>MAX_EXTRACTED)throw new IOException("Extraction limit exceeded");out.write(b,0,n);}}
 static boolean single(String n){return n.matches(".*\\.(gz|bz2|xz|lzma)$")&&!n.matches(".*\\.tar\\.(gz|bz2|xz|lzma)$");}
 static String singleName(File f){String n=f.getName();return n.substring(0,n.lastIndexOf('.'));}
 static InputStream decoded(File f)throws IOException{BufferedInputStream in=new BufferedInputStream(new FileInputStream(f));try{return new CompressorStreamFactory(true).createCompressorInputStream(in);}catch(CompressorException e){return in;}}
 static ArchiveInputStream<?> archive(InputStream in,File f)throws IOException{try{return new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(in));}catch(ArchiveException e){throw new IOException("Supported archives: ZIP, 7z, TAR and compressed TAR, CPIO/AR, ISO9660/Joliet and legacy RAR. "+e.getMessage(),e);}}
 public static void create(File output,List<File> sources,String format,String password,AtomicBoolean cancel)throws IOException{
  if(output.exists())throw new IOException("Output already exists");for(File f:sources){FileSafety.rejectDescendant(f,output);checkTree(f,cancel);}
  try{
   if(format.equals("zip")){try(net.lingala.zip4j.io.outputstream.ZipOutputStream out=new net.lingala.zip4j.io.outputstream.ZipOutputStream(FileSafety.newOutput(output),password.toCharArray())){for(File f:sources)addZip(out,f,f.getName(),password,cancel);}}
   else if(format.equals("7z")){if(!password.isEmpty())throw new IOException("7z encryption creation is unavailable; use AES-256 ZIP");try(SevenZOutputFile out=new SevenZOutputFile(output)){out.setContentCompression(SevenZMethod.LZMA2);for(File f:sources)add7(out,f,f.getName(),cancel);}}
   else if(format.equals("iso"))IsoImage.create(output,sources,"FILE_XPLORER",cancel);
   else{OutputStream raw=new BufferedOutputStream(new FileOutputStream(output));String compressor=format.equals("tar.gz")?"gz":format.equals("tar.xz")?"xz":format.equals("tar.bz2")?"bzip2":null;try{if(compressor!=null)raw=new CompressorStreamFactory().createCompressorOutputStream(compressor,raw);}catch(CompressorException e){raw.close();throw new IOException(e);}try(TarArchiveOutputStream out=new TarArchiveOutputStream(raw)){out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);for(File f:sources)addTar(out,f,f.getName(),cancel);}}
  }catch(IOException|RuntimeException e){output.delete();throw e;}
 }
 static void addZip(net.lingala.zip4j.io.outputstream.ZipOutputStream out,File f,String name,String password,AtomicBoolean cancel)throws IOException{
  checkSource(f,cancel);ZipParameters p=new ZipParameters();p.setFileNameInZip(name+(f.isDirectory()?"/":""));p.setCompressionLevel(CompressionLevel.NORMAL);if(!f.isDirectory()&&!password.isEmpty()){p.setEncryptFiles(true);p.setEncryptionMethod(EncryptionMethod.AES);p.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);}out.putNextEntry(p);if(!f.isDirectory())try(InputStream in=new FileInputStream(f)){FileSafety.transfer(in,out,Long.MAX_VALUE,cancel);}out.closeEntry();if(f.isDirectory()){File[] children=f.listFiles();if(children==null)throw new IOException("Unreadable folder");for(File child:children)addZip(out,child,name+"/"+child.getName(),password,cancel);}
 }
 static void add7(SevenZOutputFile out,File f,String name,AtomicBoolean cancel)throws IOException{checkSource(f,cancel);SevenZArchiveEntry e=out.createArchiveEntry(f,name);out.putArchiveEntry(e);if(!f.isDirectory())try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(cancel.get())throw new InterruptedIOException("Cancelled");out.write(b,0,n);}}out.closeArchiveEntry();if(f.isDirectory()){File[] a=f.listFiles();if(a==null)throw new IOException("Unreadable directory");for(File c:a)add7(out,c,name+"/"+c.getName(),cancel);}}
 static void addTar(TarArchiveOutputStream out,File f,String name,AtomicBoolean cancel)throws IOException{checkSource(f,cancel);TarArchiveEntry e=new TarArchiveEntry(f,name);out.putArchiveEntry(e);if(!f.isDirectory())try(InputStream in=new FileInputStream(f)){FileSafety.transfer(in,out,Long.MAX_VALUE,cancel);}out.closeArchiveEntry();if(f.isDirectory()){File[] a=f.listFiles();if(a==null)throw new IOException("Unreadable directory");for(File c:a)addTar(out,c,name+"/"+c.getName(),cancel);}}
 static void checkTree(File f,AtomicBoolean cancel)throws IOException{checkSource(f,cancel);if(f.isDirectory()){File[] children=f.listFiles();if(children==null)throw new IOException("Unreadable source directory");for(File child:children)checkTree(child,cancel);}}
 static void checkSource(File f,AtomicBoolean cancel)throws IOException{if(cancel.get())throw new InterruptedIOException("Cancelled");if(Files.isSymbolicLink(f.toPath()))throw new IOException("Symbolic links are not followed");}
 public static void replaceZipEntry(File zip,String entry,File source,String password)throws IOException{FileSafety.child(new File("/archive-validation"),entry);try(ZipFile z=new ZipFile(zip,password.toCharArray())){ZipParameters p=new ZipParameters();p.setFileNameInZip(entry);p.setOverrideExistingFilesInZip(true);z.addFile(source,p);}}
 public static void removeZipEntry(File zip,String entry,String password)throws IOException{try(ZipFile z=new ZipFile(zip,password.toCharArray())){z.removeFile(entry);}}
 public static void readEntry(File archive,String name,File target,String password,AtomicBoolean cancel)throws IOException{
  FileSafety.child(new File("/entry-validation"),name);String n=archive.getName().toLowerCase(Locale.ROOT);long[] total={0};
  try(OutputStream out=FileSafety.newOutput(target)){
   if(zip(archive))try(ZipFile z=new ZipFile(archive,password.toCharArray())){FileHeader h=z.getFileHeader(name);if(h==null||h.isDirectory())throw new IOException("Not a file entry");try(InputStream in=z.getInputStream(h)){FileSafety.transfer(in,out,128L*1024*1024,cancel);}return;}
   else if(n.endsWith(".7z"))try(SevenZFile z=new SevenZFile(archive,password.toCharArray())){SevenZArchiveEntry e;while((e=z.getNextEntry())!=null)if(e.getName().equals(name)){if(e.getSize()>128L*1024*1024)throw new IOException("Preview exceeds 128 MB; extract it instead");byte[] buf=new byte[65536];int count;while((count=z.read(buf))!=-1){if(cancel.get())throw new InterruptedIOException();out.write(buf,0,count);}return;}}
   else if(n.endsWith(".iso"))try(RandomAccessFile f=new RandomAccessFile(archive,"r")){for(IsoImage.Entry e:IsoImage.list(archive))if(e.name.equals(name)&&!e.dir){if(e.length>128L*1024*1024)throw new IOException("Preview exceeds 128 MB");f.seek(e.extent*2048);byte[] buf=new byte[65536];long left=e.length;while(left>0){if(cancel.get())throw new InterruptedIOException();int count=f.read(buf,0,(int)Math.min(buf.length,left));if(count<0)throw new EOFException();out.write(buf,0,count);left-=count;}return;}}
   else if(n.endsWith(".rar"))try(com.github.junrar.Archive rar=new com.github.junrar.Archive(archive)){for(com.github.junrar.rarfile.FileHeader e:rar.getFileHeaders())if(e.getFileNameString().equals(name)){try(InputStream in=rar.getInputStream(e)){FileSafety.transfer(in,out,128L*1024*1024,cancel);}return;}}catch(Exception e){throw new IOException(e);}
   else if(single(n)){try(InputStream in=decoded(archive)){FileSafety.transfer(in,out,128L*1024*1024,cancel);}return;}
   else try(InputStream in=decoded(archive);ArchiveInputStream<?> a=archive(in,archive)){ArchiveEntry e;while((e=a.getNextEntry())!=null)if(e.getName().equals(name)){FileSafety.transfer(a,out,128L*1024*1024,cancel);return;}}
   throw new IOException("Entry not found");
  }catch(IOException|RuntimeException e){target.delete();throw e;}
 }
 public static void verify(File archive,String password,AtomicBoolean cancel)throws IOException{
  OutputStream sink=new OutputStream(){public void write(int b){}public void write(byte[] b,int o,int n){}};long[] total={0};String n=archive.getName().toLowerCase(Locale.ROOT);
  if(zip(archive))try(ZipFile z=new ZipFile(archive,password.toCharArray())){for(FileHeader h:z.getFileHeaders())if(!h.isDirectory())try(InputStream in=z.getInputStream(h)){copy(in,sink,total,cancel);}}
  else if(n.endsWith(".7z"))try(SevenZFile z=new SevenZFile(archive,password.toCharArray())){byte[] b=new byte[65536];while(z.getNextEntry()!=null){int k;while((k=z.read(b))!=-1){if(cancel.get())throw new InterruptedIOException();total[0]+=k;if(total[0]>MAX_EXTRACTED)throw new IOException("Read limit exceeded");}}}
  else if(n.endsWith(".iso"))IsoImage.list(archive);
  else if(single(n))try(InputStream in=decoded(archive)){copy(in,sink,total,cancel);}
  else if(n.endsWith(".rar"))try(com.github.junrar.Archive rar=new com.github.junrar.Archive(archive)){for(com.github.junrar.rarfile.FileHeader h:rar.getFileHeaders())if(!h.isDirectory())try(InputStream in=rar.getInputStream(h)){copy(in,sink,total,cancel);}}catch(Exception e){throw new IOException(e);}
  else try(InputStream in=decoded(archive);ArchiveInputStream<?> a=archive(in,archive)){while(a.getNextEntry()!=null)copy(a,sink,total,cancel);}
 }

}
