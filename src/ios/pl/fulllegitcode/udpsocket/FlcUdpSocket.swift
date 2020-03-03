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

let UDP_BUFFER_SIZE: Int = 16 * 65507;

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
      if (error as! UDPSocketError) == UDPSocketError.connectionClosed {
        throw FlcUdpSocketError.socketClosed
      }
      print("Send error", error)
      throw FlcUdpSocketError.sendFailed
    }
  }

  func sendBytes(toIp: String, toPort: Int32, packet: [UInt8]) throws {
  	if closed {
  	  throw FlcUdpSocketError.socketClosed
  	}
  	switch client!.sendBytes(toIp: toIp, toPort: toPort, packet: packet) {
  	case .success:
  	  break
  	case .failure(let error):
  	  if (error as! UDPSocketError) == UDPSocketError.connectionClosed {
  		throw FlcUdpSocketError.socketClosed
  	  }
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
      if (error as! UDPSocketError) == UDPSocketError.connectionClosed {
        throw FlcUdpSocketError.socketClosed
      }
      print("Broadcast error", error)
      throw FlcUdpSocketError.sendFailed
    }
  }

  func broadcastBytes(toPort: Int32, packet: [UInt8]) throws {
      if closed {
        throw FlcUdpSocketError.socketClosed
      }
      switch client!.broadcastBytes(toPort: toPort, packet: packet) {
      case .success:
        break
      case .failure(let error):
        if (error as! UDPSocketError) == UDPSocketError.connectionClosed {
        throw FlcUdpSocketError.socketClosed
        }
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
      let (data, address, port) = client!.recv(UDP_BUFFER_SIZE)
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

  func receiveBytes(port: Int32, callback: (_ ip: String, _ port: Int,  _ packet: [UInt8]) -> ()) throws {
   if closed {
     throw FlcUdpSocketError.socketClosed
   }

   if client!.isBound {
     throw FlcUdpSocketError.alreadyBound
   }

   _ = client!.bind(port: port)

   while !closed {
     let (data, address, port) = client!.recv(UDP_BUFFER_SIZE)
     if data != nil {
  	 if receiveFromOwnIp {
  	   callback(address, port, data!)
  	 } else {
  	   if address != serverIp! {
  		 callback(address, port, data!)
  	   }
  	 }
     }
   }
  }
  
  func close() throws {
    if closed {
      throw FlcUdpSocketError.socketClosed
    }
    closed = true
    client?.close()
  }
  
  static func getMyIp() -> String? {
    let ips: [AnyHashable: String] = FlcUdpUtilObjectiveC.getIPAddresses()
    var myIp: String
    if String(ips["hotspot"]!) != "" {
      myIp = String(ips["hotspot"]!)
    } else {
      myIp = String(ips["wireless"]!)
    }
    return myIp
  }
  
}
