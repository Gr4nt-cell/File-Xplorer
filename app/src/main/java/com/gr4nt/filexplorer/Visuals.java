package com.gr4nt.filexplorer;
import android.content.Context;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;
public final class Visuals {
  public static int INK=0xff111318,MUTED=0xff6f7681,BLUE=0xff167cf4,LINE=0x44ffffff,CANVAS=0xffe8efff;public static boolean dark,mobile,motion=true,blur=true;
  public static void configure(boolean night){configure(night,false);}public static void configure(boolean night,boolean phone){dark=night;mobile=phone;INK=night?0xfff5f6fa:0xff111318;MUTED=night?0xffa5a8b1:0xff6f7681;BLUE=night?0xff3295ff:0xff167cf4;CANVAS=night?0xff090a0d:0xffe8efff;}
 public static boolean animate(){return motion&&android.animation.ValueAnimator.areAnimatorsEnabled();}
 public static void enter(View v){if(!animate())return;v.setAlpha(0);v.setTranslationY(dp(v.getContext(),10));v.animate().alpha(1).translationY(0).setDuration(260).setInterpolator(new android.view.animation.PathInterpolator(.2f,0,0,1)).start();}
 public static void press(View v){v.setOnTouchListener((view,e)->{if(animate()){if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)view.animate().scaleX(.96f).scaleY(.96f).setDuration(90).start();if(e.getActionMasked()==android.view.MotionEvent.ACTION_UP||e.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)view.animate().scaleX(1).scaleY(1).setDuration(180).setInterpolator(new android.view.animation.OvershootInterpolator(.6f)).start();}return false;});}
  public static android.graphics.drawable.Drawable button(float radius){return new RippleDrawable(android.content.res.ColorStateList.valueOf(dark?0x443295ff:0x33167cf4),glass(radius,false),null);}

 public static int dp(Context c,float v){return (int)(v*c.getResources().getDisplayMetrics().density+.5f);}
 static Bitmap ambient;static int ambientW,ambientH;
    public static GradientDrawable glass(float radius,boolean active){GradientDrawable g=new AmbientGlass(radius);g.setOrientation(GradientDrawable.Orientation.TL_BR);g.setColors(dark?new int[]{0xc843454b,0x9b1c1e23}:new int[]{0x9bffffff,0x78f7f9ff});g.setCornerRadius(radius);g.setStroke(1,active?(dark?0xff3295ff:0xff167cf4):(dark?0x889da2ad:0xc7ffffff));return g;}
    public static GradientDrawable softGlass(float radius,boolean active){GradientDrawable g=new AmbientGlass(radius);g.setOrientation(GradientDrawable.Orientation.TL_BR);g.setColors(dark?new int[]{0x9840444a,0x7817191e}:new int[]{0x83ffffff,0x66f7f9ff});g.setCornerRadius(radius);g.setStroke(1,active?(dark?0xd93295ff:0xc4167cf4):(dark?0x709da2ad:0x9cffffff));return g;}
 static class AmbientGlass extends GradientDrawable {final float radius;final Paint sample=new Paint(3);final Path clip=new Path();AmbientGlass(float r){radius=r;}@Override public void draw(Canvas c){if(blur&&ambient!=null&&!ambient.isRecycled()&&getCallback() instanceof View){View target=(View)getCallback();int[] xy=new int[2];target.getLocationInWindow(xy);Rect b=getBounds();c.save();clip.reset();clip.addRoundRect(new RectF(b),radius,radius,Path.Direction.CW);c.clipPath(clip);c.drawBitmap(ambient,null,new RectF(-xy[0],-xy[1],ambientW-xy[0],ambientH-xy[1]),sample);c.restore();}super.draw(c);}}

 public static final class Backdrop extends View {
  Paint p=new Paint(3);public Backdrop(Context c){super(c);}
  @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);if(w==0||h==0)return;try{Bitmap b=Bitmap.createBitmap(Math.max(1,w/8),Math.max(1,h/8),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.scale(b.getWidth()/(float)w,b.getHeight()/(float)h);onDraw(c);BlurController.boxBlur(b,3);ambient=b;ambientW=w;ambientH=h;}catch(OutOfMemoryError ignored){}}
    protected void onDraw(Canvas c){
      float w=getWidth(),h=getHeight();
      int[] bg=dark?new int[]{0xff090a0d,0xff0b0c10,0xff090a0d}:new int[]{0xffe6efff,0xfff8f5fb,0xffe8f7f2};
     p.setShader(new LinearGradient(0,0,w,h,bg,null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
     if(!dark){
       p.setShader(new RadialGradient(w*.10f,h*.12f,Math.max(w,h)*.72f,new int[]{0xb0a1c9ff,0x62a1c9ff,0x00a1c9ff},new float[]{0,.5f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
       p.setShader(new RadialGradient(w*.98f,h*.42f,Math.max(w,h)*.68f,new int[]{0x9bf1a9d0,0x58f1a9d0,0x00f1a9d0},new float[]{0,.55f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
       p.setShader(new RadialGradient(w*.05f,h*.78f,Math.max(w,h)*.66f,new int[]{0x91c6b8ff,0x4fc6b8ff,0x00c6b8ff},new float[]{0,.52f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
       p.setShader(new RadialGradient(w*.95f,h*.98f,Math.max(w,h)*.68f,new int[]{0xa0a9ecd7,0x58a9ecd7,0x00a9ecd7},new float[]{0,.54f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
       p.setShader(new RadialGradient(w*.48f,h*.42f,Math.max(w,h)*.55f,new int[]{0x5effffff,0x00ffffff},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
      }
     p.setShader(null);
    }
 }
 public static class Icon extends Drawable {
  final String name;final int color;final Paint p=new Paint(3);final Path path=new Path();
  public Icon(String name,int c){this.name=name;color=c;}
  public void draw(Canvas c){c.save();Rect b=getBounds();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);p.setColor(color);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);path.reset();
   switch(name){
    case "folder":path.moveTo(3,7);path.lineTo(3,19);path.quadTo(3,20,5,20);path.lineTo(20,20);path.quadTo(22,20,22,18);path.lineTo(22,8);path.lineTo(12,8);path.lineTo(9,4);path.lineTo(5,4);path.quadTo(3,4,3,7);path.close();c.drawPath(path,p);break;
    case "search":c.drawCircle(10.5f,10.5f,6.8f,p);c.drawLine(16,16,21,21,p);break;
    case "back":path.moveTo(15,5);path.lineTo(8,12);path.lineTo(15,19);c.drawPath(path,p);break;
    case "up":path.moveTo(5,14);path.lineTo(12,7);path.lineTo(19,14);c.drawPath(path,p);break;
    case "plus":c.drawLine(12,4,12,20,p);c.drawLine(4,12,20,12,p);break;
    case "info":c.drawCircle(12,12,9,p);p.setStyle(Paint.Style.FILL);c.drawCircle(12,7,1.2f,p);p.setStyle(Paint.Style.STROKE);c.drawLine(12,11,12,17,p);break;
    case "more":p.setStyle(Paint.Style.FILL);for(int i=0;i<3;i++)c.drawCircle(12,5+i*7,1.6f,p);break;
     case "select_all":for(int i=0;i<4;i++){float x=2+(i%2)*10,y=2+(i/2)*10;c.drawRoundRect(x,y,x+8,y+8,1.5f,1.5f,p);path.moveTo(x+1.5f,y+4);path.lineTo(x+3.4f,y+6);path.lineTo(x+6.8f,y+1.5f);c.drawPath(path,p);path.reset();}break;
     case "clear_selection":c.drawRoundRect(3,3,17,19,2,2,p);c.drawCircle(17,17,5,p);c.drawLine(15,15,19,19,p);c.drawLine(19,15,15,19,p);break;
     case "archive_iso":c.drawRoundRect(3,2,15,19,2,2,p);c.drawLine(6,7,12,7,p);c.drawLine(6,10,12,10,p);c.drawLine(6,13,10,13,p);c.drawCircle(17,17,5,p);c.drawCircle(17,17,1.3f,p);break;
      case "zip":c.drawRoundRect(4,2,20,22,2,2,p);c.drawLine(7,5,17,5,p);c.drawLine(7,8,17,8,p);c.drawLine(12,5,12,15,p);for(int i=0;i<4;i++)c.drawRect(10.8f,9+i*2,13.2f,10+i*2,p);c.drawRoundRect(10,16,14,19,1,1,p);break;
      case "extract":case "unzip":c.drawRoundRect(3,2,19,20,2,2,p);c.drawLine(6,6,16,6,p);c.drawLine(6,10,16,10,p);c.drawLine(6,14,11,14,p);c.drawLine(15,13,15,21,p);path.moveTo(12,18);path.lineTo(15,21);path.lineTo(18,18);c.drawPath(path,p);break;
      case "bin":case "delete_selected":c.drawLine(5,6,19,6,p);c.drawLine(9,4,15,4,p);c.drawRoundRect(7,7,17,21,2,2,p);c.drawLine(10,10,10,18,p);c.drawLine(14,10,14,18,p);c.drawLine(4,6,5,8,p);c.drawLine(20,6,19,8,p);break;
     case "copy":c.drawRoundRect(7,2,19,16,2,2,p);c.drawRoundRect(3,7,15,22,2,2,p);break;
     case "paste":c.drawRoundRect(4,5,20,22,2,2,p);c.drawRoundRect(8,2,16,7,2,2,p);c.drawLine(8,11,16,11,p);c.drawLine(8,15,16,15,p);c.drawLine(8,19,14,19,p);break;
     case "move":c.drawRoundRect(7,7,17,17,1,1,p);c.drawLine(12,2,12,7,p);c.drawLine(10,4,12,2,p);c.drawLine(14,4,12,2,p);c.drawLine(12,17,12,22,p);c.drawLine(10,20,12,22,p);c.drawLine(14,20,12,22,p);c.drawLine(2,12,7,12,p);c.drawLine(4,10,2,12,p);c.drawLine(4,14,2,12,p);c.drawLine(17,12,22,12,p);c.drawLine(20,10,22,12,p);c.drawLine(20,14,22,12,p);break;
     case "refresh":c.drawArc(new RectF(3,3,21,21),-45,145,false,p);c.drawArc(new RectF(3,3,21,21),135,145,false,p);path.moveTo(17,3);path.lineTo(21,4);path.lineTo(20,8);path.moveTo(7,21);path.lineTo(3,20);path.lineTo(4,16);c.drawPath(path,p);break;
     case "sort_name":p.setStyle(Paint.Style.FILL);p.setTextSize(8);c.drawText("A",2,10,p);c.drawText("Z",2,20,p);p.setStyle(Paint.Style.STROKE);c.drawLine(18,4,18,19,p);path.moveTo(15,16);path.lineTo(18,20);path.lineTo(21,16);c.drawPath(path,p);break;
     case "sort_date":c.drawRoundRect(2,4,20,21,2,2,p);c.drawLine(2,9,20,9,p);c.drawLine(7,2,7,6,p);c.drawLine(15,2,15,6,p);c.drawCircle(17,17,5,p);c.drawLine(17,14,17,17,p);c.drawLine(17,17,19,18,p);break;
     case "sort_size":c.drawLine(3,6,18,6,p);c.drawLine(3,11,14,11,p);c.drawLine(3,16,10,16,p);c.drawLine(20,4,20,19,p);path.moveTo(17,16);path.lineTo(20,20);path.lineTo(23,16);c.drawPath(path,p);break;
     case "panel":c.drawRoundRect(2,4,10,20,1.5f,1.5f,p);c.drawRoundRect(14,4,22,20,1.5f,1.5f,p);break;
    case "image":c.drawRoundRect(3,3,21,21,4,4,p);c.drawCircle(8,8,1.5f,p);path.moveTo(4,18);path.lineTo(10,12);path.lineTo(14,16);path.lineTo(17,12);path.lineTo(21,16);c.drawPath(path,p);break;
    case "video":c.drawRoundRect(3,4,21,20,4,4,p);path.moveTo(10,8);path.lineTo(16,12);path.lineTo(10,16);path.close();c.drawPath(path,p);break;
    case "audio":path.moveTo(9,17);path.lineTo(9,5);path.lineTo(20,3);path.lineTo(20,15);c.drawPath(path,p);c.drawOval(3,16,9,21,p);c.drawOval(14,14,20,19,p);break;
    case "archive":c.drawRoundRect(4,3,20,21,3,3,p);for(int i=0;i<4;i++)c.drawRect(11,4+i*3,13,5+i*3,p);c.drawRect(10,16,14,19,p);break;
    case "apps":for(int i=0;i<4;i++)c.drawRoundRect(3+(i%2)*11,3+(i/2)*11,10+(i%2)*11,10+(i/2)*11,2,2,p);break;
    case "grid":for(int i=0;i<4;i++)c.drawRoundRect(3+(i%2)*10,3+(i/2)*10,10+(i%2)*10,10+(i/2)*10,2,2,p);break;
    case "list":for(int i=0;i<3;i++){c.drawRoundRect(3,4+i*6,7,8+i*6,1,1,p);c.drawLine(10,6+i*6,21,6+i*6,p);}break;
    case "sort":c.drawLine(4,6,20,6,p);c.drawLine(7,12,17,12,p);c.drawLine(10,18,14,18,p);break;
    case "tools":path.moveTo(4,20);path.lineTo(13,11);path.cubicTo(9,2,18,1,21,4);path.lineTo(16,8);path.lineTo(19,11);path.lineTo(23,7);path.cubicTo(24,15,18,17,15,14);path.lineTo(7,22);path.close();c.drawPath(path,p);break;
    case "settings":c.drawCircle(12,12,4,p);c.drawCircle(12,12,8,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,p);}break;
    case "transfer":path.moveTo(3,7);path.lineTo(20,7);path.lineTo(16,3);path.moveTo(21,17);path.lineTo(4,17);path.lineTo(8,21);c.drawPath(path,p);break;
    case "root":path.moveTo(12,2);path.lineTo(21,6);path.lineTo(20,15);path.quadTo(17,20,12,22);path.quadTo(7,20,4,15);path.lineTo(3,6);path.close();c.drawPath(path,p);c.drawLine(8,12,11,15,p);c.drawLine(11,15,17,8,p);break;
    case "drive":c.drawRoundRect(2,6,22,19,4,4,p);c.drawLine(2,14,22,14,p);c.drawCircle(17,16.5f,.7f,p);break;
    case "disc":c.drawCircle(12,12,9,p);c.drawCircle(12,12,2.4f,p);c.drawLine(15,5,18,8,p);break;
    case "check":path.moveTo(5,12);path.lineTo(10,17);path.lineTo(20,6);c.drawPath(path,p);break;
    case "star":for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?10:4.5f;float x=12+(float)Math.cos(a)*r,y=12+(float)Math.sin(a)*r;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();c.drawPath(path,p);break;
    default:c.drawRoundRect(5,2,19,22,3,3,p);c.drawLine(8,9,16,9,p);c.drawLine(8,13,16,13,p);c.drawLine(8,17,13,17,p);
   }c.restore();
  }
  public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
  public static final class Folder3D extends Drawable {
   final int color;final Paint p=new Paint(3);final Path shell=new Path(),front=new Path();
   public Folder3D(int color){this.color=color;}
   public void draw(Canvas c){
    Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);
    shell.reset();shell.moveTo(2.5f,7);shell.quadTo(2.5f,5,4.5f,5);shell.lineTo(8.7f,5);shell.quadTo(9.5f,5,10.2f,6);shell.lineTo(12,8);shell.lineTo(19.7f,8);shell.quadTo(22,8,22,10);shell.lineTo(21,18.2f);shell.quadTo(20.8f,20,19,20);shell.lineTo(4.3f,20);shell.quadTo(2.5f,20,2.5f,18);shell.close();
    p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(2,4,20,21,new int[]{Icon3D.light(color,48),color,Icon3D.darken(color,38)},null,Shader.TileMode.CLAMP));p.setShadowLayer(2.5f,0,2,0x550b1825);c.drawPath(shell,p);p.clearShadowLayer();p.setShader(null);
    front.reset();front.moveTo(3,10);front.lineTo(21.5f,9.2f);front.lineTo(20.7f,18);front.quadTo(20.5f,19,19.2f,19);front.lineTo(4.2f,19);front.quadTo(3,19,3,17.8f);front.close();
    p.setShader(new LinearGradient(4,9,17,20,new int[]{Icon3D.light(color,26),color,Icon3D.darken(color,28)},null,Shader.TileMode.CLAMP));c.drawPath(front,p);p.setShader(null);
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.75f);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(0x8cffffff);c.drawPath(front,p);p.setStyle(Paint.Style.FILL);p.setColor(0x55ffffff);p.setStrokeWidth(.7f);c.drawLine(4,10,19.8f,9.4f,p);c.restore();
   }
   public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
  }
  public static final class Icon3D extends Drawable {final int color;final Paint p=new Paint(3);final Icon glyph;public Icon3D(String n,int c){color=c;glyph=new Icon(n,0xffffffff);}public void draw(Canvas c){Rect b=getBounds();float r=Math.min(b.width(),b.height())*.25f;c.save();p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{light(color,34),color,darken(color,36)},null,Shader.TileMode.CLAMP));p.setShadowLayer(3,0,3,0x440b2345);c.drawRoundRect(new RectF(b.left+2,b.top+3,b.right-2,b.bottom-1),r,r,p);p.clearShadowLayer();p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0x88ffffff);c.drawRoundRect(new RectF(b.left+3,b.top+3,b.right-3,b.bottom-3),r,r,p);p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(b.left,b.top,b.left,b.top+b.height()*.55f,0x66ffffff,0x00ffffff,Shader.TileMode.CLAMP));c.drawRoundRect(new RectF(b.left+4,b.top+4,b.right-4,b.top+b.height()*.52f),r,r,p);p.setShader(null);glyph.setBounds(b.left+b.width()/4,b.top+b.height()/4,b.right-b.width()/4,b.bottom-b.height()/4);glyph.draw(c);c.restore();}static int light(int c,int n){return Color.rgb(Math.min(255,Color.red(c)+n),Math.min(255,Color.green(c)+n),Math.min(255,Color.blue(c)+n));}static int darken(int c,int n){return Color.rgb(Math.max(0,Color.red(c)-n),Math.max(0,Color.green(c)-n),Math.max(0,Color.blue(c)-n));}public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}}
 public static class Ring extends View {Paint p=new Paint(3);float value;public Ring(Context c,float v){super(c);value=v;}protected void onDraw(Canvas c){float d=Math.min(getWidth(),getHeight()),s=d*.095f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(dark?0xff31435e:0xffdeebfe);RectF r=new RectF(s,s,d-s,d-s);c.drawArc(r,-90,360,false,p);p.setColor(BLUE);c.drawArc(r,-90,360*Math.min(1,value),false,p);p.setStyle(Paint.Style.FILL);p.setColor(INK);p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextSize(d*.22f);String v=Math.round(value*100)+"%";c.drawText(v,(d-p.measureText(v))/2,d*.56f,p);}}
}
