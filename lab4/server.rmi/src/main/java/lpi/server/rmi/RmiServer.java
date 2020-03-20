package lpi.server.rmi;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class RmiServer implements Runnable, Closeable, IServer {
	private static final long CLEANUP_DELAY_MS = 1000;
	private static final long SESSION_TIME_SEC = 60;

	private int port = 4321;

	private IServer proxy;
	private Registry registry;

	private ConcurrentMap<String, Instant> sessionToLastActionMap = new ConcurrentHashMap<>();
	private ConcurrentMap<String, UserInfo> sessionToUserMap = new ConcurrentHashMap<>();
	private ConcurrentMap<String, UserInfo> nameToUserMap = new ConcurrentHashMap<>();
	private Timer sessionTimer = new Timer("Session Cleanup Timer", true);

	public RmiServer(String[] args) {
		if (args.length > 0) {
			try {
				this.port = Integer.parseInt(args[0]);
			} catch (Exception ex) {
			}
		}
	}

	@Override
	public void close() throws IOException {

		if (this.sessionTimer != null) {
			this.sessionTimer.cancel();
			this.sessionTimer = null;
		}

		if (this.registry != null) {
			try {
				this.registry.unbind(RMI_SERVER_NAME);
			} catch (NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.registry = null;
		}

		if (this.proxy != null) {
			UnicastRemoteObject.unexportObject(this, true);
			this.proxy = null;
		}
	}

	@Override
	public void run() {
		try {
			this.proxy = (IServer) UnicastRemoteObject.exportObject(this, this.port);

			this.registry = LocateRegistry.createRegistry(this.port);
			this.registry.bind(RMI_SERVER_NAME, this.proxy);

			this.sessionTimer.schedule(new SessionCleanupTask(), CLEANUP_DELAY_MS, CLEANUP_DELAY_MS);

			System.out.printf("The RMI server was started successfully on the port %s%n", this.port);

		} catch (AlreadyBoundException | RemoteException e) {
			throw new RuntimeException("Failed to start server", e);
		}
	}

	@Override
	public void ping() {
		return; // simplest implementation possible.
	}

	@Override
	public String echo(String text) {
		return String.format("ECHO: %s", text);
	}

	@Override
	public String login(String login, String password) throws ArgumentException, LoginException, ServerException {
		try {
			if (login == null || login.trim().length() == 0)
				throw new ArgumentException("login", "Login can not be null or empty");

			if (password == null || password.length() == 0)
				throw new ArgumentException("password", "Password can not be null or empty");

			String sessionId = UUID.randomUUID().toString();

			UserInfo user = this.nameToUserMap.get(login);
			if (user == null) {
				UserInfo previousUser = this.nameToUserMap.putIfAbsent(login, user = new UserInfo(login, password));
				if (previousUser != null)
					user = previousUser;
			}

			if (!user.canLogin(login, password)) {
				throw new LoginException("The login and password do not match");
			}

			sessionToLastActionMap.put(sessionId, Instant.now());
			sessionToUserMap.put(sessionId, user);

			System.out.printf("%s: User \"%s\" logged in. There are %s active users.%n", new Date(), login,
					this.sessionToUserMap.size());

			return sessionId;
		} catch (RemoteException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ServerException("Server failed to process your command", ex);
		}
	}

	@Override
	public String[] listUsers(String sessionId) throws ArgumentException, ServerException {
		try {
			ensureSessionValid(sessionId);

			return this.sessionToUserMap.values().stream().map(user -> user.getLogin()).distinct().sorted()
					.toArray(size -> new String[size]);
		} catch (RemoteException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ServerException("Server failed to process your command", ex);
		}
	}

	@Override
	public void sendMessage(String sessionId, Message msg) throws ArgumentException, ServerException {
		try {
			UserInfo user = ensureSessionValid(sessionId);

			if (msg == null)
				throw new ArgumentException("msg", "The message has to be specified");

			if (msg.getReceiver() == null || msg.getReceiver().trim().length() == 0)
				throw new ArgumentException("msg.receiver", "The message receiver has to be specified.");

			UserInfo receiver = this.nameToUserMap.get(msg.getReceiver());
			if (receiver == null)
				throw new ArgumentException("msg.receiver", "There is no such receiver.");

			msg.setSender(user.getLogin());
			if (!receiver.addMessage(msg))
				throw new ArgumentException("msg.receiver",
						"The receiver can not receive your message now. Try sending it later, when he cleans up his message box.");
		} catch (RemoteException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ServerException("Server failed to process your command", ex);
		}
	}

	@Override
	public Message receiveMessage(String sessionId) throws ArgumentException, ServerException {
		try {
			UserInfo user = ensureSessionValid(sessionId);

			return user.popMessage();

		} catch (RemoteException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ServerException("Server failed to process your command", ex);
		}
	}

	@Override
	public void sendFile(String sessionId, FileInfo file) throws ArgumentException, ServerException {
		try {
			UserInfo user = ensureSessionValid(sessionId);

			if (file == null)
				throw new ArgumentException("file", "The file has to be specified");

			if (file.getReceiver() == null || file.getReceiver().trim().length() == 0)
				throw new ArgumentException("file.receiver", "The file receiver has to be specified.");

			file.setSender(user.getLogin());

			UserInfo receiver = this.nameToUserMap.get(file.getReceiver());
			if (receiver == null)
				throw new ArgumentException("file.receiver", "There is no such receiver.");

			if (!receiver.addFile(file))
				throw new ArgumentException("file.receiver",
						"The receiver can not receive your file now. Try sending it later, when he cleans up his message box.");

		} catch (RemoteException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ServerException("Server failed to process your command", ex);
		}
	}

	@Override
	public FileInfo receiveFile(String sessionId) throws ArgumentException, ServerException {
		try {
			UserInfo user = ensureSessionValid(sessionId);

			return user.popFile();
		} catch (RemoteException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ServerException("Server failed to process your command", ex);
		}
	}

	@Override
	public void exit(String sessionId) throws ServerException {
		try {
			if (sessionId == null || sessionId.length() == 0)
				return;
			this.sessionToLastActionMap.remove(sessionId);
			UserInfo user = this.sessionToUserMap.remove(sessionId);

			if (user != null) {
				System.out.printf("%s: User \"%s\" logged out. There are %s active users.%n", new Date(),
						user.getLogin(), this.sessionToUserMap.size());
			}

		} catch (Exception ex) {
			throw new ServerException("Server failed to process your command", ex);
		}
	}

	private UserInfo ensureSessionValid(String sessionId) throws ArgumentException {

		if (sessionId == null || sessionId.length() == 0)
			throw new ArgumentException("sessionId", "The provided session id is not valid");

		UserInfo user = this.sessionToUserMap.get(sessionId);
		if (user == null) {
			throw new ArgumentException("sessionId",
					String.format(
							"The session id is not valid or expired. "
									+ "Ensure you perform any operation with your session at least each %s seconds.",
							SESSION_TIME_SEC));
		}

		// in case the user was removed right in between these actions, let's
		// restore him there.
		if (this.sessionToLastActionMap.put(sessionId, Instant.now()) == null)
			this.sessionToUserMap.putIfAbsent(sessionId, user);

		return user;
	}

	private class SessionCleanupTask extends TimerTask {
		@Override
		public void run() {
			try {
				Instant erasingPoint = Instant.now().minus(SESSION_TIME_SEC, ChronoUnit.SECONDS);

				// removing all session older than erasing point, defined above.
				sessionToLastActionMap.entrySet().stream().filter(entry -> entry.getValue().isBefore(erasingPoint))
						.map(entry -> entry.getKey()).collect(Collectors.toList()).forEach((session -> {
							sessionToLastActionMap.remove(session);
							UserInfo user = sessionToUserMap.remove(session);
							if (user != null) {
								System.out.printf("%s: User's \"%s\" session expired. There are %s active users.%n",
										new Date(), user.getLogin(), sessionToUserMap.size());
							}
						}));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
