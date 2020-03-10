package server.tcp;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Logger;

public class ConnectionHandler implements Closeable, Runnable {

	private static final Object CONNECTION_RESET = "Connection reset";
	private Logger log = Logger.getLogger(this.getClass().getName());
	private Socket socket;
	private ProtocolManager protocolManager;
	private volatile boolean isClosing = false;
	private Thread thread = null;

	public ConnectionHandler(Socket socket, ProtocolManager protocol) {
		this.socket = socket;
		this.protocolManager = protocol;
		this.thread = new Thread(this, "Connection Handler " + new Date());
		this.thread.start();
	}

	@Override
	public void close() {
		close(false);
	}

	private void close(boolean selfClose) {
		this.isClosing = true;

		if (this.socket != null) {
			try {
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.socket = null;
		}

		if (!selfClose) {
			try {
				this.thread.join();
			} catch (InterruptedException e) {
				// We were interrupted, no need to wait.
			}
		} else {
			this.protocolManager.clientDisconnected(this);
		}
	}

	@Override
	public void run() {
		try {
			DataInputStream readStream = new DataInputStream(this.socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(this.socket.getOutputStream());

			while (!this.isClosing) {
				try {
					//
					// Reading Command.
					//
					int contentSize = readStream.readInt();
					byte[] response = null;
					if (this.protocolManager.isContentSizeOk(contentSize)) {
						byte[] message = new byte[contentSize];
						readStream.readFully(message);
						response = this.protocolManager.execute(this, message);
					} else {
						response = this.protocolManager.incorrectContentSize();
					}

					//
					// Sending Response.
					//
					if (response == null) {
						response = this.protocolManager.serverError();
					}
					outputStream.writeInt(response.length);
					outputStream.write(response);

				} catch (EOFException ex) {
					this.close(true);
					break;
				} catch (IOException ex) {
					if (ex.getMessage().equals(CONNECTION_RESET)) {
						this.close(true);
						break;
					}
					log.info(ex.toString());
				}
			}
		} catch (IOException ex) {
			log.warning("Failed to bind to a socket:" + ex.getMessage());
		}
	}
}
