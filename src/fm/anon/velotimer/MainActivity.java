package fm.anon.velotimer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.AssetManager;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener,Runnable,OnUtteranceCompletedListener,OnClickListener{
	public static final String TAG="VELOTIMER";
	private TextToSpeech tts;
	private boolean ttsReady=false;
	private Phrasegen gen=new Phrasegen();
	AssetManager ass=null;
	Handler h=new Handler();
	int duration=1*60;
	long started;
	long lastSpeak=0;
	boolean isRunning;
	boolean isPause;

	//ui
	LinearLayout topButtons;
	LinearLayout timeSelection;
	LinearLayout bottomControls;
	TextView clock;
	LinearLayout main;

	
	// options
	int speechPauseMs=3000;
	boolean sayPercent=true;
	boolean mute=false;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT,1.0f);
		ScrollView scroll=new ScrollView(this);
		scroll.setLayoutParams(p);
		main=new LinearLayout(this);
		main.setOrientation(LinearLayout.VERTICAL);
		main.setLayoutParams(p);

		// read txt-files in Assets and build dialogs
		topButtons=new LinearLayout(this);
		topButtons.setOrientation(LinearLayout.HORIZONTAL);
		topButtons.setLayoutParams(p);
		ass=getAssets();
		String[] assets=null;
		try{
			assets=ass.list("");
			Arrays.sort(assets);
		}catch(IOException e){
		}
		for(String asset:assets){
			if(asset.contains(".txt")){
				Button b=new Button(this);
				b.setText(asset.replace(".txt",""));
				b.setSingleLine(true);
				b.setTag("view:"+asset);
				b.setOnClickListener(this);
				b.setLayoutParams(p);
				topButtons.addView(b);
			}
		}
		topButtons.setGravity(Gravity.FILL_HORIZONTAL);
		topButtons.setBackgroundColor(0x88000000);
		main.addView(topButtons);
		// add time selection
		timeSelection=new LinearLayout(this);
		timeSelection.setOrientation(LinearLayout.VERTICAL);
		TextView tit=new TextView(this);
		tit.setText("Выберите длительность тренировки");
		tit.setGravity(Gravity.CENTER_HORIZONTAL);
		timeSelection.addView(tit);
		int q,w;
		LinearLayout h=new LinearLayout(this);
		for(w=0;w<5;w++){
			h=new LinearLayout(this);
			h.setOrientation(LinearLayout.HORIZONTAL);
			h.setLayoutParams(p);
			for(q=0;q<5;q++){
				int sec=(q*5+w*25)*60;
				Button t=new Button(this);
				t.setText(getTimeMinutes(sec));
				t.setTextSize(TypedValue.COMPLEX_UNIT_DIP,55);
				t.setPadding(0,0,0,0);
				t.setTag("train:"+sec);
				t.setOnClickListener(this);
				t.setLayoutParams(p);
				h.addView(t);
			}
			timeSelection.addView(h);
		}
		main.addView(timeSelection);
		clock=new TextView(this);
		clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP,getTextsizeFillScreen("-00:00:00-"));
		clock.setGravity(Gravity.CENTER_HORIZONTAL);
		clock.setSingleLine(true);
		clock.setPadding(0,0,0,0);
		main.addView(clock);
		bottomControls=new LinearLayout(this);
		bottomControls.setOrientation(LinearLayout.HORIZONTAL);
		bottomControls.setLayoutParams(p);
		Button btn;
		btn=new Button(this);
		btn.setText("Пауза (pause)");
		btn.setTag("pause");
		btn.setLayoutParams(p);
		btn.setOnClickListener(this);
		bottomControls.addView(btn);
		btn=new Button(this);
		btn.setText("Заткнись (mute)");
		btn.setTag("mute");
		btn.setLayoutParams(p);
		btn.setOnClickListener(this);
		bottomControls.addView(btn);
		btn=new Button(this);
		btn.setText("Нахуй (reset)");
		btn.setTag("reset");
		btn.setLayoutParams(p);
		btn.setOnClickListener(this);
		bottomControls.addView(btn);
		btn=new Button(this);
		btn.setText("Свернуть (pip)");
		btn.setTag("pip");
		btn.setLayoutParams(p);
		btn.setOnClickListener(this);
		bottomControls.addView(btn);
		main.addView(bottomControls);
		bottomControls.setVisibility(View.GONE);
		tts=new TextToSpeech(this,this);
		scroll.addView(main);
		setContentView(scroll);
	}

	int getTextsizeFillScreen(String sample){

		DisplayMetrics dm=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		TextView dummy=new TextView(this);
		dummy.setText(sample);

		Rect tr=new Rect();
		int min=5;
		int max=1000;
		int cur;
		while(true){
			cur=(max-min)/2+min;
			dummy.setTextSize(TypedValue.COMPLEX_UNIT_DIP,cur);
			dummy.getPaint().getTextBounds(sample,0,sample.length(),tr);
			Log.e("SCALE",min+"..."+max+"="+tr.width()+"<"+dm.widthPixels+"&&"+tr.height()+"<"+dm.heightPixels);
			if(tr.width()<dm.widthPixels&&tr.height()<dm.heightPixels){
				min=cur;
			} else {
				max=cur;
			}
			if(max-min<10){break;}
			
		}

		return(min);

	}

	String getTimeMinutes(int seconds){
		int hours=seconds/3600;
		int minutes=(seconds/60)%60;
		seconds=seconds%60;
		return String.format("%02d:%02d",hours,minutes);
	}

	String getTimeSeconds(int seconds){
		int hours=seconds/3600;
		int minutes=(seconds/60)%60;
		seconds=seconds%60;
		return String.format("%02d:%02d:%02d",hours,minutes,seconds);
	}

	@Override
	protected void onPause(){
		super.onPause();
	}

	@Override
	@Deprecated
	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode){
		// TODO Auto-generated method stub
		super.onPictureInPictureModeChanged(isInPictureInPictureMode);
		clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP,getTextsizeFillScreen("-00:00:00-"));

	}

	@Override
	public void onPointerCaptureChanged(boolean hasCapture){
		// TODO Auto-generated method stub
	}

	@Override
	public void onInit(int status){
		if(status==TextToSpeech.SUCCESS){
			int result=tts.setLanguage(Locale.forLanguageTag("RU"));
			if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED){
				Log.e(TAG,"This Language is not supported");
				return;
			}
			tts.setOnUtteranceCompletedListener(this);
			// tts.setOnUtteranceProgressListener
			ttsReady=true;
		}else{
			Log.e(TAG,"Can't init TTS Engine");
		}
	}

	@Override
	public void run(){
		while(isRunning){
			h.post(new Runnable(){
				@Override
				public void run(){
					long now=System.currentTimeMillis();
					if(isPause){
						started+=1000;
					}
					int passed=(int)((now-started)/1000);
					boolean isSpeechPause=now-lastSpeak>speechPauseMs;
					clock.setText(getTimeSeconds(passed));
					if(!tts.isSpeaking()){
						Phrase ph=gen.getPhrase(!isSpeechPause||mute,passed,duration,0,0,0,0);
						if(ph!=null){
							TextView l=new TextView(MainActivity.this);
							l.setText(ph.text);
							l.setTextSize(TypedValue.COMPLEX_UNIT_DIP,10);
							main.addView(l);
							gen.setSpoken(ph);
							if(!mute){
								//tts.speak(ph.text+(ph.isRepeatable?". повтори! ":""),TextToSpeech.QUEUE_ADD,null,"123");
								tts.speak(ph.text,TextToSpeech.QUEUE_ADD,null,"123");
							}
						}
					}
				}
			});
			try{
				Thread.sleep(1000);
			}catch(InterruptedException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	@Deprecated
	public void onUtteranceCompleted(String arg0){
		lastSpeak=System.currentTimeMillis();
	}

	@Override
	public void onClick(View v){
		String tag=(String)v.getTag();
		String[] tagParam=tag.split(":");
		if(tagParam[0].equals("view")){
			try{
				InputStream in=ass.open(tagParam[1]);
				byte[] buf=new byte[in.available()];
				in.read(buf);
				in.close();
				AlertDialog d=new AlertDialog.Builder(MainActivity.this).create();
				d.setCancelable(true);
				d.setTitle(tagParam[1]);
				d.setMessage(new String(buf));
				d.show();
			}catch(IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
				clock.setText(e.toString());
			}
		}
		if(tagParam[0].equals("train")){
			duration=Integer.parseInt(tagParam[1],10);
			started=System.currentTimeMillis();
			bottomControls.setVisibility(View.VISIBLE);
			topButtons.setVisibility(View.GONE);
			timeSelection.setVisibility(View.GONE);
			clock.setVisibility(View.VISIBLE);
			isRunning=true;

			new Thread(this).start();
		}
		if(tagParam[0].equals("pause")){
			isPause=!isPause;
		}
		if(tagParam[0].equals("pip")){
			enterPictureInPictureMode();
		}
		if(tagParam[0].equals("reset")){
			isRunning=false;
			bottomControls.setVisibility(View.GONE);
			topButtons.setVisibility(View.VISIBLE);
			timeSelection.setVisibility(View.VISIBLE);
			clock.setVisibility(View.GONE);
			
		}
		if(tagParam[0].equals("mute")){
			if(!mute){
				tts.stop();
			}
			mute=!mute;
		}
	}
}
