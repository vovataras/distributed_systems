package lpi.server.mq;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		System.out.println("Welcome to RST test ActiveMQ WS Server. Press ENTER to shutdown.");

		try (ActiveMQServer server = new ActiveMQServer(args)) {
			server.run();
			System.in.read();
		}
		
		System.out.println("The server was shut down.");
	}
}
