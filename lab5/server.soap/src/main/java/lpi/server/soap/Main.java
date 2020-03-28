package lpi.server.soap;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		System.out.println("Welcome to RST test SOAP WS Server. Press ENTER to shutdown.");

		try (SoapServer server = new SoapServer(args)) {
			server.run();
			System.in.read();
		}
		
		System.out.println("The server was shut down.");
	}
}
