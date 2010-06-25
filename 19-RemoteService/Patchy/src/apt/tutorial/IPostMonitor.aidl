package apt.tutorial;

import apt.tutorial.IPostListener;

interface IPostMonitor {
	void registerAccount(String user, String password,
												IPostListener callback);
	void removeAccount(IPostListener callback);
}