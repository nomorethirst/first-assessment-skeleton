import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let host
let port
let command_state

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]', 'Connect to server.')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    host = args.host ? args.host : 'localhost'
    port = args.port ? args.port : 8080
    server = connect({ host, port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      const jsonArray = `[${buffer.toString().split('}{').join('},{').split(',')}]`
      const objArray = JSON.parse(jsonArray)
      for (let o of objArray) {
        this.log(new Message(o).toString())
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

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo' || command === 'broadcast' || command[0] === '@') {
      command_state = command
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command_state !== null) {
      server.write(new Message({ username, command: command_state, contents: command + ' ' + contents }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
