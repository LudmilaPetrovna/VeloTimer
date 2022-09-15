package fm.anon.velotimer;

import java.util.Random;

public class OrderedString extends RandomString{
	public int[] ind;
	public int current;
	public String[] texts=null;
	private boolean isLoop;
	private boolean isLoopShuffle;
	Random r;

	public OrderedString(boolean loop,boolean shuffleOnLoop,String[] srcTexts){
		texts=srcTexts;
		isLoop=loop;
		isLoopShuffle=shuffleOnLoop;
		r=new Random(System.currentTimeMillis());
		shuffle();
	}

	@Override
	public String getString(){
		if(isLoop&&current>=texts.length){
			current=0;
			if(isLoopShuffle){
				shuffle();
			}
		}
		if(texts!=null&&current<texts.length){ return(texts[current++]); }
		return(null);
	}

	void shuffle(){
		current=0;
		int q,ri,t;
		ind=new int[texts.length];
		for(q=0;q<texts.length;q++){
			ind[q]=q;
		}
		for(q=0;q<texts.length;q++){
			ri=r.nextInt(texts.length);
			t=ind[ri];
			ind[ri]=ind[q];
			ind[q]=t;
		}
	}
}
