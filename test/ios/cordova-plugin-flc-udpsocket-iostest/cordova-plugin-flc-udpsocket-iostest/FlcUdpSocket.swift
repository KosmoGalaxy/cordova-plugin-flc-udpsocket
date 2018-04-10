//
//  FlcUdpSocket.swift
//  cordova-plugin-flc-udpsocket-iostest
//
//  Created by wojcieszki on 06/04/2018.
//  Copyright © 2018 wojcieszki. All rights reserved.
//

import Foundation

enum FlcUdpSocketError: Error {
  case socketClosed
  case sendFailed
}

class FlcUdpSocket {
  
  var closed: Bool = true
  var client: UDPClient?
  var serverIp: String?
  
  init() {
    serverIp = FlcUdpSocket.getMyIp()!
  }
  
  func create(port: Int32) {
    client = UDPClient(address: serverIp!, port: port)
    closed = false
    print("Socket created server IP =", serverIp!, "PORT =", port)
  }
  
  func send(to: String, message: String) throws {
    if closed {
      throw FlcUdpSocketError.socketClosed
    }
    switch client!.send(ip: to, packet: message) {
    case .success:
      break
    case .failure(let error):
      print("Send error", error)
      throw FlcUdpSocketError.sendFailed
    }
  }
  
  func broadcast(message: String) throws {
    if closed {
      throw FlcUdpSocketError.socketClosed
    }
    switch client!.broadcast(packet: message) {
    case .success:
      break
    case .failure(let error):
      print("Broadcast error", error)
      throw FlcUdpSocketError.sendFailed
    }
  }
 
  func receive(callback: (_ ip: String, _ port: Int,  _ packet: String) -> ()) throws {
    if closed {
      throw FlcUdpSocketError.socketClosed
    }
    
    while !closed {
      let (data, address, port) = client!.recv(1024)
      if data != nil {
        let dataString: String = String(data: Data(data!), encoding: .utf8)!
        if address != serverIp! {
          callback(address, port, dataString)
        }
      }
    }
  }
  
  func close() throws {
    client?.close()
  }
  
  static func getMyIp() -> String? {
    let ips: [AnyHashable: String] = UtilObjectiveC.getIPAddresses()
    let myIp: String = String(ips["wireless"]!)
    return myIp
  }
  
}
