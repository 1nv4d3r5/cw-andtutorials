package apt.tutorial.two;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Intent i=new Intent(context, PostMonitor.class);
		
		i.setAction(PostMonitor.POLL_ACTION);
		
		WakefulIntentService.sendWakefulWork(context, i);
	}
}
