package apt.tutorial;

import apt.tutorial.IPostListener;

public interface IPostMonitor {
	void registerAccount(String user, String password,
												IPostListener callback);
	void removeAccount(IPostListener callback);
}