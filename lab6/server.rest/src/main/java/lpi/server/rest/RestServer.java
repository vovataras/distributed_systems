package lpi.server.rest;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.grizzly.http.server.HttpServer;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.util.Base64;

import javax.ws.rs.core.Response.Status;

public class RestServer implements Closeable, Runnable, IServer {

	private static RestServer instance;

	private String hostname = "localhost";
	private int port = 8080;
	private HttpServer server;
	private ConcurrentMap<String, UserInfo> users = new ConcurrentHashMap<>();

	private RestServer(String[] args) {
		if (args.length > 1) {
			try {
				this.hostname = args[0];
				this.port = Integer.parseInt(args[1]);
			} catch (Exception ex) {
				// if we failed to parse port out of parameters, we'll just use
				// the default ones.
			}
		}
	}

	public synchronized static RestServer createInstance(String[] args) {
		return instance = new RestServer(args);
	}

	public static RestServer get() {
		return instance;
	}

	public boolean userValid(String username, String password) {
		return true;
	}

	@Override
	public void run() {

		String uri = String.format("http://%s:%d/chat", this.hostname, this.port);

		ResourceConfig resourceConfig = new DefaultResourceConfig();
		resourceConfig.getSingletons().add(this);
		resourceConfig.getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters",
				"lpi.server.rest.AuthorizationFilter");

