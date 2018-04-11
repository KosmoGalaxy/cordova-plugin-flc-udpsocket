//
//  FlcUdpSocketPlugin.swift
//  HelloCordova
//
//  Created by wojcieszki on 11/04/2018.
//

import Foundation

@objc(FlcUdpSocketPlugin) class FlcUdpSocketPlugin : CDVPlugin {
  
  var isDebug: Bool = false
  var sockets: [Int: FlcUdpSocket]
  
  override init() {
    sockets = [Int: FlcUdpSocket]()
    super.init()
  }
  
  override func pluginInitialize() {
    sockets = [Int: FlcUdpSocket]()
    super.pluginInitialize()
  }
  
  @objc(setDebug:) func setDebug(command: CDVInvokedUrlCommand) {
    isDebug = command.argument(at: 0) as? Bool ?? false
    self.commandDelegate!.run(inBackground: {
      let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
      self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    })
  }
  
  @objc(receiveFromOwnIp:) func receiveFromOwnIp(command: CDVInvokedUrlCommand) {
    let receiveFromOwnIp: Bool = command.argument(at: 0) as? Bool ?? false
    self.commandDelegate!.run(inBackground: {
      for socket in self.sockets.values {
        socket.receiveFromOwnIp = receiveFromOwnIp
      }
      let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
      self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    })
  }
  
  @objc(create:) func create(command: CDVInvokedUrlCommand) {
    let socketId: Int = command.argument(at: 0) as! Int
    self.commandDelegate!.run(inBackground: {
      var pluginResult: CDVPluginResult
      if self.sockets[socketId] != nil {
        pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Socket with socketId = \(socketId) already exists")
      } else {
        let socket = FlcUdpSocket()
        socket.create()
        self.sockets[socketId] = socket
        pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
      }
      self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    })
  }
  
  @objc(send:) func send(command: CDVInvokedUrlCommand) {
    let socketId: Int = command.argument(at: 0) as! Int
    let ip: String = command.argument(at: 1) as! String
    let port: Int = command.argument(at: 2) as! Int
    let packet: String = command.argument(at: 3) as! String
    self.commandDelegate!.run(inBackground: {
      var pluginResult: CDVPluginResult
      let socket = self.sockets[socketId]
      if socket != nil {
        do {
          try socket!.send(toIp: ip, toPort: Int32(port), packet: packet)
          pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        } catch FlcUdpSocketError.socketClosed {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Send failed. Socket closed.")
        } catch FlcUdpSocketError.sendFailed {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Send failed.")
        } catch {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Send failed. Unknown error.")
        }
      } else {
        pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Invalid socketId")
      }
      self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    })
  }
  
  @objc(broadcast:) func broadcast(command: CDVInvokedUrlCommand) {
    let socketId: Int = command.argument(at: 0) as! Int
    let port: Int = command.argument(at: 1) as! Int
    let packet: String = command.argument(at: 2) as! String
    self.commandDelegate!.run(inBackground: {
      var pluginResult: CDVPluginResult
      let socket = self.sockets[socketId]
      if socket != nil {
        do {
          try socket!.broadcast(toPort: Int32(port), packet: packet)
          pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        } catch FlcUdpSocketError.socketClosed {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Broadcast failed. Socket closed.")
        } catch FlcUdpSocketError.sendFailed {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Broadcast failed.")
        } catch {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Broadcast failed. Unknown error.")
        }
      } else {
        pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Invalid socketId")
      }
      self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    })
  }
  
  @objc(receive:) func receive(command: CDVInvokedUrlCommand) {
    let socketId: Int = command.argument(at: 0) as! Int
    let port: Int = command.argument(at: 1) as! Int
    self.commandDelegate!.run(inBackground: {
      var pluginResult: CDVPluginResult
      let socket = self.sockets[socketId]
      if socket != nil {
        do {
          try socket!.receive(port: Int32(port)) { ip, port, packet in
            let payload: [String: String] = ["ip": ip, "port": String(port), "packet": packet]
            let pluginResultReceive = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: payload)
            pluginResultReceive!.setKeepCallbackAs(true)
            self.commandDelegate.send(pluginResultReceive!, callbackId: command.callbackId)
          }
        } catch FlcUdpSocketError.socketClosed {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Receive failed. Socket closed.")
        } catch FlcUdpSocketError.alreadyBound {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Receive failed. Socket already bound.")
        } catch FlcUdpSocketError.bindFailed {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Receive failed. Socket bind failed.")
        } catch {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Receive failed. Unknown error.")
        }
      } else {
        pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Invalid socketId")
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
      }
    })
  }
  
  @objc(close:) func close(command: CDVInvokedUrlCommand) {
    let socketId: Int = command.argument(at: 0) as! Int
    self.commandDelegate!.run(inBackground: {
      var pluginResult: CDVPluginResult
      let socket = self.sockets[socketId]
      if socket != nil {
        do {
          try socket!.close()
          pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        } catch FlcUdpSocketError.socketClosed {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Close failed. Socket already closed.")
        } catch {
          pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Close failed. Unknown error.")
        }
      } else {
        pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Invalid socketId")
      }
      self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    })
  }
  
}
