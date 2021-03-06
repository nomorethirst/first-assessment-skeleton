import vorpal from 'vorpal'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let host
let port
let commandState = null

// Animated welcome message
let count = 0
let frame = 0
const frames = ['-', '\\', '|', '/']
const welcome = () => {
  cli.ui.redraw(cli.chalk['yellow'](
`
              ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆
              ${frame}  Welcome to FastChat'D! ${frame}
              ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆
`
  ))
  if (count < 15) {
    frame = frames[++count % frames.length]
    setTimeout(() => {
      welcome()
    }, 50)
  } else {
    cli.ui.redraw(cli.chalk['yellow'](
`
              ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆
              ${frame}  Welcome to FastChat'D! ${frame}
              ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆
          Type 'help' to see available commands.
`
  ))
    cli.ui.redraw.done()
  }
}
welcome()

// prompt
cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

// connect mode
const connectDesc = `Available commands are:
   echo [message]         Send message and server echoes back.
   broadcast [message]    Send message to all connected users.
   users                  Retrieve list of all connected users.
   @username [message]    Send direct message to <username>.
   disconnect             Disconnect from server.
After issuing <echo>, <broadcast>, and <@username> commands, the
command is remembered (until another command is entered) and you
only have to type the message each time afterwards.`

cli
  .mode('connect <username> [host] [port]', 'Connect to server.')
  .delimiter(cli.chalk['green']('connected>'))
  .init((args, callback) => {
    username = args.username
    host = args.host ? args.host : 'localhost'
    port = args.port ? args.port : 8080
    server = connect({ host, port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })
    cli.log(cli.chalk['yellow'](`You are now connecting to the server...\n${connectDesc}`))

    server.on('data', (buffer) => {
      // buffer may contain multiple message jsons, so split them and process each
      const jsonArray = `[${buffer.toString().split('}{').join('},{').split(',')}]`
      const objArray = JSON.parse(jsonArray)
      for (let o of objArray) {
        // this.log(o)
        let msg = new Message(o)
        if (msg.contents === 'invalid user') {
          commandState = null
        }
        this.log(cli.chalk[command2color(msg.command)](msg.toString()))
      }
    })

    server.on('error', (error) => {
      if (error.code === 'ECONNREFUSED') {
        this.log(`Connection refused at ${error.address}:${error.port}. Try a different host/port.`)
      } else {
        this.log(error)
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action((input, callback) => {
    const inputArray = input.split(' ')
    const command = inputArray.splice(0, 1)[0]
    let contents = inputArray.join(' ')
    // this.log(`contents: "${contents}", command: "${command}", username: "${username}"`)

    if (command === 'disconnect') {
      commandState = null
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo' || command === 'broadcast' || command[0] === '@') {
      commandState = command
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'users') {
      commandState = null
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (commandState !== null) {
      contents = contents === '' ? command : command + ' ' + contents
      server.write(new Message({ username, command: commandState, contents }).toJSON() + '\n')
    } else {
      this.log(cli.chalk['red'](`Command <${command}> was not recognized.\n${connectDesc}`))
    }

    callback()
  })

// util function for chalking output of different command types
const command2color = (command) => {
  switch (command[0] === '@' ? 'direct' : command) {
    case 'connect':
    case 'disconnect':
      return 'green'
    case 'echo':
      return 'blue'
    case 'broadcast':
      return 'magenta'
    case 'direct':
      return 'cyan'
    case 'users':
      return 'green'
    default:
      return 'white'
  }
}
