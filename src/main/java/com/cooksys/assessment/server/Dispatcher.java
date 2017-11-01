package com.cooksys.assessment.server;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Dispatcher {
	
	private Map<String, ConcurrentLinkedQueue<Message>> outBoxes;
	private ObjectMapper mapper;
			
	public Dispatcher() {
		this.outBoxes = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>>();
		this.mapper = new ObjectMapper();

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
		broadcast(msg);
	}

	public void dispatch(String username, PrintWriter writer) throws JsonProcessingException {
		ConcurrentLinkedQueue<Message> outBox = outBoxes.get(username);
		Message msg;
		while (!outBox.isEmpty()) {
			msg = outBox.remove();
			writer.write(mapper.writeValueAsString(msg));
			writer.flush();
		}
	}

}
