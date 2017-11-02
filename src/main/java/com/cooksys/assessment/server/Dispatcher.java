package com.cooksys.assessment.server;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class for handling communication between threaded clients ({@link ClientHandler})
 * of a chat server.  Each client represents a connected user, who sends and receives
 * messages ({@link Message}) to the server, sometimes necessitating communication with
 * other connected clients. This class maintains a map {@link outBoxes} of usernames 
 * to queues of messages intended for that user, like an email "outbox", and methods
 * for adding and removing these queues and the messages in them.
 * 
 * @author Danny Smith
 *
 */
public class Dispatcher {
	
	/**
	 * A Map of usernames to queues of messages intended for those users.
	 */
	private Map<String, ConcurrentLinkedQueue<Message>> outBoxes;
			
	/**
	 * Class constructor.
	 */
	public Dispatcher() {
		this.outBoxes = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>>();
	}

	/**
	 * Adds a single {@link Message} to all outBoxes.
	 * @param msg message to be added.
	 */
	public void broadcast(Message msg) {
		for (String user: outBoxes.keySet()) {
			outBoxes.get(user).add(msg);
		}
	}
	
	/**
	 * Sets up an outBox for a newly connected user.
	 * 
	 * @param msg message from user of type (command=) 'connect'.
	 */
	public void connect(Message msg) {
		outBoxes.putIfAbsent(msg.getUsername(), new ConcurrentLinkedQueue<Message>());
		broadcast(msg);
	}
	
	/**
	 * Removes outBox of disconnecting user.
	 * 
	 * @param msg message from user of type (command=) 'disconnect'.
	 */
	public void disconnect(Message msg) {
		outBoxes.remove(msg.getUsername());
		if (msg.getCommand() != null)
			broadcast(msg);
	}
	
	/**
	 * Adds a message from one user to another. Message is added to both
	 * their outBoxes, one to receive, and sender to receive feedback.
	 * 
	 * @param msg message to be added.
	 */
	public void directMessage(Message msg) {
		outBoxes.get(msg.getCommand().substring(1)).add(msg);
		outBoxes.get(msg.getUsername()).add(msg);
	}

	/**
	 * Method used by each client thread to retrieve all their messages from
	 * the outBox and send to client. Utilizes already created PrintWriter and
	 * ObjectMapper connected to their client's socket. and empties their queue
	 * into it.
	 * 
	 * @param username username of connected client.
	 * @param writer   for writing to stream.
	 * @param mapper   marshals Message objects to json for stream.
	 * @throws JsonProcessingException
	 */
	public void dispatch(String username, PrintWriter writer, ObjectMapper mapper) throws JsonProcessingException {
		ConcurrentLinkedQueue<Message> outBox = outBoxes.get(username);
		Message msg;
		while (!outBox.isEmpty()) {
			msg = outBox.remove();
			writer.write(mapper.writeValueAsString(msg));
			writer.flush();
		}
	}
	
	/**
	 * Returns a Set of all currently connected users.
	 * @return Set of all currently connected users.
	 */
	public Set<String> getCurrentUsers() {
		return outBoxes.keySet();
	}
	
	/**
	 * Checks if particular user is connected (in Map).
	 * @param user username to use as key for check.
	 * @return true if username in map, else false.
	 */
	public boolean isConnected(String user) {
		return outBoxes.get(user) != null;
	}

}
