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
				String raw = reader.readLine();
				Message msg = mapper.readValue(raw, Message.class);
				msg.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

				switch (msg.getCommand()) {
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
						String response = mapper.writeValueAsString(msg);
						writer.write(response);
						writer.flush();
						break;
				}

				// Process messages to client
				dispatcher.dispatch(this.username, writer);
				
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
