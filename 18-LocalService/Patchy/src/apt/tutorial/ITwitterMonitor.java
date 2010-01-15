package apt.tutorial;

import apt.tutorial.ITwitterListener;

public interface ITwitterMonitor {
	void registerAccount(String user, String password,
												ITwitterListener callback);
	void removeAccount(ITwitterListener callback);
}