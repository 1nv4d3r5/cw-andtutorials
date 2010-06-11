package apt.tutorial.three;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Intent i=new Intent(context, TwitterMonitor.class);
		
		i.setAction(TwitterMonitor.POLL_ACTION);
		
		WakefulIntentService.sendWakefulWork(context, i);
	}
}
