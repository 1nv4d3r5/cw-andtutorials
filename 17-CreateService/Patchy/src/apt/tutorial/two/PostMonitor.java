package apt.tutorial.two;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import winterwell.jtwitter.Twitter;
import apt.tutorial.IPostMonitor;

public class PostMonitor extends Service {
	private static final int POLL_PERIOD=60000;
	private AtomicBoolean active=new AtomicBoolean(true);
	private Set<Long> seenStatus=new HashSet<Long>();
	private final Binder binder=new LocalBinder();
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		new Thread(threadBody).start();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return(binder);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		active.set(false);
	}
	
	private void poll() {
		Twitter client=new Twitter();	// need credentials!
		List<Twitter.Status> timeline=client.getFriendsTimeline();
		
		for (Twitter.Status s : timeline) {
			if (!seenStatus.contains(s.id)) {
				// found a new one!
				seenStatus.add(s.id);
			}
		}
	}
	
	private Runnable threadBody=new Runnable() {
		public void run() {
			while (active.get()) {
				poll();				
				SystemClock.sleep(POLL_PERIOD);
			}
		}
	};
	
	public class LocalBinder extends Binder implements IPostMonitor {
		void registerAccount(String user, String password) {
		}
	}
}
