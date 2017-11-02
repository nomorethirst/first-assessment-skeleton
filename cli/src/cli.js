import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let host
let port
let command_state = null

// Animated welcome message
let i = 0, count = 0, frame = 0
const frames = ['-', '\\', '|', '/']
function welcome() {
    cli.ui.redraw(cli.chalk['yellow'](
`
                ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆
                ${frame}  Welcome to FastChat'D! ${frame}
                ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆ ☆
`
    ))
    if (count < 15) {
      frame = frames[i = ++count % frames.length];
      setTimeout( () => {
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

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]', 'Connect to server.')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    host = args.host ? args.host : '127.0.0.1'
    port = args.port ? args.port : 8084
    server = connect({ host, port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      const jsonArray = `[${buffer.toString().split('}{').join('},{').split(',')}]`
      const objArray = JSON.parse(jsonArray)
      for (let o of objArray) {
        //this.log(o)
        let msg = new Message(o)
        if (msg.contents === 'invalid user')
          command_state = null
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
  .action(function (input, callback) {
    const inputArray = input.split(' ')
    const command = inputArray.splice(0,1)[0]
    const contents = inputArray.join(' ')
    //this.log(`contents: "${contents}", command: "${command}"`)

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo' || command === 'broadcast' || command[0] === '@' || command === 'users') {
      command_state = command
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command_state !== null) {
      server.write(new Message({ username, command: command_state, contents: command + ' ' + contents }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })

function command2color(command) {
  switch (command[0] === '@' ? 'direct' : command) {
    case 'connect':
    case 'disconnect':
      return 'magenta'
    case 'echo':
      return 'blue'
    case 'broadcast':
      return 'green'
    case 'direct':
      return 'cyan'
    case 'users':
      return 'yellow'
    default:
      return 'white'
  }
}