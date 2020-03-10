package server.tcp;

import java.util.List;

import server.tcp.UserInfo.Message;

public interface IServer {

	void onClientDisconnected(ConnectionHandler connectionHandler);

	boolean userExists(String username);
	
	boolean login(String username, String password, ConnectionHandler connection);
	
	public boolean isAuthenticated(ConnectionHandler connection);
	
	String[] getUsers();
	
	boolean sendMessage(ConnectionHandler connection, String user, String message);
	
	boolean sendFile(ConnectionHandler connection, String user, String message, byte[] content);
	
	Message receiveMessage(ConnectionHandler connection);
	
	Message receiveFile(ConnectionHandler connection);
}
