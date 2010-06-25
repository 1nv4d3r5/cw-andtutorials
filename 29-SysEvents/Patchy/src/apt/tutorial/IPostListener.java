package apt.tutorial;

public interface IPostListener {
	void newFriendStatus(String friend, String status,
												String createdAt);
}