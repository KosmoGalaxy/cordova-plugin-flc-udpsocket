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
  case bindFailed
  case alreadyBound
}

let BUFFER_SIZE: Int = 16 * 1024;

class FlcUdpSocket {
  
  var closed: Bool = true
  var client: UDPClient?
  var serverIp: String?
  public var receiveFromOwnIp = true
  
  init() {
    serverIp = FlcUdpSocket.getMyIp()!
  }
  
  func create() {
    client = UDPClient()
    closed = false
  }
  
  func send(toIp: String, toPort: Int32, packet: String) throws {
    if closed {
      throw FlcUdpSocketError.socketClosed
    }
    switch client!.send(toIp: toIp, toPort: toPort, packet: packet) {
    case .success:
      break
    case .failure(let error):
      print("Send error", error)
      throw FlcUdpSocketError.sendFailed
    }
  }
  
  func broadcast(toPort: Int32, packet: String) throws {
    if closed {
      throw FlcUdpSocketError.socketClosed
    }
    switch client!.broadcast(toPort: toPort, packet: packet) {
    case .success:
      break
    case .failure(let error):
      print("Broadcast error", error)
      throw FlcUdpSocketError.sendFailed
    }
  }
 
  func receive(port: Int32, callback: (_ ip: String, _ port: Int,  _ packet: String) -> ()) throws {
    if closed {
      throw FlcUdpSocketError.socketClosed
    }
    
    if client!.isBound {
      throw FlcUdpSocketError.alreadyBound
    }
    
    _ = client!.bind(port: port)
    
    while !closed {
      let (data, address, port) = client!.recv(BUFFER_SIZE)
      if data != nil {
        let dataString: String = String(data: Data(data!), encoding: .utf8)!
        if receiveFromOwnIp {
          callback(address, port, dataString)
        } else {
          if address != serverIp! {
            callback(address, port, dataString)
          }
        }
      }
    }
  }
  
  func close() throws {
    if closed {
      throw FlcUdpSocketError.socketClosed
    }
    
    client?.close()
  }
  
  static func getMyIp() -> String? {
    let ips: [AnyHashable: String] = UtilObjectiveC.getIPAddresses()
    let myIp: String = String(ips["wireless"]!)
    return myIp
  }
  
}
