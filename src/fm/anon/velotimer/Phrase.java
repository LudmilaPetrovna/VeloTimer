package fm.anon.velotimer;

public class Phrase{
	public String text;
	public boolean isUrgent;
	public boolean isRepeatable;

	public Phrase(String message,boolean urgent,boolean repeatable){
		text=message;
		isUrgent=urgent;
		isRepeatable=repeatable;
	}
}
