package apt.tutorial.three;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArrayList;
import winterwell.jtwitter.Twitter;
import apt.tutorial.ITwitterListener;
import apt.tutorial.ITwitterMonitor;

public class TwitterMonitor extends Service {
	public static final String STATUS_UPDATE="apt.tutorial.three.STATUS_UPDATE";
	public static final String FRIEND="apt.tutorial.three.FRIEND";
	public static final String STATUS="apt.tutorial.three.STATUS";
	public static final String CREATED_AT="apt.tutorial.three.CREATED_AT";
	private static final int NOTIFY_ME_ID=1337;
	private static final int POLL_PERIOD=60000;
	private static final int INITIAL_POLL_PERIOD=1000;
	private int pollPeriod=INITIAL_POLL_PERIOD;
	private AtomicBoolean active=new AtomicBoolean(true);
	private Set<Long> seenStatus=new HashSet<Long>();
	private Map<ITwitterListener, Account> accounts=
					new ConcurrentHashMap<ITwitterListener, Account>();
	private CopyOnWriteArrayList<String> bff=new CopyOnWriteArrayList<String>();
	private NotificationManager mgr=null;
	private AtomicBoolean isBatteryLow=new AtomicBoolean(false);
	
	@Override
	public void onCreate() {
		super.onCreate();

		mgr=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);		
		new Thread(threadBody).start();
		registerReceiver(onBatteryChanged,
											new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return(binder);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(onBatteryChanged);
		active.set(false);
	}
	
	synchronized private void poll(Account l) {
		try {
			Twitter client=new Twitter(l.user, l.password);
			List<Twitter.Status> timeline=client.getFriendsTimeline();
			for (Twitter.Status s : timeline) {
				if (!seenStatus.contains(s.id)) {
					try {
						Intent broadcast=new Intent(STATUS_UPDATE);
						broadcast.putExtra(FRIEND, s.user.screenName);
						broadcast.putExtra(STATUS, s.text);
						broadcast.putExtra(CREATED_AT,
																s.createdAt.toString());
						sendBroadcast(broadcast);
						seenStatus.add(s.id);
					}
					catch (Throwable t) {
						Log.e("TwitterMonitor", "Exception in callback", t);
					}
					if (bff.contains(s.user.screenName)) {
						notify(s.user.screenName);
					}
				}
			}
		}
		catch (Throwable t) {
			Log.e("TwitterMonitor", "Exception in poll()", t);
		}
	}
	
	private void notify(String friend) {
		Notification note=new Notification(R.drawable.red_ball,
																			 "Tweet!",
																			 System.currentTimeMillis());
		Intent i=new Intent();
		
		i.setClassName("apt.tutorial.two",
									"apt.tutorial.two.Patchy");
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		PendingIntent pi=PendingIntent.getActivity(this, 0, i,
																							 0);
		
		note.setLatestEventInfo(this, "Tweet!",
													 friend+" updated their Twitter status",
													 pi);
		mgr.notify(NOTIFY_ME_ID, note);
	}
	
	private Runnable threadBody=new Runnable() {
		public void run() {
			while (active.get()) {
				for (Account l : accounts.values()) {
					poll(l);
					pollPeriod=POLL_PERIOD;
					
					if (isBatteryLow.get()) {
						pollPeriod*=10;
					}
				}
				
				SystemClock.sleep(pollPeriod);
			}
		}
	};
	
	class Account {
		String user=null;
		String password=null;
		ITwitterListener callback=null;
		
		Account(String user, String password,
						 ITwitterListener callback) {
			this.user=user;
			this.password=password;
			this.callback=callback;
		}
	}
	
	private final ITwitterMonitor.Stub binder=new ITwitterMonitor.Stub() {
		public void registerAccount(String user, String password,
																	ITwitterListener callback) {
			Account l=new Account(user, password, callback);
			
			poll(l);
			accounts.put(callback, l);
		}
		
		public void removeAccount(ITwitterListener callback) {
			accounts.remove(callback);
		}
		
		public void setBestFriends(ITwitterListener callback,
																List<String> newBff) {
			bff.clear();
			bff.addAll(newBff);
		}
		
		public void updateTimeline() {
			new Thread(new Runnable() {
				public void run() {
					for (Account l : accounts.values()) {
						poll(l);
					}
				}
			}).start();
		}
	};
	
	BroadcastReceiver onBatteryChanged=new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			int pct=100*intent.getIntExtra("level", 1)/intent.getIntExtra("scale", 1);
			
			isBatteryLow.set(pct<=25);
		}
	};
}