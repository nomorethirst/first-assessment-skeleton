package com.cooksys.assessment.server;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Dispatcher {
	
	private Map<String, ConcurrentLinkedQueue<Message>> outBoxes;
			
	public Dispatcher() {
		this.outBoxes = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>>();
	}

	public void broadcast(Message msg) {
		for (String user: outBoxes.keySet()) {
			outBoxes.get(user).add(msg);
		}
	}
	
	public void connect(Message msg) {
		outBoxes.putIfAbsent(msg.getUsername(), new ConcurrentLinkedQueue<Message>());
		broadcast(msg);
	}
	
	public void disconnect(Message msg) {
		outBoxes.remove(msg.getUsername());
		if (msg.getCommand() != null)
			broadcast(msg);
	}
	
	public void directMessage(Message msg) {
		outBoxes.get(msg.getCommand().substring(1)).add(msg);
		outBoxes.get(msg.getUsername()).add(msg);
	}

	public void dispatch(String username, PrintWriter writer, ObjectMapper mapper) throws JsonProcessingException {
		ConcurrentLinkedQueue<Message> outBox = outBoxes.get(username);
		Message msg;
		while (outBox != null && !outBox.isEmpty()) {
			msg = outBox.remove();
			writer.write(mapper.writeValueAsString(msg));
			writer.flush();
		}
	}
	
	public Set<String> getCurrentUsers() {
		return outBoxes.keySet();
	}
	
	public boolean isConnected(String user) {
		return outBoxes.get(user) != null;
	}

}
