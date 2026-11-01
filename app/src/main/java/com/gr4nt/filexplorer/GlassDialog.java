package com.gr4nt.filexplorer;
import android.app.*;import android.content.*;import android.content.res.ColorStateList;import android.graphics.Color;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;
/** Shared glass treatment for every app-owned modal, including native button panels. */
public final class GlassDialog extends AlertDialog {
 final Activity activity;boolean acquired;
 GlassDialog(Activity c){super(c,theme(c));activity=c;}
 static int theme(Activity c){return c.getResources().getIdentifier(Visuals.dark?"GlassDialogDark":"GlassDialogLight","style",c.getPackageName());}
 @Override protected void onStart(){super.onStart();style(this,activity);if(!acquired){BlurController.acquire(activity);acquired=true;}}
 @Override protected void onStop(){if(acquired){BlurController.release(activity);acquired=false;}super.onStop();}
 static void style(Dialog dialog,Activity activity){Window w=dialog.getWindow();if(w==null)return;int d=Visuals.dp(activity,1);w.setBackgroundDrawable(new InsetDrawable(Visuals.glass(d*30,false),d*6));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(Visuals.dark?.40f:.16f);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);w.setLayout(Math.min(activity.getResources().getDisplayMetrics().widthPixels-d*28,d*640),-2);
  if(Build.VERSION.SDK_INT>=31)w.setBackgroundBlurRadius(Visuals.blur?d*22:0);
  WindowManager.LayoutParams lp=w.getAttributes();lp.windowAnimations=Visuals.animate()?activity.getResources().getIdentifier("GlassMotion","style",activity.getPackageName()):0;w.setAttributes(lp);
  tint(w.getDecorView(),true);View panel=w.getDecorView().findViewById(android.R.id.content);if(panel!=null&&Visuals.animate()){panel.setAlpha(0);panel.setScaleX(.97f);panel.setScaleY(.97f);panel.animate().alpha(1).scaleX(1).scaleY(1).setDuration(220).setInterpolator(new android.view.animation.PathInterpolator(.2f,0,0,1)).start();}
 }
 static void tint(View view,boolean nativePanel){
  String name="";try{name=view.getResources().getResourceEntryName(view.getId());}catch(Exception ignored){}
  if(name.equals("parentPanel")||name.equals("customPanel")||name.equals("contentPanel")||name.equals("topPanel")||name.equals("buttonPanel")||name.equals("custom"))view.setBackgroundColor(Color.TRANSPARENT);
  if(view instanceof TextView){TextView t=(TextView)view;t.setTextColor(view instanceof Button?Visuals.BLUE:Visuals.INK);t.setHintTextColor(Visuals.MUTED);if(view instanceof EditText)t.setBackgroundTintList(ColorStateList.valueOf(Visuals.BLUE));}
  if(view instanceof CompoundButton)((CompoundButton)view).setButtonTintList(ColorStateList.valueOf(Visuals.BLUE));
  if(view instanceof Button){view.setBackground(Visuals.button(Visuals.dp(view.getContext(),15)));Visuals.press(view);}
  if(view instanceof ProgressBar)((ProgressBar)view).setIndeterminateTintList(ColorStateList.valueOf(Visuals.BLUE));
  if(view instanceof ListView){ListView list=(ListView)view;list.setDivider(new ColorDrawable(Color.TRANSPARENT));list.setDividerHeight(Visuals.dp(view.getContext(),9));list.setPadding(Visuals.dp(view.getContext(),4),Visuals.dp(view.getContext(),5),Visuals.dp(view.getContext(),4),Visuals.dp(view.getContext(),7));list.setClipToPadding(false);list.setSelector(Visuals.button(Visuals.dp(view.getContext(),14)));}
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)tint(group.getChildAt(i),nativePanel);}
 }
 public static class Builder {
  final Activity activity;String title,message;View view;String[] items;DialogInterface.OnClickListener selected,positive,negative,neutral;String yes,no,extra;
  public Builder(Activity c){activity=c;}
  public Builder setTitle(String s){title=s;return this;}public Builder setMessage(String s){message=s;return this;}public Builder setView(View v){view=v;return this;}
  public Builder setItems(String[] s,DialogInterface.OnClickListener action){items=s;selected=action;return this;}
  public Builder setPositiveButton(String s,DialogInterface.OnClickListener action){yes=s;positive=action;return this;}public Builder setNegativeButton(String s,DialogInterface.OnClickListener action){no=s;negative=action;return this;}public Builder setNeutralButton(String s,DialogInterface.OnClickListener action){extra=s;neutral=action;return this;}
  public GlassDialog create(){GlassDialog dialog=new GlassDialog(activity);if(title!=null)dialog.setTitle(title);if(message!=null)dialog.setMessage(message);if(items!=null){ListView list=new ListView(activity);list.setAdapter(adapter(activity,items));list.setOnItemClickListener((a,v,i,id)->{dialog.dismiss();if(selected!=null)selected.onClick(dialog,i);});int height=Math.min(items.length*Visuals.dp(activity,71),(int)(activity.getResources().getDisplayMetrics().heightPixels*.61));android.widget.FrameLayout frame=new android.widget.FrameLayout(activity);frame.addView(list,new android.widget.FrameLayout.LayoutParams(-1,height));dialog.setView(frame,Visuals.dp(activity,12),0,Visuals.dp(activity,12),0);}else if(view!=null)dialog.setView(view);if(yes!=null)dialog.setButton(-1,yes,positive);if(no!=null)dialog.setButton(-2,no,negative);if(extra!=null)dialog.setButton(-3,extra,neutral);return dialog;}
  public GlassDialog show(){GlassDialog d=create();d.show();return d;}
 }
 public static android.widget.BaseAdapter adapter(Activity activity,String[] values){return new BaseAdapter(){public int getCount(){return values.length;}public Object getItem(int i){return values[i];}public long getItemId(int i){return i;}public View getView(int i,View reuse,ViewGroup group){TextView t=reuse instanceof TextView?(TextView)reuse:new TextView(activity);t.setText(values[i]);t.setTextColor(Visuals.INK);t.setTextSize(15);t.setGravity(Gravity.CENTER_VERTICAL);t.setMinHeight(Visuals.dp(activity,62));t.setPadding(Visuals.dp(activity,18),Visuals.dp(activity,14),Visuals.dp(activity,18),Visuals.dp(activity,14));t.setBackground(Visuals.softGlass(Visuals.dp(activity,18),false));return t;}};}
 public static class Progress extends ProgressDialog {final Activity activity;boolean acquired;public Progress(Activity c){super(c,theme(c));activity=c;}@Override public void onStart(){super.onStart();style(this,activity);BlurController.acquire(activity);acquired=true;}@Override protected void onStop(){if(acquired){BlurController.release(activity);acquired=false;}super.onStop();}}
 public static class Editor extends Dialog {final Activity activity;public Editor(Activity c){super(c,theme(c));activity=c;requestWindowFeature(Window.FEATURE_NO_TITLE);}@Override protected void onStart(){super.onStart();style(this,activity);getWindow().setLayout(-1,-1);}}
}
