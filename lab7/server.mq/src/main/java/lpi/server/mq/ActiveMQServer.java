package lpi.server.mq;

import java.io.Closeable;
import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.RemoveInfo;
import org.apache.activemq.jaas.GroupPrincipal;
import org.apache.activemq.security.MessageAuthorizationPolicy;
import org.apache.activemq.security.SimpleAuthenticationPlugin;

public class ActiveMQServer implements Closeable, Runnable, MessageAuthorizationPolicy {

	private static final long CLEANUP_INTERVAL_MS = 1000;
	private static final long CLIENT_TIMEOUT_SEC = 120;

	private static final String BROKER_NAME = "chatBroker";

	private static final String ADMIN_LOGIN = "admin";
	private static final String ADMIN_PASSWORD = "p@szw0Rdd";
	private static final String USER_LOGIN = "user";
	private static final String USER_PASSWORD = "userUser123";

	private static final String ADMIN_GROUP = "admins";
	private static final String USERS_GROUP = "users";

	private static final String ADVISORY_QUEUE = "ActiveMQ.Advisory.Connection";
	private static final String PING_QUEUE = "chat.diag.ping";
	private static final String ECHO_QUEUE = "chat.diag.echo";
	private static final String LOGIN_QUEUE = "chat.login";
	private static final String LIST_USERS_QUEUE = "chat.listUsers";
	private static final String SEND_MESSAGE_QUEUE = "chat.sendMessage";
	private static final String MESSAGES_QUEUE = "chat.messages";
	private static final String SEND_FILE_QUEUE = "chat.sendFile";
	private static final String FILES_QUEUE = "chat.files";
	private static final String EXIT_QUEUE = "chat.exit";

	private static final String LOGIN_KEY = "login";
	private static final String PASSWORD_KEY = "password";
	private static final String IS_SUCCESS_KEY = "success";
	private static final String MESSAGE_KEY = "message";
	private static final String IS_NEW_USER_KEY = "isNew";
	private static final String SENDER_KEY = "sender";
	private static final String RECEIVER_KEY = "receiver";

	private String hostname = "localhost";
	private int port = 61616;

	private BrokerService broker;
	private Connection connection;
	private Session session;
	private Timer cleanupTimer;
	private Map<String, UserInfo> clientIdToUserInfo = new ConcurrentHashMap<String, UserInfo>();
	private Map<String, Instant> clientIdToLastActionInfo = new ConcurrentHashMap<String, Instant>();
	private ConcurrentHashMap<String, UserInfo> loginToUserInfo = new ConcurrentHashMap<String, UserInfo>();

