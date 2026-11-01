package com.gr4nt.filexplorer;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;
public final class ShareProvider extends ContentProvider {
 public static Uri uri(File f){return new Uri.Builder().scheme("content").authority("com.gr4nt.filexplorer.share").appendPath("file").appendQueryParameter("path",f.getAbsolutePath()).build();}
 File file(Uri uri)throws FileNotFoundException{String path=uri.getQueryParameter("path");if(path==null)throw new FileNotFoundException();File f=new File(path);try{String c=f.getCanonicalPath();String external=android.os.Environment.getExternalStorageDirectory().getCanonicalPath()+"/",cache=getContext().getCacheDir().getCanonicalPath()+"/",files=getContext().getFilesDir().getCanonicalPath()+"/";if(!c.startsWith(external)&&!c.startsWith(cache)&&!c.startsWith(files))throw new FileNotFoundException("Path not shared");}catch(IOException e){throw new FileNotFoundException();}return f;}
 @Override public boolean onCreate(){return true;}
 @Override public String getType(Uri u){return "application/octet-stream";}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!mode.equals("r"))throw new FileNotFoundException("Read-only share");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 @Override public Cursor query(Uri u,String[] p,String s,String[] a,String sort){try{File f=file(u);MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});c.addRow(new Object[]{f.getName(),f.length()});return c;}catch(FileNotFoundException e){return null;}}
 @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}@Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}@Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
