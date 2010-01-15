package apt.tutorial;

public interface ITwitterListener {
	void newFriendStatus(String friend, String status,
												String createdAt);
}