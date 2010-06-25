package apt.tutorial.two;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import winterwell.jtwitter.Twitter;
import apt.tutorial.IPostListener;
import apt.tutorial.IPostMonitor;

public class PostMonitor extends Service {
	public static final int NOTIFICATION_ID=1337;
	private static final String NOTIFY_KEYWORD="snicklefritz";
	private static final int POLL_PERIOD=60000;
	private AtomicBoolean active=new AtomicBoolean(true);
	private Set<Long> seenStatus=new HashSet<Long>();
	private Map<IPostListener, Account> accounts=
					new ConcurrentHashMap<IPostListener, Account>();
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
	
	private void poll(Account l) {
		try {
			Twitter client=new Twitter(l.user, l.password);

			client.setAPIRootUrl("https://identi.ca/api");

			List<Twitter.Status> timeline=client.getFriendsTimeline();
			
			for (Twitter.Status s : timeline) {
				if (!seenStatus.contains(s.id)) {
					l.callback.newFriendStatus(s.user.screenName, s.text,
																		 s.createdAt.toString());
					seenStatus.add(s.id);
					
					if (s.text.indexOf(NOTIFY_KEYWORD)>-1) {
						showNotification();
					}
				}
			}
		}
		catch (Throwable t) {
			android.util.Log.e("PostMonitor",
												 "Exception in poll()", t);
		}
	}
	
	private void showNotification() {
		final NotificationManager mgr=
			(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Notification note=new Notification(R.drawable.status,
																				"New matching post!",
																				System.currentTimeMillis());
		Intent i=new Intent(this, Patchy.class);
		
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
							 Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pi=PendingIntent.getActivity(this, 0,
																							i,
																							0);
		
		note.setLatestEventInfo(this, "Identi.ca Post!",
														"Found your keyword: "+NOTIFY_KEYWORD,
														pi);
		
		mgr.notify(NOTIFICATION_ID, note);
	}
	
	private Runnable threadBody=new Runnable() {
		public void run() {
			while (active.get()) {
				for (Account l : accounts.values()) {
					poll(l);
				}
				
				SystemClock.sleep(POLL_PERIOD);
			}
		}
	};
	
	class Account {
		String user=null;
		String password=null;
		IPostListener callback=null;
		
		Account(String user, String password,
						 IPostListener callback) {
			this.user=user;
			this.password=password;
			this.callback=callback;
		}
	}
	
	public class LocalBinder extends Binder implements IPostMonitor {
		public void registerAccount(String user, String password,
																	IPostListener callback) {
			Account l=new Account(user, password, callback);
			
			poll(l);
			accounts.put(callback, l);
		}
		
		public void removeAccount(IPostListener callback) {
			accounts.remove(callback);
		}
	}
}