		try {
			this.server = GrizzlyServerFactory.createHttpServer(URI.create(uri), resourceConfig);
		} catch (IllegalArgumentException | NullPointerException | IOException e) {
			throw new RuntimeException("Failed to start REST Server", e);
		}
	}

	@Override
	public void close() throws IOException {
		if (this.server != null) {
			this.server.stop();
			this.server = null;
		}
	}

	@Override
	public String ping(Request request) {
		return "Pong!";
	}

	@Override
	public String echo(Request request, String text) {
		return String.format("'%s' receiving confirmed!", text);
	}

	@Override
	public Response login(Request request, User userInfo) {
		try {
			//
			// checking parameters
			//
			if (userInfo == null)
				return Response.status(Status.BAD_REQUEST).entity("Content with login and password has to be provided.")
						.build();

			if (userInfo.getLogin() == null || userInfo.getLogin().length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The login has to be specified.").build();

			if (userInfo.getPassword() == null || userInfo.getPassword().length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The password has to be specified.").build();

			//
			// Retrieving or registering user
			//
			boolean isNewUser = false;
			UserInfo newUser = new UserInfo(userInfo.getLogin(), userInfo.getPassword());

			UserInfo user = this.users.putIfAbsent(userInfo.getLogin(), newUser);
			if (user == null) {
				user = newUser;
				isNewUser = true;
			}

			//
			// checking and responding.
			//
			if (user.canLogin(userInfo.getLogin(), userInfo.getPassword()))
				return Response.status(isNewUser ? Status.CREATED : Status.ACCEPTED).entity("You may proceed now.")
						.build();
			else
				return Response.status(Status.UNAUTHORIZED).entity("Invalid user or password.").build();

		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response listUsers(Request request) {
		try {
			return Response.status(Status.OK)
					.entity(new WrappedList(this.users.keySet().stream().sorted().collect(Collectors.toList())))
					.build();
		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response sendMessage(Request request, String username, String msg) {
		try {
			UserInfo srcUser = getUser(request);
			if (srcUser == null)
				return Response.status(Status.UNAUTHORIZED).entity("Failed to recognise the user credentials.").build();

			if (username == null || username.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The target user was not specified.").build();

			UserInfo dstUser = this.users.get(username);
			if (dstUser == null)
				return Response.status(Status.BAD_REQUEST)
						.entity(String.format("The user '%s' is unknown to the server.", username)).build();

			if (msg == null || msg.trim().length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The message has to be specified").build();

			if (dstUser.addMessage(new UserInfo.Message(srcUser.getLogin(), msg)))
				return Response.status(Status.CREATED).entity("Your message was created.").build();
			else
				return Response.status(Status.NOT_ACCEPTABLE)
						.entity("Apparently, target user can not accept your message. Maybe, there's too much of them?")
						.build();
		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response receiveMessage(Request request, String username) {
		try {
			UserInfo srcUser = getUser(request);
			if (srcUser == null)
				return Response.status(Status.UNAUTHORIZED).entity("Failed to recognise the user credentials.").build();

			if (username == null || username.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The user was not specified.").build();

			if (!srcUser.getLogin().trim().toLowerCase().equals(username.trim().toLowerCase()))
				return Response.status(Status.FORBIDDEN).entity("You may access only your own messages.").build();

			String[] messages = srcUser.getMessages();

			if (messages == null || messages.length == 0)
				return Response.status(Status.NO_CONTENT).build();

			return Response.status(Status.OK).entity(new WrappedList(messages)).build();

		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response receiveMessage(Request request, String username, String messageId) {
		try {
			UserInfo srcUser = getUser(request);
			if (srcUser == null)
				return Response.status(Status.UNAUTHORIZED).entity("Failed to recognise the user credentials.").build();

			if (messageId == null || messageId.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The message id was not specified.").build();

			if (username == null || username.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The user was not specified.").build();

			if (!srcUser.getLogin().trim().toLowerCase().equals(username.trim().toLowerCase()))
				return Response.status(Status.FORBIDDEN).entity("You may access only your own messages.").build();

			UserInfo.Message msg = srcUser.getMessage(messageId);

			if (msg == null)
				return Response.status(Status.NOT_FOUND)
						.entity("The message you were trying to retrieve was not found.").build();

			return Response.status(Status.OK).entity(new Message(msg.sender, msg.message)).build();

		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response deleteMessage(Request request, String username, String messageId) {
		try {
			UserInfo srcUser = getUser(request);
			if (srcUser == null)
				return Response.status(Status.UNAUTHORIZED).entity("Failed to recognise the user credentials.").build();

			if (messageId == null || messageId.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The message id was not specified.").build();

			if (username == null || username.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The user was not specified.").build();

			if (!srcUser.getLogin().trim().toLowerCase().equals(username.trim().toLowerCase()))
				return Response.status(Status.FORBIDDEN).entity("You may access only your own messages.").build();

			boolean result = srcUser.removeMessage(messageId);
			return Response.status(result ? Status.OK : Status.NOT_FOUND)
					.entity(result ? "Successfully deleted." : "Message with the provided id was not found.").build();
		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response sendFile(Request request, String username, FileInfo file) {
		try {
			UserInfo srcUser = getUser(request);
			if (srcUser == null)
				return Response.status(Status.UNAUTHORIZED).entity("Failed to recognise the user credentials.").build();

			if (username == null || username.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The target user was not specified.").build();

			UserInfo dstUser = this.users.get(username);
			if (dstUser == null)
				return Response.status(Status.BAD_REQUEST)
						.entity(String.format("The user '%s' is unknown to the server.", username)).build();

			if (file == null)
				return Response.status(Status.BAD_REQUEST).entity("The file has to be specified").build();

			if (file.getFilename() == null || file.getFilename().length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The file name has to be specified").build();

			if (file.getContent() == null || file.getContent().length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The file content has to be specified").build();

			file.setSender(srcUser.getLogin());

			if (dstUser.addFile(new UserInfo.FileInfo(file.getSender(), file.getFilename(), file.getContent())))
				return Response.status(Status.CREATED).entity("Your file was created.").build();
			else
				return Response.status(Status.NOT_ACCEPTABLE)
						.entity("Apparently, target user can not accept your file. Maybe, there's too much of them?")
						.build();
		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response receiveFile(Request request, String username) {
		try {
			UserInfo srcUser = getUser(request);
			if (srcUser == null)
				return Response.status(Status.UNAUTHORIZED).entity("Failed to recognise the user credentials.").build();

			if (username == null || username.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The user was not specified.").build();

			if (!srcUser.getLogin().trim().toLowerCase().equals(username.trim().toLowerCase()))
				return Response.status(Status.FORBIDDEN).entity("You may access only your own files.").build();

			String[] files = srcUser.getFiles();

			if (files == null || files.length == 0)
				return Response.status(Status.NO_CONTENT).build();

			return Response.status(Status.OK).entity(new WrappedList(files)).build();

		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response receiveFile(Request request, String username, String fileId) {
		try {
			UserInfo srcUser = getUser(request);
			if (srcUser == null)
				return Response.status(Status.UNAUTHORIZED).entity("Failed to recognise the user credentials.").build();

			if (fileId == null || fileId.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The file id was not specified.").build();

			if (username == null || username.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The user was not specified.").build();

			if (!srcUser.getLogin().trim().toLowerCase().equals(username.trim().toLowerCase()))
				return Response.status(Status.FORBIDDEN).entity("You may access only your own files.").build();

			UserInfo.FileInfo file = srcUser.getFile(fileId);

			if (file == null)
				return Response.status(Status.NOT_FOUND)
						.entity("The file you were trying to retrieve was not found.").build();

			return Response.status(Status.OK).entity(new FileInfo(file.sender, file.filename, file.content)).build();

		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	@Override
	public Response deleteFile(Request request, String username, String fileId) {
		try {
			UserInfo srcUser = getUser(request);
			if (srcUser == null)
				return Response.status(Status.UNAUTHORIZED).entity("Failed to recognise the user credentials.").build();

			if (fileId == null || fileId.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The file id was not specified.").build();

			if (username == null || username.length() == 0)
				return Response.status(Status.BAD_REQUEST).entity("The user was not specified.").build();

			if (!srcUser.getLogin().trim().toLowerCase().equals(username.trim().toLowerCase()))
				return Response.status(Status.FORBIDDEN).entity("You may access only your own files.").build();

			boolean result = srcUser.removeFile(fileId);
			return Response.status(result ? Status.OK : Status.NOT_FOUND)
					.entity(result ? "Successfully deleted." : "File with the provided id was not found.").build();
		} catch (Exception ex) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		}
	}

	private UserInfo getUser(Request request) {
		if (request instanceof HttpRequestContext) {
			HttpRequestContext httpRequest = (HttpRequestContext) request;
			String auth = httpRequest.getHeaderValue("authorization");

			if (auth == null)
				return null;

			auth = auth.replaceFirst("[Bb]asic ", "");
			String userColonPass = Base64.base64Decode(auth);
			String[] credentials = userColonPass.split(":");

			return this.users.get(credentials[0]);
		}
		return null;
	}
}
