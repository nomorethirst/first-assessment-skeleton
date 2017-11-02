export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ timestamp, username, command, contents }) {
    this.timestamp = timestamp
    this.username = username
    this.command = command
    this.contents = contents
  }

  toJSON () {
    return JSON.stringify({
      timestamp: this.timestamp,
      username: this.username,
      command: this.command,
      contents: this.contents
    })
  }

  toString () {
    switch (this.command[0] === '@' ? 'direct' : this.command) {
      case 'connect':
        return `${this.timestamp}: <${this.username}> has connected`
      case 'disconnect':
        return `${this.timestamp}: <${this.username}> has disconnected`
      case 'echo':
        return `${this.timestamp} <${this.username}> (echo): ${this.contents}`
      case 'broadcast':
        return `${this.timestamp} <${this.username}> (all): ${this.contents}`
      case 'direct':
        if (this.contents !== "invalid user")
          return `${this.timestamp} <${this.username}> (whisper): ${this.contents}`
        else
          return `${this.timestamp} <${this.command.slice(1)}> is not connected.`
      case 'users':
        return `${this.timestamp}: currently connected users:\n${this.contents.slice(1,-1).split(/, */).join('\n')}\n<${this.username}>`
      default:
        return `Error: Invalid command in Message object.`
    }
  }
}
