package lpi.server.rmi;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		System.out.println("Welcome to RST test TCP Server. Press ENTER to shutdown.");

		try (RmiServer server = new RmiServer(args)) {
			server.run();
			System.in.read();
		}
		
		System.out.println("The server was shut down.");
	}
}
