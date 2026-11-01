package com.gr4nt.filexplorer;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.provider.DocumentsContract;
import android.os.ParcelFileDescriptor;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.net.ftp.FTPClient;import org.apache.commons.net.ftp.FTPFile;import org.apache.commons.net.ftp.FTPReply;

public final class Storage {
 public static final class Node {
  public String path,name;public boolean dir,root,shizuku;public long size,modified;
  public Node(String p,String n,boolean d,long s,long m,boolean r){this(p,n,d,s,m,r,false);}
  public Node(String p,String n,boolean d,long s,long m,boolean r,boolean z){path=p;name=n;dir=d;size=s;modified=m;root=r;shizuku=z;}
   public boolean saf(){return path.startsWith("content://");}
   public boolean ftp(){return path.startsWith("ftpfs://");}
  public String key(){return (root?"root:":shizuku?"shizuku:":"")+path;}
 }
  final ContentResolver resolver;final FtpSessions ftp;final ShizukuAccess shizuku;volatile boolean rootBackingKnown,rootBackingAvailable=true;
  public Storage(Context c,FtpSessions ftp,ShizukuAccess shizuku){resolver=c.getContentResolver();this.ftp=ftp;this.shizuku=shizuku;}
 public Node node(File f,boolean root){return new Node(f.getAbsolutePath(),f.getName().isEmpty()?"/":f.getName(),f.isDirectory(),f.length(),f.lastModified(),root);}
 public Node tree(Uri tree)throws IOException{Uri doc=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));return document(doc);}
 public Node document(Uri uri)throws IOException{
  try(Cursor c=resolver.query(uri,new String[]{"_display_name","mime_type","_size","last_modified"},null,null,null)){if(c!=null&&c.moveToFirst())return new Node(uri.toString(),c.getString(0),DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(1)),c.isNull(2)?0:c.getLong(2),c.isNull(3)?0:c.getLong(3),false);}throw new IOException("Folder permission is unavailable");
 }
 public List<Node> list(Node parent)throws IOException{
   if(parent.ftp())return listFtp(parent);
   if(parent.shizuku)return shizuku.list(parent.path);
  if(parent.saf()){
   List<Node> out=new ArrayList<>();Uri uri=Uri.parse(parent.path);Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(uri,DocumentsContract.getDocumentId(uri));
   try(Cursor c=resolver.query(children,new String[]{"document_id","_display_name","mime_type","_size","last_modified"},null,null,null)){if(c!=null)while(c.moveToNext()){Uri doc=DocumentsContract.buildDocumentUriUsingTree(uri,c.getString(0));out.add(new Node(doc.toString(),c.getString(1),DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2)),c.isNull(3)?0:c.getLong(3),c.isNull(4)?0:c.getLong(4),false));}}return out;
  }
  if(parent.root)return rootList(parent);
  File[] files=new File(parent.path).listFiles();if(files==null)throw new IOException("Cannot read this folder. Grant storage access or choose it with the folder picker.");List<Node> out=new ArrayList<>();for(File f:files)out.add(node(f,false));return out;
 }
  List<Node> listFtp(Node parent)throws IOException{
   FTPClient client=ftp.open(parent.path);List<Node> out=new ArrayList<>();
   try{
    FTPFile[] files=client.listFiles(FtpSessions.remotePath(parent.path));
    if(files==null||!FTPReply.isPositiveCompletion(client.getReplyCode()))throw new IOException("FTP could not list this folder: "+client.getReplyString());
    for(FTPFile file:files){String name=file.getName();if(name==null||name.isEmpty()||name.equals(".")||name.equals("..")||name.indexOf('/')>=0||name.indexOf('\0')>=0||file.isSymbolicLink())continue;if(!file.isDirectory()&&!file.isFile())continue;Calendar time=file.getTimestamp();long modified=time==null?0:time.getTimeInMillis();String path=FtpSessions.nodePath(FtpSessions.sessionId(parent.path),FtpSessions.childPath(FtpSessions.remotePath(parent.path),name));out.add(new Node(path,name,file.isDirectory(),Math.max(0,file.getSize()),modified,false));}
    return out;
   }finally{FtpSessions.disconnect(client);}
  }
  List<Node> rootList(Node parent)throws IOException{
   String backing=RootShell.backingPath(parent.path,externalRoot());RootShell.Result r=RootShell.run(rootListCommand(backing),true,45);
   if(!backing.equals(parent.path)){RootShell.Result visible=RootShell.run(rootListCommand(parent.path),true,45);if(RootShell.preferVisiblePath(r.code,r.text,visible.code,visible.text)){rootBackingKnown=true;rootBackingAvailable=false;r=visible;}else if(r.code==0){rootBackingKnown=true;rootBackingAvailable=true;}else throw new IOException("Root could not list "+parent.path+". Check that your root manager granted File Xplorer access.\nBacking path: "+r.text+"\nMounted path: "+visible.text);}
   if(r.code!=0)throw new IOException("Root could not list "+parent.path+". Check that your root manager granted File Xplorer access.\n"+r.text);String[] rows=r.text.split("\n");List<Node> out=new ArrayList<>();
   for(int i=0;i+1<rows.length;i+=2)try{String[] meta=rows[i].split("\\|");long mode=Long.parseLong(meta[0],16);String p=RootShell.visiblePath(new String(Base64.getDecoder().decode(rows[i+1]),"UTF-8"),external);out.add(new Node(p,new File(p).getName(),(mode&0170000)==0040000,Long.parseLong(meta[1]),Long.parseLong(meta[2])*1000,true));}catch(Exception e){throw new IOException("Root directory response could not be read: "+rows[i]);}return out;
 }
   String rootListCommand(String path){String q=RootShell.quote(path);return "test -d "+q+" && test -r "+q+" && test -x "+q+" || exit 1; for f in "+q+"/.[!.]* "+q+"/..?* "+q+"/*; do [ -e \"$f\" ] || [ -L \"$f\" ] || continue; stat -c '%f|%s|%Y|%a|%u|%g' \"$f\" || { echo 'Root could not inspect a directory entry' >&2; exit 2; }; encoded=$(printf %s \"$f\" | base64 | tr -d '\\n') || exit 2; printf '%s\\n' \"$encoded\" || exit 2; done";}
  String externalRoot(){return android.os.Environment.getExternalStorageDirectory().getAbsolutePath();}
  boolean protectedSharedRoot(String path)throws IOException{String target=new File(path).getCanonicalPath();File shared=new File(externalRoot()).getCanonicalFile();return target.equals(new File(shared,"Android/data").getCanonicalPath())||target.equals(new File(shared,"Android/obb").getCanonicalPath());}
  public String rootPath(String path){return rootBackingKnown&&!rootBackingAvailable?path:RootShell.backingPath(path,externalRoot());}
 public InputStream read(Node n)throws IOException{
   if(n.ftp()){FTPClient client=ftp.open(n.path);InputStream in=client.retrieveFileStream(FtpSessions.remotePath(n.path));if(in==null){String message=client.getReplyString();FtpSessions.disconnect(client);throw new IOException("FTP could not open the file: "+message);}return new FilterInputStream(in){boolean closed;@Override public void close()throws IOException{if(closed)return;closed=true;IOException failure=null;try{super.close();if(!client.completePendingCommand())failure=new IOException("FTP transfer failed: "+client.getReplyString());}catch(IOException e){failure=e;}finally{FtpSessions.disconnect(client);}if(failure!=null)throw failure;}};}
   if(n.shizuku)return new ParcelFileDescriptor.AutoCloseInputStream(shizuku.open(n.path,false));
  if(n.saf()){InputStream in=resolver.openInputStream(Uri.parse(n.path));if(in==null)throw new IOException("Cannot open file");return in;}
   if(n.root){Process p=new ProcessBuilder("su","-c","cat -- "+RootShell.quote(rootPath(n.path))).redirectError(ProcessBuilder.Redirect.INHERIT).start();return new FilterInputStream(p.getInputStream()){@Override public void close()throws IOException{super.close();try{if(p.waitFor()!=0)throw new IOException("Root read failed");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}}};}
  return new FileInputStream(n.path);
 }
 public OutputStream write(Node n)throws IOException{
   if(n.ftp()){FTPClient client=ftp.open(n.path);OutputStream out=client.storeFileStream(FtpSessions.remotePath(n.path));if(out==null){String message=client.getReplyString();FtpSessions.disconnect(client);throw new IOException("FTP could not write the file: "+message);}return new FilterOutputStream(out){boolean closed;@Override public void close()throws IOException{if(closed)return;closed=true;IOException failure=null;try{super.close();if(!client.completePendingCommand())failure=new IOException("FTP transfer failed: "+client.getReplyString());}catch(IOException e){failure=e;}finally{FtpSessions.disconnect(client);}if(failure!=null)throw failure;}};}
   if(n.shizuku)return new ParcelFileDescriptor.AutoCloseOutputStream(shizuku.open(n.path,true));
  if(n.saf()){OutputStream out=resolver.openOutputStream(Uri.parse(n.path),"wt");if(out==null)throw new IOException("Cannot write file");return out;}
   if(n.root){Process p=new ProcessBuilder("su","-c","cat > "+RootShell.quote(rootPath(n.path))).redirectError(ProcessBuilder.Redirect.INHERIT).start();return new FilterOutputStream(p.getOutputStream()){@Override public void close()throws IOException{super.close();try{if(p.waitFor()!=0)throw new IOException("Root write failed");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}}};}
  return new FileOutputStream(n.path);
 }
 public Node create(Node parent,String name,boolean directory)throws IOException{
  validName(name);
   if(parent.ftp()){for(Node n:list(parent))if(n.name.equals(name))throw new IOException("An item with that name already exists");FTPClient client=ftp.open(parent.path);try{String target=FtpSessions.childPath(FtpSessions.remotePath(parent.path),name);boolean ok=directory?client.makeDirectory(target):client.storeFile(target,new ByteArrayInputStream(new byte[0]));if(!ok)throw new IOException("FTP server refused to create "+name+": "+client.getReplyString());return new Node(FtpSessions.nodePath(FtpSessions.sessionId(parent.path),target),name,directory,0,System.currentTimeMillis(),false);}finally{FtpSessions.disconnect(client);}}
  for(Node n:list(parent))if(n.name.equals(name))throw new IOException("An item with that name already exists");
  if(parent.saf()){Uri uri=DocumentsContract.createDocument(resolver,Uri.parse(parent.path),directory?DocumentsContract.Document.MIME_TYPE_DIR:"application/octet-stream",name);if(uri==null)throw new IOException("Provider refused creation");return document(uri);}
  File f=new File(parent.path,name);
   if(parent.shizuku){shizuku.create(parent.path,name,directory);return new Node(f.getPath(),name,directory,0,System.currentTimeMillis(),false,true);}
   if(parent.root){RootShell.check((directory?"mkdir -- ":"set -C; : > ")+RootShell.quote(rootPath(f.getPath())));return new Node(f.getPath(),name,directory,0,System.currentTimeMillis(),true);}
  if(directory?!f.mkdir():!f.createNewFile())throw new IOException("Could not create item");return node(f,false);
 }
 public void rename(Node n,String name)throws IOException{
  validName(name);
   if(n.root&&protectedSharedRoot(n.path))throw new IOException("The Android/data and Android/obb roots cannot be renamed. Rename items inside them instead.");
   if(n.ftp()){String oldPath=FtpSessions.remotePath(n.path);int slash=oldPath.lastIndexOf('/');String parent=slash<=0?"/":oldPath.substring(0,slash);FTPClient client=ftp.open(n.path);try{if(!client.rename(oldPath,FtpSessions.childPath(parent,name)))throw new IOException("FTP server refused the rename: "+client.getReplyString());}finally{FtpSessions.disconnect(client);}return;}
  if(n.saf()){if(DocumentsContract.renameDocument(resolver,Uri.parse(n.path),name)==null)throw new IOException("Rename refused");return;}
   if(n.shizuku){shizuku.rename(n.path,name);return;}
  File target=new File(new File(n.path).getParentFile(),name);if(!n.root&&target.exists())throw new IOException("Destination exists");
   if(n.root)RootShell.check("test ! -e "+RootShell.quote(rootPath(target.getPath()))+" && test ! -L "+RootShell.quote(rootPath(target.getPath()))+" && mv -n -- "+RootShell.quote(rootPath(n.path))+" "+RootShell.quote(rootPath(target.getPath()))+" && test ! -e "+RootShell.quote(rootPath(n.path)));else if(!new File(n.path).renameTo(target))throw new IOException("Rename failed");
 }
 public void delete(Node n)throws IOException{
   if(n.ftp()){if(n.dir)for(Node child:list(n))delete(child);FTPClient client=ftp.open(n.path);try{String remote=FtpSessions.remotePath(n.path);boolean ok=n.dir?client.removeDirectory(remote):client.deleteFile(remote);if(!ok)throw new IOException("FTP server refused to delete "+n.name+": "+client.getReplyString());}finally{FtpSessions.disconnect(client);}return;}
  if(n.saf()){if(!DocumentsContract.deleteDocument(resolver,Uri.parse(n.path)))throw new IOException("Delete refused");return;}
   if(n.shizuku){if(n.dir)for(Node child:list(n))delete(child);shizuku.delete(n.path);return;}
   if(n.root&&protectedSharedRoot(n.path))throw new IOException("The Android/data and Android/obb roots cannot be deleted. Delete items inside them instead.");
  String path=n.root?new File(n.path).toPath().toAbsolutePath().normalize().toString():new File(n.path).getCanonicalPath();if(path.equals("/")||path.equals("/storage")||path.equals("/storage/emulated/0"))throw new IOException("Storage roots cannot be deleted");
   if(n.root){RootShell.check("rm -rf -- "+RootShell.quote(rootPath(path)));return;}
  if(Files.isSymbolicLink(new File(n.path).toPath())){Files.delete(new File(n.path).toPath());return;}
  if(n.dir)for(Node child:list(n))delete(child);if(!new File(n.path).delete())throw new IOException("Could not delete "+n.name);
 }
 public Node copy(Node source,Node destination,AtomicBoolean cancelled,Progress progress)throws IOException{
    if(source.dir&&!source.saf()&&!destination.saf()&&!source.ftp()&&!destination.ftp()){if(source.root||destination.root)rejectRootCopyIntoSelf(source,destination);else FileSafety.rejectDescendant(new File(source.path),new File(destination.path,source.name));}
  if(source.dir&&source.saf()&&destination.saf())rejectProviderDescendant(source,destination,cancelled);
  if(cancelled.get())throw new InterruptedIOException("Cancelled");
  Node out=create(destination,source.name,source.dir);
  try{
   if(source.dir){for(Node child:list(source)){if(!child.saf()&&!child.root&&Files.isSymbolicLink(new File(child.path).toPath()))throw new IOException("Symbolic links are not followed during recursive copy");copy(child,out,cancelled,progress);}}
   else try(InputStream in=read(source);OutputStream to=write(out)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1){if(cancelled.get())throw new InterruptedIOException("Cancelled");to.write(buf,0,n);progress.bytes(n,source.name);}}
   return out;
  }catch(IOException e){try{delete(out);}catch(IOException ignored){}throw e;}
 }
  void rejectRootCopyIntoSelf(Node source,Node destination)throws IOException{String sourcePath=RootShell.quote(rootPath(source.path)),destinationPath=RootShell.quote(rootPath(destination.path)),name=RootShell.quote(source.name);String cmd="s=$(readlink -f -- "+sourcePath+") || exit 1; d=$(readlink -f -- "+destinationPath+") || exit 1; t=\"$d\"/"+name+"; if [ \"$s\" = / ] || [ \"$t\" = \"$s\" ]; then echo 'Cannot copy a folder into itself' >&2; exit 1; fi; case \"$t/\" in \"$s/\"*) echo 'Cannot copy a folder into itself' >&2; exit 1;; esac";RootShell.check(cmd);}
 void rejectProviderDescendant(Node source,Node destination,AtomicBoolean cancelled)throws IOException{
  Uri dest=Uri.parse(destination.path);String destId=dest.getAuthority()+":"+DocumentsContract.getDocumentId(dest);Set<String> seen=new HashSet<>();ArrayDeque<Node> todo=new ArrayDeque<>();todo.add(source);
  while(!todo.isEmpty()){if(cancelled.get())throw new InterruptedIOException("Cancelled");Node n=todo.removeFirst();Uri uri=Uri.parse(n.path);String id=uri.getAuthority()+":"+DocumentsContract.getDocumentId(uri);if(id.equals(destId))throw new IOException("Cannot copy a folder into itself");if(!seen.add(id))throw new IOException("Document provider contains a directory cycle");if(seen.size()>250000)throw new IOException("Folder preflight exceeded directory limit");for(Node c:list(n))if(c.dir)todo.addLast(c);}
 }
 public interface Progress{void bytes(long n,String name);}
 public static void validName(String s)throws IOException{if(s==null||s.isEmpty()||s.equals(".")||s.equals("..")||s.indexOf('/')>=0||s.indexOf('\0')>=0)throw new IOException("Choose a valid single file name");}
}
