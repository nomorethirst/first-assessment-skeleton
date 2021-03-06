package com.cooksys.assessment.model;

/**
 * Main data model for chat server.  Message objects are the units of
 * communication between this chat server and its clients.
 * 
 * @author Danny Smith
 *
 */
public class Message {

	private String timestamp;
	private String username;
	private String command;
	private String contents;

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	@Override
	public String toString() {
		return "Message [timestamp=" + timestamp + ", username=" + username + ", command=" + command + ", contents="
				+ contents + "]";
	}
	
}