	public ActiveMQServer(String[] args) {
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

	@Override
	public void close() {

		if (this.cleanupTimer != null) {
			this.cleanupTimer.cancel();
			this.cleanupTimer = null;
		}

		// closing server-side.
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (JMSException ex) {
				ex.printStackTrace();
			}
		}

		// closing broker.
		if (broker != null && !broker.isStopped() && !broker.isStopping()) {
			try {
				broker.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		close();
		try {

			// allowing classes from the specified packages to be used in object
			// messages.
			System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", "lpi.server.mq");

			this.broker = new BrokerService();

			// configuring broker.
			this.broker.setBrokerName(BROKER_NAME);
			this.broker.setPlugins(new BrokerPlugin[] { configureSecurityPlugin() });
			this.broker.addConnector("tcp://" + this.hostname + ":" + this.port);
			this.broker.setMessageAuthorizationPolicy(this);

			// starting broker.
			this.broker.start();

			startClient();

			// starting cleanup timer that removes clients that do not do
			// anything in a while.
			this.cleanupTimer = new Timer(true);
			this.cleanupTimer.schedule(new UserCleanupTask(), CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS);

		} catch (Exception ex) {
			throw new RuntimeException("Failed to start server", ex);
		}
	}

	private BrokerPlugin configureSecurityPlugin() {
		SimpleAuthenticationPlugin result = new SimpleAuthenticationPlugin();
		result.setAnonymousAccessAllowed(true);

		Map<String, String> userPasswords = new HashMap<>();
		userPasswords.put(ADMIN_LOGIN, ADMIN_PASSWORD);
		userPasswords.put(USER_LOGIN, USER_PASSWORD);

		Map<String, Set<Principal>> userGroups = new HashMap<>();

		Principal usersGroup = new GroupPrincipal(USERS_GROUP);

		// configuring admin.
		Set<Principal> groups = new HashSet<>();
		groups.add(new GroupPrincipal(ADMIN_GROUP));
		groups.add(usersGroup);
		userGroups.put(ADMIN_LOGIN, groups);

		// configuring user.
		groups = new HashSet<>();
		groups.add(usersGroup);
		userGroups.put(USER_LOGIN, groups);

		result.setUserPasswords(userPasswords);
		result.setUserGroups(userGroups);

		return result;
	}

	private void startClient() throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://" + BROKER_NAME);
		connectionFactory.setTrustedPackages(Arrays.asList("lpi.server.mq"));
		this.connection = connectionFactory.createConnection();
		connection.start();
		this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		session.createConsumer(session.createTopic(ADVISORY_QUEUE)).setMessageListener(new ConnectDisconnectListener());

		// PING QUEUE
		session.createConsumer(session.createQueue(PING_QUEUE)).setMessageListener(new PingMessageListener());

		// ECHO QUEUE
		session.createConsumer(session.createQueue(ECHO_QUEUE)).setMessageListener(new EchoMessageListener());

		// LOGIN/REGISTRATION QUEUE
		session.createConsumer(session.createQueue(LOGIN_QUEUE)).setMessageListener(new LoginListener());

		// EXIT QUEUE
		session.createConsumer(session.createQueue(EXIT_QUEUE)).setMessageListener(new ExitListener());

		// LIST USERS QUEUE
		session.createConsumer(session.createQueue(LIST_USERS_QUEUE)).setMessageListener(new ListUsersListener());

		// MESSAGE SENDING QUEUE
		session.createConsumer(session.createQueue(SEND_MESSAGE_QUEUE)).setMessageListener(new SentMessagesListener());

		// FILE SENDING QUEUE
		session.createConsumer(session.createQueue(SEND_FILE_QUEUE)).setMessageListener(new SentFilesListener());
	}

	@Override
	public boolean isAllowedToConsume(ConnectionContext context, org.apache.activemq.command.Message message) {
		String messageType = message.getType();
		String destinationName = message.getDestination().getQualifiedName();

		// checking if this is some system message.
		if (messageType == "Advisory") {
			if (destinationName.equals("topic://ActiveMQ.Advisory.TempQueue")) {
				// this is a request for an empty queue, we allow it. TODO:
				// check if this is always the case.
				return true;
			}
		}

		// we allow all consuming on temporal queues.
		if (destinationName.startsWith("temp-queue://"))
			return true;

		if (destinationName.startsWith("queue://")) {
			destinationName = destinationName.replace("queue://", "");

			// if anyone tries reading from server-side queues, we reject this.
			if (destinationName.equals(PING_QUEUE) || destinationName.equals(ECHO_QUEUE)
					|| destinationName.equals(LOGIN_QUEUE) || destinationName.equals(LIST_USERS_QUEUE)
					|| destinationName.equals(SEND_MESSAGE_QUEUE) || destinationName.equals(SEND_FILE_QUEUE)
					|| destinationName.equals(EXIT_QUEUE))
				return false;

			// if someone tries reading from messages/files queue, we strictly
			// control
			// this.
			if (destinationName.equals(MESSAGES_QUEUE) || destinationName.equals(FILES_QUEUE)) {
				// if this message was sent not by the server, noone is allowed
				// to receive this message. TODO: can we just ignore/remove this
				// message?
				if (!message.getProducerId().getConnectionId().equals(
						((ActiveMQConnection) this.connection).getConnectionInfo().getConnectionId().getValue()))
					return false;

				// if the user that tries to receive is not authorized or is not
				// the one this message is addressed to, we do not allow
				// receiving this message.
				UserInfo user = this.clientIdToUserInfo.get(context.getConnectionId().getValue());
				if (user == null || !user.getLogin().equals(message.getCorrelationId()))
					return false;
				else
					// otherwise it's fine to receive this message.
					return true;
			}
		}

		System.out.println("HERE IT IS!");
		return true;
	}

	private MapMessage createResponseMessage(boolean isSuccess, String message) throws JMSException {
		MapMessage msg = this.session.createMapMessage();

		msg.setBoolean(IS_SUCCESS_KEY, isSuccess);

		if (message != null)
			msg.setString(MESSAGE_KEY, message);

		return msg;
	}

	private String getUserId(Message msg) {
		return ((ActiveMQMessage) msg).getProducerId().getConnectionId();
	}

	private void updateLastActivity(Message msg) {
		updateLastActivity(getUserId(msg));
	}

	private void updateLastActivity(String clientId) {
		if (clientId != null && this.clientIdToUserInfo.containsKey(clientId))
			this.clientIdToLastActionInfo.put(clientId, Instant.now());
	}

	private Message login(String login, String password, String clientId) throws JMSException {
		if (login == null || login.length() == 0 || password == null || password.length() == 0)
			return createResponseMessage(false, "The \"login\" or \"password\" property was not specified");

		if (clientId == null || clientId.length() == 0)
			return createResponseMessage(false, "Internal server error: failed to retrieve connection id.");

		// retrieving (or creating) the user info object associated with this
		// login
		UserInfo user = new UserInfo(login, password);
		boolean isNewUser = true;
		UserInfo oldUser = this.loginToUserInfo.putIfAbsent(login, user);
		if (oldUser != null) {
			user = oldUser;
			isNewUser = false;
		}

		// checking login and password
		if (!user.canLogin(login, password))
			return createResponseMessage(false, "The username or login are not correct.");

		// creating session to login association.
		this.clientIdToUserInfo.put(clientId, user);
		updateLastActivity(clientId);

		System.out.println(String.format("User \"%s\" %s (%s) successfully.", login, clientId,
				isNewUser ? "registered" : "logged in"));

		MapMessage response = createResponseMessage(true,
				isNewUser ? "Registered successfully" : "Logged in successfully.");
		response.setBoolean(IS_NEW_USER_KEY, isNewUser);
		return response;
	}

	private Message listUsers(String clientId) throws JMSException {
		if (!this.clientIdToUserInfo.containsKey(clientId))
			return createResponseMessage(false, "This operation requires to login. Please, login first.");

		return this.session.createObjectMessage(this.clientIdToUserInfo.values().stream().map(user -> user.getLogin())
				.distinct().toArray(size -> new String[size]));
	}

	private Message processMessage(MapMessage message) throws JMSException {
		String clientId = getUserId(message);

		UserInfo user = clientId != null ? this.clientIdToUserInfo.get(clientId) : null;
		String receiver = message.getString(RECEIVER_KEY);
		String text = message.getString(MESSAGE_KEY);

		if (user == null)
			return createResponseMessage(false, "Failed to process the message: Please, login first.");

		if (receiver == null || receiver.length() == 0 || !this.loginToUserInfo.containsKey(receiver))
			return createResponseMessage(false,
					"Failed to process the message: The receiver \"" + receiver + "\" is not known.");

		MessageProducer producer = this.session.createProducer(this.session.createQueue(MESSAGES_QUEUE));
		try {
			MapMessage response = this.session.createMapMessage();
			response.setString(SENDER_KEY, user.getLogin());
			response.setString(RECEIVER_KEY, receiver);
			response.setString(MESSAGE_KEY, text);
			response.setJMSCorrelationID(receiver);

			producer.send(response);
			return createResponseMessage(true, "The message was sent successfully.");
		} finally {
			producer.close();
		}
	}

	private Message processFile(ObjectMessage message) throws JMSException {

		// retrieving sender info
		String clientId = getUserId(message);
		UserInfo user = clientId != null ? this.clientIdToUserInfo.get(clientId) : null;

		if (user == null)
			return createResponseMessage(false, "Failed to process the file: Please, login first.");

		// retrieving the message
		Serializable fileInfoSerializable = message.getObject();
		if (!(fileInfoSerializable instanceof FileInfo))
			return createResponseMessage(false,
					"Failed to process the file: provided object is not an instance of lpi.server.mq.FileInfo");

		FileInfo fileInfo = (FileInfo) fileInfoSerializable;

		// checking receiver.
		if (fileInfo.getReceiver() == null || !this.loginToUserInfo.containsKey(fileInfo.getReceiver()))
			return createResponseMessage(false,
					"Failed to process the file: The receiver \"" + fileInfo.getReceiver() + "\" is not known.");

		// sending the message.
		fileInfo.setSender(user.getLogin());
		MessageProducer producer = this.session.createProducer(this.session.createQueue(FILES_QUEUE));
		try {
			ObjectMessage msg = this.session.createObjectMessage(fileInfo);
			msg.setJMSCorrelationID(fileInfo.getReceiver());
			producer.send(msg);
			return createResponseMessage(true, "The file was sent successfully.");
		} finally {
			producer.close();
		}
	}

	private Message processExit(Message message) throws JMSException {
		// retrieving sender info
		String clientId = getUserId(message);
		this.clientIdToLastActionInfo.remove(clientId);
		UserInfo user = this.clientIdToUserInfo.remove(clientId);

		if (user != null)
			System.out.printf("%s:\tClient %s (%s) exited gracefully.%n", Instant.now(), user.getLogin(), clientId);
		else
			System.out.printf("%s:\tClient %s exited gracefully.%n", Instant.now(), clientId);

		return createResponseMessage(true, "Exited successfully.");
	}

	private class ConnectDisconnectListener implements MessageListener {
		@Override
		public void onMessage(Message message) {
			if (!(message instanceof ActiveMQMessage))
				return; // this is some wrong message.

			DataStructure dataStructure = ((ActiveMQMessage) message).getDataStructure();

			if (dataStructure instanceof ConnectionInfo) {
				// someone connected.
				ConnectionInfo connection = (ConnectionInfo) dataStructure;

				if (connection.getClientIp().startsWith("vm://"))
					return; // this is a local(in-process) connection.

				System.out.printf("%s:\tClient %s from %s connected.%n", Instant.now(),
						connection.getConnectionId().getValue(), connection.getClientIp());
			} else if (dataStructure instanceof RemoveInfo) {
				// someone disconnected.
				String clientId = ((RemoveInfo) dataStructure).getObjectId().toString();

				// cleaning up the structures.
				clientIdToLastActionInfo.remove(clientId);
				UserInfo user = clientIdToUserInfo.remove(clientId);

				if (user != null)
					System.out.printf("%s:\tClient %s (%s) disconnected.%n", Instant.now(), user.getLogin(), clientId);
				else
					System.out.printf("%s:\tClient %s disconnected.%n", Instant.now(), clientId);
			}
		}
	}

	private abstract class AbstractMessageListener<T extends Message> implements MessageListener {

		private final Class<T> msgClass;

		public AbstractMessageListener(Class<T> msgClass) {
			this.msgClass = msgClass;
		}

		@SuppressWarnings("unchecked")
		@Override
		public final void onMessage(Message message) {
			try {
				MessageProducer producer = null;
				try {
					updateLastActivity(message);

					if (message.getJMSReplyTo() == null)
						return;

					producer = session.createProducer(message.getJMSReplyTo());

					if (msgClass.isAssignableFrom(message.getClass())) {
						producer.send(process((T) message));
					} else {
						producer.send(session
								.createTextMessage("ERROR: unexpected type of message: " + message.getClass().getName()));
					}

				} finally {
					if (producer != null)
						producer.close();
				}
			} catch (JMSException ex) {
				ex.printStackTrace();
			}
		}

		protected abstract Message process(T message) throws JMSException;
	}

	private class PingMessageListener extends AbstractMessageListener<Message> {

		public PingMessageListener() {
			super(Message.class);
		}

		@Override
		protected Message process(Message message) throws JMSException {
			return session.createMessage();
		}
	}

	private class EchoMessageListener extends AbstractMessageListener<TextMessage> {

		public EchoMessageListener() {
			super(TextMessage.class);
		}

		@Override
		protected Message process(TextMessage message) throws JMSException {
			return session.createTextMessage("ECHO: " + ((TextMessage) message).getText());
		}

	}

	private class LoginListener extends AbstractMessageListener<MapMessage> {

		public LoginListener() {
			super(MapMessage.class);
		}

		@Override
		protected Message process(MapMessage message) throws JMSException {
			return login(message.getString(LOGIN_KEY), message.getString(PASSWORD_KEY), getUserId(message));
		}
	}

	private class ListUsersListener extends AbstractMessageListener<Message> {

		public ListUsersListener() {
			super(Message.class);
		}

		@Override
		protected Message process(Message message) throws JMSException {
			return listUsers(getUserId(message));
		}
	}

	private class SentMessagesListener extends AbstractMessageListener<MapMessage> {
		public SentMessagesListener() {
			super(MapMessage.class);
		}

		@Override
		protected Message process(MapMessage message) throws JMSException {
			return processMessage(message);
		}
	}

	private class SentFilesListener extends AbstractMessageListener<ObjectMessage> {

		public SentFilesListener() {
			super(ObjectMessage.class);
		}

		@Override
		protected Message process(ObjectMessage message) throws JMSException {
			return processFile(message);
		}
	}

	private class ExitListener extends AbstractMessageListener<Message> {

		public ExitListener() {
			super(Message.class);
		}

		@Override
		protected Message process(Message message) throws JMSException {
			return processExit(message);
		}
	}

	private class UserCleanupTask extends TimerTask {

		@Override
		public void run() {

			if (clientIdToLastActionInfo.size() == 0)
				return;

			Instant expirationDate = Instant.now().minusSeconds(CLIENT_TIMEOUT_SEC);

			String[] connectionsToRemove = clientIdToLastActionInfo.entrySet().stream()
					.filter(e -> e.getValue().compareTo(expirationDate) < 0).map(e -> e.getKey())
					.toArray(size -> new String[size]);

			for (String id : connectionsToRemove) {
				clientIdToLastActionInfo.remove(id);
				UserInfo user = clientIdToUserInfo.remove(id);
				if (user != null) {
					System.out.printf("User %s (%s) connection exceeded %s sec timeout.%n", user.getLogin(), id,
							CLIENT_TIMEOUT_SEC);
				}
			}
		}
	}
}
