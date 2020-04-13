package lpi.server.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Defines the operations provided by the server.
 * 
 * @author RST
 */
@Path("server")
@Produces(MediaType.APPLICATION_JSON)
public interface IServer {

	/**
	 * Simplest method that does not accept any parameters and does not return
	 * any result. The easiest way to ensure everything works as expected.
	 */
	@GET
	@Path("ping")
	@Produces(MediaType.TEXT_PLAIN)
	public String ping(@Context Request request);

	/**
	 * Next method to test client-server communication and parameter passing.
	 * 
	 * @param text
	 *            Any text you want to send to the server.
	 * @return The text you sent prepended with the "ECHO:".
	 */
	@POST
	@Path("echo")
	@Produces(MediaType.TEXT_PLAIN)
	public String echo(@Context Request request, String text);

	/**
	 * Allows logging in the user.
	 * 
	 * @param login
	 *            The unique name of the user.
	 * @param password
	 *            The password of the user.
	 * @return The String that specifies the welcome message. Operation result
	 *         is available in response code.
	 */
	@PUT
	@Path("user")
	@Produces(MediaType.TEXT_PLAIN)
	public Response login(@Context Request request, User userInfo);

	/**
	 * Provides a list of users currently active on the server.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @return An array of user names.
	 */
	@GET
	@Path("users")
	public Response listUsers(@Context Request request);

	/**
	 * Sends the message to the user, registered on the server.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @param msg
	 *            An message object that contains message information.
	 */
	@POST
	@Path("{username}/messages")
	public Response sendMessage(@Context Request request, @PathParam("username") String username, String msg);

	/**
	 * Receives a message if there are any pending messages addressed to the
	 * logged in user associated with the specified sessionId.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @return A message object or <b>null</b> if there are no pending messages.
	 */
	@GET
	@Path("{username}/messages")
	public Response receiveMessage(@Context Request request, @PathParam("username") String username);

	@GET
	@Path("{username}/messages/{id}")
	public Response receiveMessage(@Context Request request, @PathParam("username") String username,
			@PathParam("id") String messageId);

	@DELETE
	@Path("{username}/messages/{id}")
	public Response deleteMessage(@Context Request request, @PathParam("username") String username,
			@PathParam("id") String messageId);

	/**
	 * Sends the file to the user, registered on the server.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @param file
	 *            The file information that should be delivered to the receiver.
	 */
	@POST
	@Path("{username}/files")
	public Response sendFile(@Context Request request, @PathParam("username") String username, FileInfo file);

	/**
	 * Receives a file if there are any pending files addressed to the logged in
	 * user associated with the specified sessionId.
	 * 
	 * @param sessionId
	 *            The session id assigned to you after <b>login</b> call.
	 * @return A file info object describing the file targeted to a user,
	 *         registered on the server.
	 */
	@GET
	@Path("{username}/files")
	public Response receiveFile(@Context Request request, @PathParam("username") String login);

	@GET
	@Path("{username}/files/{id}")
	public Response receiveFile(@Context Request request, @PathParam("username") String username,
			@PathParam("id") String fileId);

	@DELETE
	@Path("{username}/files/{id}")
	public Response deleteFile(@Context Request request, @PathParam("username") String username,
			@PathParam("id") String fileId);

	@XmlRootElement
	public static class User {
		private String login;
		private String password;

		public String getLogin() {
			return login;
		}

		public void setLogin(String login) {
			this.login = login;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
	
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class WrappedList{
		
		public List<String> items;
		
		public WrappedList(){}
		
		public WrappedList(List<String> items){
			this.items = items;
		}
		
		public WrappedList(String[] items){
			this.items = new ArrayList<>(Arrays.asList(items));
		}
	}

	@XmlRootElement
	public static class Message {
		private String sender;
		private String message;

		public Message() {
		}

		public Message(String sender, String message) {
			this.sender = sender;
			this.message = message;
		}

		public String getSender() {
			return sender;
		}

		public void setSender(String sender) {
			this.sender = sender;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	@XmlRootElement
	public static class FileInfo {
		private String sender;
		private String filename;
		private String content;

		public FileInfo() {
		}

		public FileInfo(String sender, String filename, String content){
			this.sender = sender;
			this.filename = filename;
			this.content = content;
		}
		
		public String getSender() {
			return sender;
		}

		public void setSender(String sender) {
			this.sender = sender;
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}
}
