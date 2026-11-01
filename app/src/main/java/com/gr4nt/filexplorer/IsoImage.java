package com.gr4nt.filexplorer;
import java.io.*;import java.nio.charset.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** ISO9660/Joliet reader and writer; does not pretend UDF/DAA/DMG are ISO9660. */
public final class IsoImage {
 static final int SECTOR=2048;
 public static final class Entry{public String name;public boolean dir;public long extent,length;Entry(String n,boolean d,long e,long l){name=n;dir=d;extent=e;length=l;}}
 static long le(byte[] b,int p){return (b[p]&255L)|((b[p+1]&255L)<<8)|((b[p+2]&255L)<<16)|((b[p+3]&255L)<<24);}
 static void l16(byte[] b,int p,long v){b[p]=(byte)v;b[p+1]=(byte)(v>>8);}static void l32(byte[] b,int p,long v){for(int i=0;i<4;i++)b[p+i]=(byte)(v>>(8*i));}
 static void b16(byte[] b,int p,long v){b[p]=(byte)(v>>8);b[p+1]=(byte)v;}static void b32(byte[] b,int p,long v){for(int i=0;i<4;i++)b[p+i]=(byte)(v>>(8*(3-i)));}
 static void both16(byte[] b,int p,long v){l16(b,p,v);b16(b,p+2,v);}static void both32(byte[] b,int p,long v){l32(b,p,v);b32(b,p+4,v);}
 public static List<Entry> list(File iso)throws IOException{
  try(RandomAccessFile f=new RandomAccessFile(iso,"r")){byte[] selected=null;boolean joliet=false;
   for(int s=16;s<128;s++){byte[] b=new byte[SECTOR];f.seek((long)s*SECTOR);f.readFully(b);if(!new String(b,1,5,StandardCharsets.US_ASCII).equals("CD001"))throw new IOException("Not an ISO9660/Joliet image; UDF and proprietary disc images are not supported");if(b[0]==1&&selected==null)selected=b;if(b[0]==2&&b[88]=='%'&&b[89]=='/'){selected=b;joliet=true;}if((b[0]&255)==255)break;}
   if(selected==null)throw new IOException("No ISO9660 volume descriptor");List<Entry> out=new ArrayList<>();Set<Long> visited=new HashSet<>();walk(f,le(selected,158),le(selected,166),"",joliet,out,visited,0);return out;
  }
 }
 static void walk(RandomAccessFile f,long sector,long length,String prefix,boolean joliet,List<Entry> out,Set<Long> visited,int depth)throws IOException{
  if(depth>128||length>128L*1024*1024||sector*SECTOR+length>f.length())throw new IOException("Invalid ISO directory bounds");if(!visited.add(sector))return;
  long start=sector*SECTOR,pos=0;
  while(pos<length){f.seek(start+pos);int len=f.readUnsignedByte();if(len==0){pos=((pos/SECTOR)+1)*SECTOR;continue;}if(len<34||pos+len>length)throw new IOException("Invalid ISO directory record");byte[] rec=new byte[len];f.seek(start+pos);f.readFully(rec);pos+=len;int nameLen=rec[32]&255;if(33+nameLen>len)throw new IOException("Invalid name");if(nameLen==1&&(rec[33]==0||rec[33]==1))continue;
   String name=new String(rec,33,nameLen,joliet?StandardCharsets.UTF_16BE:StandardCharsets.US_ASCII).replaceFirst(";[0-9]+$","");String full=prefix+name;boolean dir=(rec[25]&2)!=0;if((rec[25]&128)!=0)throw new IOException("ISO multi-extent files are not supported");long extent=le(rec,2),size=le(rec,10);if(extent*SECTOR+size>f.length())throw new IOException("ISO entry out of bounds");out.add(new Entry(full,dir,extent,size));if(out.size()>200000)throw new IOException("Too many ISO entries");if(dir)walk(f,extent,size,full+"/",joliet,out,visited,depth+1);
  }
 }
 public static void extract(File iso,File dest,AtomicBoolean cancel)throws IOException{
  try(RandomAccessFile f=new RandomAccessFile(iso,"r")){long total=0;for(Entry e:list(iso)){if(cancel.get())throw new InterruptedIOException("Cancelled");File out=FileSafety.child(dest,e.name);if(e.dir){if(!out.isDirectory()&&!out.mkdirs())throw new IOException("Directory creation failed");continue;}if(out.exists())throw new IOException("Duplicate ISO path");if(!out.getParentFile().isDirectory()&&!out.getParentFile().mkdirs())throw new IOException("Directory creation failed");try(OutputStream to=FileSafety.newOutput(out)){f.seek(e.extent*SECTOR);long left=e.length;byte[] b=new byte[65536];while(left>0){if(cancel.get())throw new InterruptedIOException("Cancelled");int n=f.read(b,0,(int)Math.min(left,b.length));if(n<0)throw new EOFException();left-=n;total+=n;if(total>Archives.MAX_EXTRACTED)throw new IOException("Extraction limit exceeded");to.write(b,0,n);}}}}
 }
 static final class Tree{File file;String name,ascii;Tree parent;List<Tree> children=new ArrayList<>();int index,isoBlock,jolietBlock,isoSize,jolietSize,dataBlock;boolean dir(){return file==null||file.isDirectory();}}
 static void gather(Tree parent,File file,Set<String> seen,int depth)throws IOException{
  if(depth>100)throw new IOException("Directory nesting exceeds ISO limit");if(Files.isSymbolicLink(file.toPath()))throw new IOException("ISO creation does not follow symlinks");if(!seen.add(file.getName()))throw new IOException("Duplicate source names");if(file.getName().length()>64)throw new IOException("Joliet file names must fit 64 UTF-16 code units");if(file.length()>0xffffffffL&&!file.isDirectory())throw new IOException("ISO files above 4 GiB require unsupported multi-extent encoding");
  Tree n=new Tree();n.file=file;n.name=file.getName();n.parent=parent;n.ascii="N"+Integer.toString(parent.children.size()+1,36).toUpperCase(Locale.ROOT)+(file.isDirectory()?"":".DAT");parent.children.add(n);
  if(file.isDirectory()){File[] files=file.listFiles();if(files==null)throw new IOException("Unreadable folder");Set<String> names=new HashSet<>();for(File f:files)gather(n,f,names,depth+1);}
 }
 static byte[] id(Tree n,boolean j){return (j?n.name:n.ascii+(n.dir()?"":";1")).getBytes(j?StandardCharsets.UTF_16BE:StandardCharsets.US_ASCII);}
 static int recSize(int names){return 33+names+(names%2==0?1:0);}
 static int dirSize(Tree d,boolean j){int pos=68;for(Tree c:d.children){int len=recSize(id(c,j).length);if(pos%SECTOR+len>SECTOR)pos=((pos/SECTOR)+1)*SECTOR;pos+=len;}return ((pos+SECTOR-1)/SECTOR)*SECTOR;}
 static byte[] record(Tree t,byte[] name,boolean j){byte[] b=new byte[recSize(name.length)];b[0]=(byte)b.length;both32(b,2,t.dir()?(j?t.jolietBlock:t.isoBlock):t.dataBlock);both32(b,10,t.dir()?(j?t.jolietSize:t.isoSize):t.file.length());b[18]=126;b[19]=1;b[20]=1;b[25]=(byte)(t.dir()?2:0);both16(b,28,1);b[32]=(byte)name.length;System.arraycopy(name,0,b,33,name.length);return b;}
 static byte[] pathTable(List<Tree> dirs,boolean j,boolean big){ByteArrayOutputStream out=new ByteArrayOutputStream();for(Tree d:dirs){byte[] name=d.parent==null?new byte[]{0}:id(d,j);byte[] b=new byte[8+name.length+(name.length%2)];b[0]=(byte)name.length;if(big){b32(b,2,j?d.jolietBlock:d.isoBlock);b16(b,6,d.parent==null?1:d.parent.index);}else{l32(b,2,j?d.jolietBlock:d.isoBlock);l16(b,6,d.parent==null?1:d.parent.index);}System.arraycopy(name,0,b,8,name.length);out.write(b,0,b.length);}return out.toByteArray();}
 static int sectors(long size){return (int)((size+SECTOR-1)/SECTOR);}
 public static void create(File output,List<File> files,String label,AtomicBoolean cancel)throws IOException{
  if(output.exists())throw new IOException("Output exists");Tree root=new Tree();root.name="";root.ascii="";Set<String> names=new HashSet<>();for(File f:files)gather(root,f,names,0);
  List<Tree> dirs=new ArrayList<>(),all=new ArrayList<>();ArrayDeque<Tree> q=new ArrayDeque<>();q.add(root);while(!q.isEmpty()){Tree t=q.removeFirst();all.add(t);if(t.dir()){t.index=dirs.size()+1;dirs.add(t);q.addAll(t.children);}}if(dirs.size()>65535)throw new IOException("Too many directories");
  int aLen=pathTable(dirs,false,false).length,jLen=pathTable(dirs,true,false).length;int aL=19,aM=aL+sectors(aLen),jL=aM+sectors(aLen),jM=jL+sectors(jLen),next=jM+sectors(jLen);
  for(Tree d:dirs){d.isoSize=dirSize(d,false);d.isoBlock=next;next+=sectors(d.isoSize);d.jolietSize=dirSize(d,true);d.jolietBlock=next;next+=sectors(d.jolietSize);}
  for(Tree t:all)if(!t.dir()){t.dataBlock=next;next+=sectors(t.file.length());}long finalSize=(long)next*SECTOR;
  try(RandomAccessFile out=new RandomAccessFile(output,"rw")){out.setLength(finalSize);for(int type=1;type<=2;type++){
    boolean j=type==2;byte[] b=new byte[SECTOR];b[0]=(byte)type;System.arraycopy("CD001".getBytes(StandardCharsets.US_ASCII),0,b,1,5);b[6]=1;Arrays.fill(b,8,72,(byte)' ');byte[] lab=label.getBytes(j?StandardCharsets.UTF_16BE:StandardCharsets.US_ASCII);if(j){for(int k=8;k<72;k+=2){b[k]=0;b[k+1]=32;}}System.arraycopy(lab,0,b,40,Math.min(32,lab.length));both32(b,80,next);if(j){b[88]='%';b[89]='/';b[90]='E';}both16(b,120,1);both16(b,124,1);both16(b,128,SECTOR);both32(b,132,j?jLen:aLen);l32(b,140,j?jL:aL);b32(b,148,j?jM:aM);byte[] r=record(root,new byte[]{0},j);System.arraycopy(r,0,b,156,r.length);b[881]=1;out.seek((long)(15+type)*SECTOR);out.write(b);
   }
   byte[] end=new byte[SECTOR];end[0]=(byte)255;System.arraycopy("CD001".getBytes(StandardCharsets.US_ASCII),0,end,1,5);end[6]=1;out.seek(18L*SECTOR);out.write(end);
   int[] blocks={aL,aM,jL,jM};for(int i=0;i<4;i++){out.seek((long)blocks[i]*SECTOR);out.write(pathTable(dirs,i>=2,(i&1)==1));}
   for(Tree d:dirs)for(boolean j:new boolean[]{false,true}){byte[] buffer=new byte[j?d.jolietSize:d.isoSize];byte[] self=record(d,new byte[]{0},j),parent=record(d.parent==null?d:d.parent,new byte[]{1},j);System.arraycopy(self,0,buffer,0,self.length);System.arraycopy(parent,0,buffer,self.length,parent.length);int pos=self.length+parent.length;for(Tree c:d.children){byte[] r=record(c,id(c,j),j);if(pos%SECTOR+r.length>SECTOR)pos=((pos/SECTOR)+1)*SECTOR;System.arraycopy(r,0,buffer,pos,r.length);pos+=r.length;}out.seek((long)(j?d.jolietBlock:d.isoBlock)*SECTOR);out.write(buffer);}
   for(Tree t:all)if(!t.dir()){if(cancel.get())throw new InterruptedIOException("Cancelled");out.seek((long)t.dataBlock*SECTOR);try(InputStream in=new FileInputStream(t.file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(cancel.get())throw new InterruptedIOException("Cancelled");out.write(b,0,n);}}}
  }catch(IOException|RuntimeException e){output.delete();throw e;}
 }
 public static void binToIso(File bin,File output,AtomicBoolean cancel)throws IOException{
  if(output.exists())throw new IOException("Output exists");try(RandomAccessFile in=new RandomAccessFile(bin,"r");OutputStream out=new BufferedOutputStream(new FileOutputStream(output))){if(in.length()%2352!=0)throw new IOException("Requires a single-track raw 2352-byte-sector data BIN");byte[] sector=new byte[2352];while(in.getFilePointer()<in.length()){if(cancel.get())throw new InterruptedIOException("Cancelled");in.readFully(sector);if(sector[0]!=0||sector[11]!=0)throw new IOException("Not a raw CD data sector");for(int i=1;i<11;i++)if((sector[i]&255)!=255)throw new IOException("Invalid CD sync bytes");int mode=sector[15]&255;if(mode!=1&&mode!=2)throw new IOException("Audio or unknown sector type");if(mode==2&&(sector[18]&0x20)!=0)throw new IOException("Mode 2 Form 2 sectors cannot be converted to a 2048-byte ISO");out.write(sector,mode==1?16:24,2048);}}
  catch(IOException e){output.delete();throw e;}
 }
}
