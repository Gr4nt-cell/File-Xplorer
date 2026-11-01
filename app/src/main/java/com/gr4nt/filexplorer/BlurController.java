package com.gr4nt.filexplorer;
import android.app.Activity;import android.graphics.*;import android.graphics.drawable.BitmapDrawable;import android.os.Build;import android.view.*;import java.util.*;
/** Blur only the activity behind a modal: dialog text and controls stay crisp. */
public final class BlurController {
 static final WeakHashMap<Activity,State> states=new WeakHashMap<>();
 static class State{View root;int count;BitmapDrawable overlay;Bitmap bitmap;}
 public static void acquire(Activity activity){
  if(!Visuals.blur)return;State previous=states.get(activity);if(previous!=null){previous.count++;return;}
  State state=new State();state.root=activity.findViewById(android.R.id.content);if(state.root==null||state.root.getWidth()==0)return;state.count=1;states.put(activity,state);
  if(Build.VERSION.SDK_INT>=31){state.root.setRenderEffect(RenderEffect.createBlurEffect(Visuals.dp(activity,18),Visuals.dp(activity,18),Shader.TileMode.CLAMP));return;}
  try{int w=Math.max(1,state.root.getWidth()/8),h=Math.max(1,state.root.getHeight()/8);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.scale(w/(float)state.root.getWidth(),h/(float)state.root.getHeight());state.root.draw(canvas);boxBlur(bitmap,3);state.bitmap=bitmap;state.overlay=new BitmapDrawable(activity.getResources(),bitmap);state.overlay.setBounds(0,0,state.root.getWidth(),state.root.getHeight());state.root.getOverlay().add(state.overlay);}catch(OutOfMemoryError e){/* The translucent scrim remains usable on low-memory devices. */}
 }
 public static void release(Activity activity){State state=states.get(activity);if(state==null||--state.count>0)return;states.remove(activity);if(Build.VERSION.SDK_INT>=31)state.root.setRenderEffect(null);if(state.overlay!=null)state.root.getOverlay().remove(state.overlay);if(state.bitmap!=null)state.bitmap.recycle();}
 static void boxBlur(Bitmap bitmap,int radius){int w=bitmap.getWidth(),h=bitmap.getHeight();int[] data=new int[w*h],out=new int[w*h];bitmap.getPixels(data,0,w,0,0,w,h);for(int pass=0;pass<3;pass++){for(int y=0;y<h;y++)for(int x=0;x<w;x++){int r=0,g=0,b=0,n=0;for(int dx=-radius;dx<=radius;dx++){int c=data[y*w+Math.max(0,Math.min(w-1,x+dx))];r+=c>>16&255;g+=c>>8&255;b+=c&255;n++;}out[y*w+x]=0xff000000|r/n<<16|g/n<<8|b/n;}for(int y=0;y<h;y++)for(int x=0;x<w;x++){int r=0,g=0,b=0,n=0;for(int dy=-radius;dy<=radius;dy++){int c=out[Math.max(0,Math.min(h-1,y+dy))*w+x];r+=c>>16&255;g+=c>>8&255;b+=c&255;n++;}data[y*w+x]=0xff000000|r/n<<16|g/n<<8|b/n;}}bitmap.setPixels(data,0,w,0,0,w,h);}
}
