package com.gr4nt.filexplorer;
import java.io.*;import java.nio.charset.*;import java.text.Normalizer;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;import com.google.re2j.*;
/** Streaming, cancellable search shared by local, SAF and root storage adapters. */
public final class SearchEngine {
 public static final class Item{public String path,name;public boolean dir;public long size,modified;public Object tag;public Item(String p,String n,boolean d,long s,long m,Object t){path=p;name=n;dir=d;size=s;modified=m;tag=t;}}
 public interface Backend{List<Item> children(Item dir)throws IOException;InputStream read(Item file)throws IOException;String identity(Item item)throws IOException;}
 public static final class Options{public String query="",content="",extensions="";public boolean regex,caseSensitive,recursive=true,hidden;public long minSize=0,maxSize=Long.MAX_VALUE,after=0,before=Long.MAX_VALUE,contentLimit=50L*1024*1024;public int maxResults=5000;}
 public static final class Match{public final Item item;public final int line;public final String snippet;Match(Item i,int l,String s){item=i;line=l;snippet=s;}}
 public static final class Stats{public long scanned,skipped;public int matched;public boolean capped,cancelled;}
 public interface Listener{void match(Match m);void progress(Stats s);}
 static String fold(String s,boolean sensitive){String n=Normalizer.normalize(s,Normalizer.Form.NFC);return sensitive?n:n.toLowerCase(Locale.ROOT);}
 static final class Guarded implements CharSequence{final String s;final long end;Guarded(String s){this.s=s;end=System.nanoTime()+30_000_000L;}public int length(){return s.length();}public char charAt(int i){if(System.nanoTime()>end)throw new RegexBudget();return s.charAt(i);}public CharSequence subSequence(int a,int b){return s.subSequence(a,b);}public String toString(){return s;}}
 static final class RegexBudget extends RuntimeException{}
 static Pattern pattern(String s,Options o){if(s.isEmpty())return null;return Pattern.compile(s,o.caseSensitive?0:Pattern.CASE_INSENSITIVE);}
 static boolean matches(String value,String query,Pattern regex,Options o){if(query.isEmpty())return true;if(regex!=null)return regex.matcher(new Guarded(value)).find();String hay=fold(value,o.caseSensitive);for(String term:fold(query,o.caseSensitive).trim().split("\\s+"))if(!hay.contains(term))return false;return true;}
 static InputStream limited(InputStream source,long max){return new FilterInputStream(source){long count;public int read()throws IOException{int c=super.read();if(c>=0&&++count>max)throw new IOException("Content byte limit");return c;}public int read(byte[] b,int off,int len)throws IOException{int n=in.read(b,off,len);if(n>0&&(count+=n)>max)throw new IOException("Content byte limit");return n;}};}
 public static Stats run(Backend backend,List<Item> roots,Options o,AtomicBoolean cancel,Listener listener)throws IOException{
  Pattern namePattern=o.regex?pattern(o.query,o):null,contentPattern=o.regex?pattern(o.content,o):null;Set<String> ext=new HashSet<>();for(String s:o.extensions.toLowerCase(Locale.ROOT).split("[,;\\s]+"))if(!s.isEmpty())ext.add(s.replaceFirst("^\\.",""));
  Stats stats=new Stats();ArrayDeque<Item> dirs=new ArrayDeque<>(roots);Set<String> visited=new HashSet<>();
  while(!dirs.isEmpty()&&!cancel.get()){
   Item dir=dirs.removeFirst();String identity;try{identity=backend.identity(dir);}catch(IOException e){stats.skipped++;continue;}if(!visited.add(identity))continue;
   List<Item> entries;try{entries=backend.children(dir);}catch(IOException|SecurityException e){stats.skipped++;continue;}
   for(Item item:entries){if(cancel.get()){stats.cancelled=true;break;}stats.scanned++;if(!o.hidden&&item.name.startsWith("."))continue;
    if(item.dir&&o.recursive&&visited.size()<250000)dirs.addLast(item);
    try{
     if(!matches(item.name,o.query,namePattern,o))continue;
     if(item.modified<o.after||item.modified>o.before)continue;
     if(!ext.isEmpty()){int dot=item.name.lastIndexOf('.');if(item.dir||dot<0||!ext.contains(item.name.substring(dot+1).toLowerCase(Locale.ROOT)))continue;}
     if(item.size<o.minSize||item.size>o.maxSize)continue;
     int line=0;String snippet="";
     if(!o.content.isEmpty()){
      if(item.dir||item.size>o.contentLimit){stats.skipped++;continue;}
      try(InputStream source=limited(backend.read(item),o.contentLimit);PushbackInputStream in=new PushbackInputStream(source,4096)){
       byte[] prefix=new byte[4096];int n=in.read(prefix);if(n<0)continue;in.unread(prefix,0,n);Charset charset=StandardCharsets.UTF_8;
       boolean utf16=n>=2&&((prefix[0]==(byte)0xff&&prefix[1]==(byte)0xfe)||(prefix[0]==(byte)0xfe&&prefix[1]==(byte)0xff));
       if(utf16)charset=StandardCharsets.UTF_16;else {boolean binary=false;for(int i=0;i<n;i++)if(prefix[i]==0){binary=true;break;}if(binary){stats.skipped++;continue;}}
       try(Reader reader=new InputStreamReader(in,charset)){StringBuilder current=new StringBuilder();long read=0;int ch;int ln=1;boolean found=false;
        while((ch=reader.read())!=-1){if(cancel.get())break;if(++read>o.contentLimit)break;if(ch=='\n'||current.length()>=65536){String text=current.toString();if(matches(text,o.content,contentPattern,o)){line=ln;snippet=text.length()>180?text.substring(0,180)+"…":text;found=true;break;}current.setLength(0);ln++;}else current.append((char)ch);}
        if(!found&&!cancel.get()&&matches(current.toString(),o.content,contentPattern,o)){line=ln;snippet=current.substring(0,Math.min(180,current.length()));found=true;}if(!found)continue;
       }
      }
     }
     listener.match(new Match(item,line,snippet));stats.matched++;if(stats.matched>=o.maxResults){stats.capped=true;listener.progress(stats);return stats;}
    }catch(IOException|SecurityException|RegexBudget e){stats.skipped++;}
    if(stats.scanned%64==0)listener.progress(stats);
   }

  }
  stats.cancelled=cancel.get();listener.progress(stats);return stats;
 }
}
