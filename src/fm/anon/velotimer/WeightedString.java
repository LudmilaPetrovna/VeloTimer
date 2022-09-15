package fm.anon.velotimer;

import java.util.Random;

public class WeightedString extends RandomString{
	public String[] texts=null;
	public int[] weights;
	private Random r;

	public WeightedString(String[] srcTexts){
		texts=srcTexts;
		weights=new int[texts.length];
		r=new Random(System.currentTimeMillis());
		r.longs(texts.length);
	}

	@Override
	public String getString(){
		String ret=null;
		int score=(weights[0]+100)*100;
		int rolls=10;
		int t;
		while(rolls>=0){
			t=r.nextInt(texts.length);
			if(weights[t]<score){
				score=weights[t];
				ret=texts[t];
				weights[t]++;
			}
			rolls--;
		}
		return(ret);
	}
}
