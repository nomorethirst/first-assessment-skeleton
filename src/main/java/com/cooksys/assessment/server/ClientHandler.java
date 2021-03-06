package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private Dispatcher dispatcher;
	private String username;

	public ClientHandler(Socket socket, Dispatcher dispatcher) {
		super();
		this.socket = socket;
		this.dispatcher = dispatcher;
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				// Process messages from client
				if (reader.ready()) {
					String raw = reader.readLine();
					Message msg = mapper.readValue(raw, Message.class);
					msg.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

					switch (msg.getCommand().charAt(0) == '@' ? "direct" : msg.getCommand()) {
						case "connect":
							log.info("user <{}> connected", msg.getUsername());
							this.username = msg.getUsername();
							dispatcher.connect(msg);
							break;
						case "disconnect":
							log.info("user <{}> disconnected", msg.getUsername());
							this.username = null;
							dispatcher.disconnect(msg);
							this.socket.close();
							break;
						case "echo":
							log.info("user <{}> echoed message <{}>", msg.getUsername(), msg.getContents());
							writer.write(mapper.writeValueAsString(msg));
							writer.flush();
							break;
						case "broadcast":
							log.info("user <{}> broadcasted message <{}>", msg.getUsername(), msg.getContents());
							dispatcher.broadcast(msg);
							break;
						case "direct":
							log.info("user <{}> sent user <{}> message <{}>", msg.getUsername(), msg.getCommand().substring(1), msg.getContents());
							if (dispatcher.isConnected(msg.getCommand().substring(1))) {
								dispatcher.directMessage(msg);
							} else {
								msg.setContents("invalid user");
								writer.write(mapper.writeValueAsString(msg));
								writer.flush();
							}
							break;
						case "users":
							log.info("user <{}> requested user list", msg.getUsername());
							msg.setContents(dispatcher.getCurrentUsers().toString());
							writer.write(mapper.writeValueAsString(msg));
							writer.flush();
							break;
					}
					
				}
				// Process messages to client if connected
				Thread.sleep(500);
				if (this.username != null)
					dispatcher.dispatch(this.username, writer, mapper);
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		} catch (NullPointerException e) {
			e.printStackTrace();
			log.info("user <{}> closed connection.", this.username);
			Message msg = new Message();
			msg.setUsername(this.username);
			dispatcher.disconnect(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				log.error("Error closing socket for user <{}>.", this.username);
				e.printStackTrace();
			}
		}
	}

}
