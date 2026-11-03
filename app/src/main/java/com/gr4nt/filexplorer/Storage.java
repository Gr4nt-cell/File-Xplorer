package com.gr4nt.filexplorer;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.provider.DocumentsContract;
import android.os.ParcelFileDescriptor;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.net.ftp.FTPClient;import org.apache.commons.net.ftp.FTPFile;import org.apache.commons.net.ftp.FTPReply;

public final class Storage {
  public static final class Node {
   public String path,name;public boolean dir;public long size,modified;
   public Node(String p,String n,boolean d,long s,long m){path=p;name=n;dir=d;size=s;modified=m;}
    public boolean saf(){return path.startsWith("content://");}
    public boolean ftp(){return path.startsWith("ftpfs://");}
   public String key(){return path;}
 }
   final ContentResolver resolver;final FtpSessions ftp;
   public Storage(Context c,FtpSessions ftp){resolver=c.getContentResolver();this.ftp=ftp;}
  public Node node(File f){return new Node(f.getAbsolutePath(),f.getName().isEmpty()?"/":f.getName(),f.isDirectory(),f.length(),f.lastModified());}
  @Deprecated public Node node(File f,boolean unused){return node(f);}
 public Node tree(Uri tree)throws IOException{Uri doc=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));return document(doc);}
 public Node document(Uri uri)throws IOException{
   try(Cursor c=resolver.query(uri,new String[]{"_display_name","mime_type","_size","last_modified"},null,null,null)){if(c!=null&&c.moveToFirst())return new Node(uri.toString(),c.getString(0),DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(1)),c.isNull(2)?0:c.getLong(2),c.isNull(3)?0:c.getLong(3));}throw new IOException("Folder permission is unavailable");
 }
 public List<Node> list(Node parent)throws IOException{
   if(parent.ftp())return listFtp(parent);
  if(parent.saf()){
   List<Node> out=new ArrayList<>();Uri uri=Uri.parse(parent.path);Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(uri,DocumentsContract.getDocumentId(uri));
    try(Cursor c=resolver.query(children,new String[]{"document_id","_display_name","mime_type","_size","last_modified"},null,null,null)){if(c!=null)while(c.moveToNext()){Uri doc=DocumentsContract.buildDocumentUriUsingTree(uri,c.getString(0));out.add(new Node(doc.toString(),c.getString(1),DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2)),c.isNull(3)?0:c.getLong(3),c.isNull(4)?0:c.getLong(4)));}}return out;
  }
   File[] files=new File(parent.path).listFiles();if(files==null)throw new IOException("Cannot read this folder. Grant storage access or choose it with the folder picker.");List<Node> out=new ArrayList<>();for(File f:files)out.add(node(f));return out;
 }
  List<Node> listFtp(Node parent)throws IOException{
   FTPClient client=ftp.open(parent.path);List<Node> out=new ArrayList<>();
   try{
    FTPFile[] files=client.listFiles(FtpSessions.remotePath(parent.path));
    if(files==null||!FTPReply.isPositiveCompletion(client.getReplyCode()))throw new IOException("FTP could not list this folder: "+client.getReplyString());
     for(FTPFile file:files){String name=file.getName();if(name==null||name.isEmpty()||name.equals(".")||name.equals("..")||name.indexOf('/')>=0||name.indexOf('\0')>=0||file.isSymbolicLink())continue;if(!file.isDirectory()&&!file.isFile())continue;Calendar time=file.getTimestamp();long modified=time==null?0:time.getTimeInMillis();String path=FtpSessions.nodePath(FtpSessions.sessionId(parent.path),FtpSessions.childPath(FtpSessions.remotePath(parent.path),name));out.add(new Node(path,name,file.isDirectory(),Math.max(0,file.getSize()),modified));}
    return out;
   }finally{FtpSessions.disconnect(client);}
  }
 public InputStream read(Node n)throws IOException{
   if(n.ftp()){FTPClient client=ftp.open(n.path);InputStream in=client.retrieveFileStream(FtpSessions.remotePath(n.path));if(in==null){String message=client.getReplyString();FtpSessions.disconnect(client);throw new IOException("FTP could not open the file: "+message);}return new FilterInputStream(in){boolean closed;@Override public void close()throws IOException{if(closed)return;closed=true;IOException failure=null;try{super.close();if(!client.completePendingCommand())failure=new IOException("FTP transfer failed: "+client.getReplyString());}catch(IOException e){failure=e;}finally{FtpSessions.disconnect(client);}if(failure!=null)throw failure;}};}
  if(n.saf()){InputStream in=resolver.openInputStream(Uri.parse(n.path));if(in==null)throw new IOException("Cannot open file");return in;}
  return new FileInputStream(n.path);
 }
 public OutputStream write(Node n)throws IOException{
   if(n.ftp()){FTPClient client=ftp.open(n.path);OutputStream out=client.storeFileStream(FtpSessions.remotePath(n.path));if(out==null){String message=client.getReplyString();FtpSessions.disconnect(client);throw new IOException("FTP could not write the file: "+message);}return new FilterOutputStream(out){boolean closed;@Override public void close()throws IOException{if(closed)return;closed=true;IOException failure=null;try{super.close();if(!client.completePendingCommand())failure=new IOException("FTP transfer failed: "+client.getReplyString());}catch(IOException e){failure=e;}finally{FtpSessions.disconnect(client);}if(failure!=null)throw failure;}};}
  if(n.saf()){OutputStream out=resolver.openOutputStream(Uri.parse(n.path),"wt");if(out==null)throw new IOException("Cannot write file");return out;}
  return new FileOutputStream(n.path);
 }
 public Node create(Node parent,String name,boolean directory)throws IOException{
  validName(name);
    if(parent.ftp()){for(Node n:list(parent))if(n.name.equals(name))throw new IOException("An item with that name already exists");FTPClient client=ftp.open(parent.path);try{String target=FtpSessions.childPath(FtpSessions.remotePath(parent.path),name);boolean ok=directory?client.makeDirectory(target):client.storeFile(target,new ByteArrayInputStream(new byte[0]));if(!ok)throw new IOException("FTP server refused to create "+name+": "+client.getReplyString());return new Node(FtpSessions.nodePath(FtpSessions.sessionId(parent.path),target),name,directory,0,System.currentTimeMillis());}finally{FtpSessions.disconnect(client);}}
  for(Node n:list(parent))if(n.name.equals(name))throw new IOException("An item with that name already exists");
  if(parent.saf()){Uri uri=DocumentsContract.createDocument(resolver,Uri.parse(parent.path),directory?DocumentsContract.Document.MIME_TYPE_DIR:"application/octet-stream",name);if(uri==null)throw new IOException("Provider refused creation");return document(uri);}
  File f=new File(parent.path,name);
   if(directory?!f.mkdir():!f.createNewFile())throw new IOException("Could not create item");return node(f);
 }
 public Node rename(Node n,String name)throws IOException{
  validName(name);
   if(name.equals(n.name))return n;
    if(n.ftp()){String oldPath=FtpSessions.remotePath(n.path);int slash=oldPath.lastIndexOf('/');String parent=slash<=0?"/":oldPath.substring(0,slash);FTPClient client=ftp.open(n.path);try{if(!client.rename(oldPath,FtpSessions.childPath(parent,name)))throw new IOException("FTP server refused the rename: "+client.getReplyString());return new Node(FtpSessions.nodePath(FtpSessions.sessionId(n.path),FtpSessions.childPath(parent,name)),name,n.dir,n.size,n.modified);}finally{FtpSessions.disconnect(client);}}
  if(n.saf()){Uri renamed=DocumentsContract.renameDocument(resolver,Uri.parse(n.path),name);if(renamed==null)throw new IOException("Rename refused");return document(renamed);}
   File source=new File(n.path);File parent=source.getAbsoluteFile().getParentFile();if(parent==null)throw new IOException("Storage roots cannot be renamed");File target=new File(parent,name);if(target.exists())throw new IOException("An item named \""+name+"\" already exists");
   try{Files.move(source.toPath(),target.toPath());return node(target);}catch(IOException e){throw new IOException("Could not rename \""+n.name+"\" to \""+name+"\": "+e.getMessage(),e);}
 }
 public void delete(Node n)throws IOException{
   if(n.ftp()){if(n.dir)for(Node child:list(n))delete(child);FTPClient client=ftp.open(n.path);try{String remote=FtpSessions.remotePath(n.path);boolean ok=n.dir?client.removeDirectory(remote):client.deleteFile(remote);if(!ok)throw new IOException("FTP server refused to delete "+n.name+": "+client.getReplyString());}finally{FtpSessions.disconnect(client);}return;}
  if(n.saf()){if(!DocumentsContract.deleteDocument(resolver,Uri.parse(n.path)))throw new IOException("Delete refused");return;}
   String path=new File(n.path).getCanonicalPath();if(path.equals("/")||path.equals("/storage")||path.equals("/storage/emulated/0"))throw new IOException("Storage roots cannot be deleted");
  if(Files.isSymbolicLink(new File(n.path).toPath())){Files.delete(new File(n.path).toPath());return;}
  if(n.dir)for(Node child:list(n))delete(child);if(!new File(n.path).delete())throw new IOException("Could not delete "+n.name);
 }
 public Node copy(Node source,Node destination,AtomicBoolean cancelled,Progress progress)throws IOException{
  return copyAs(source,destination,source.name,cancelled,progress);
 }
 public Node copyAs(Node source,Node destination,String outputName,AtomicBoolean cancelled,Progress progress)throws IOException{
     validName(outputName);
     if(source.dir&&!source.saf()&&!destination.saf()&&!source.ftp()&&!destination.ftp())FileSafety.rejectDescendant(new File(source.path),new File(destination.path,outputName));
  if(source.dir&&source.saf()&&destination.saf())rejectProviderDescendant(source,destination,cancelled);
  if(cancelled.get())throw new InterruptedIOException("Cancelled");
  Node out=create(destination,outputName,source.dir);
  try{
    if(source.dir){for(Node child:list(source)){if(!child.saf()&&Files.isSymbolicLink(new File(child.path).toPath()))throw new IOException("Symbolic links are not followed during recursive copy");copy(child,out,cancelled,progress);}}
   else try(InputStream in=read(source);OutputStream to=write(out)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1){if(cancelled.get())throw new InterruptedIOException("Cancelled");to.write(buf,0,n);progress.bytes(n,source.name);}}
   return out;
  }catch(IOException e){try{delete(out);}catch(IOException ignored){}throw e;}
 }
 public Node replaceCopy(Node source,Node destination,Node existing,AtomicBoolean cancelled,Progress progress)throws IOException{
  if(existing==null)throw new IOException("The existing destination item could not be found");
  if(source.key().equals(existing.key()))return existing;
  String backupName=".filexplorer-replaced-"+UUID.randomUUID().toString().replace("-","");
  Node backup=rename(existing,backupName);
  Node output;
  try{output=copyAs(source,destination,existing.name,cancelled,progress);}
  catch(IOException failure){try{rename(backup,existing.name);}catch(IOException rollback){failure.addSuppressed(rollback);}throw failure;}
  try{delete(backup);}
  catch(IOException cleanup){throw new IOException("The replacement was copied, but the previous item could not be removed from its temporary backup. Check the destination before retrying.",cleanup);}
  return output;
 }
 void rejectProviderDescendant(Node source,Node destination,AtomicBoolean cancelled)throws IOException{
  Uri dest=Uri.parse(destination.path);String destId=dest.getAuthority()+":"+DocumentsContract.getDocumentId(dest);Set<String> seen=new HashSet<>();ArrayDeque<Node> todo=new ArrayDeque<>();todo.add(source);
  while(!todo.isEmpty()){if(cancelled.get())throw new InterruptedIOException("Cancelled");Node n=todo.removeFirst();Uri uri=Uri.parse(n.path);String id=uri.getAuthority()+":"+DocumentsContract.getDocumentId(uri);if(id.equals(destId))throw new IOException("Cannot copy a folder into itself");if(!seen.add(id))throw new IOException("Document provider contains a directory cycle");if(seen.size()>250000)throw new IOException("Folder preflight exceeded directory limit");for(Node c:list(n))if(c.dir)todo.addLast(c);}
 }
 public interface Progress{void bytes(long n,String name);}
 public static void validName(String s)throws IOException{if(s==null||s.isEmpty()||s.equals(".")||s.equals("..")||s.indexOf('/')>=0||s.indexOf('\0')>=0)throw new IOException("Choose a valid single file name");}
}
