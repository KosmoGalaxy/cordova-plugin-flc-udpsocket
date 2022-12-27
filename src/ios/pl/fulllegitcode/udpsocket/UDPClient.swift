import Foundation

@_silgen_name("flc_udpsocket_receive") func c_flc_udpsocket_receive(_ fd:Int32,buff:UnsafePointer<UInt8>,len:Int32,ip:UnsafePointer<Int8>,port:UnsafePointer<Int32>) -> Int32

open class UDPClient: UDPSocket {

  var isBound = false

  public override init() {
    super.init()

    let fd = flc_udpsocket_create()
    if fd > 0 {
      self.fd = fd
    }
  }

  open func send(toIp: String, toPort: Int32, packet: String) -> UDPResult {
    /*guard let fd = self.fd else { return .failure(UDPSocketError.connectionClosed) }

    let sendsize = c_flc_udpsocket_sendto(fd, packet, Int32(strlen(packet)), toIp, toPort)
    if sendsize == Int32(strlen(packet)) {
      return .success
    } else {
      let errcode = UDPSocket.getSocketErrorCode(fd);
      if (errcode == 9) {
        print("socket error_code: bad file descriptor");
        self.close();
        return .failure(UDPSocketError.connectionClosed)
      }
      return .failure(UDPSocketError.unknownError)
    }*/
    return .failure(UDPSocketError.unknownError)
  }

  open func sendBytes(toIp: String, toPort: Int32, packet: [UInt8]) -> UDPResult {
      guard let fd = self.fd else { return .failure(UDPSocketError.connectionClosed) }
//       let ipPointer: UnsafePointer<Int8> = UnsafeRawPointer(toIp).assumingMemoryBound(to: Int8.self)
//       let ipPointer = UnsafeMutablePointer<CChar>(mutating: toIp.cString(using: .utf8))
      let pointer: UnsafePointer<Int8> = UnsafeRawPointer(packet).assumingMemoryBound(to: Int8.self)
      let sendsize = flc_udpsocket_sendto(fd, pointer, Int32(packet.count), toIp, toPort)
      if sendsize == Int32(packet.count) {
        return .success
      } else {
        let errcode = UDPSocket.getSocketErrorCode(fd)
        if (errcode == 9) {
          print("socket error_code: bad file descriptor");
          self.close();
          return .failure(UDPSocketError.connectionClosed)
        }
        return .failure(UDPSocketError.unknownError)
      }
    }

  open func broadcast(toPort: Int32, packet: String) -> UDPResult {
    /*guard let fd = self.fd else { return .failure(UDPSocketError.connectionClosed) }

    let sendsize = flc_udpsocket_broadcast(fd, packet, Int32(strlen(packet)), address, toPort)
    if sendsize == Int32(strlen(packet)) {
      return .success
    } else {
      let errcode = UDPSocket.getSocketErrorCode(fd);
      if (errcode == 9) {
        print("socket error_code: bad file descriptor");
        self.close();
        return .failure(UDPSocketError.connectionClosed)
      }
      return .failure(UDPSocketError.unknownError)
    }*/
    return .failure(UDPSocketError.unknownError)
  }

  open func broadcastBytes(toPort: Int32, packet: [UInt8]) -> UDPResult {
      guard let fd = self.fd else { return .failure(UDPSocketError.connectionClosed) }
      let pointer: UnsafePointer<Int8> = UnsafeRawPointer(packet).assumingMemoryBound(to: Int8.self)
      let sendsize = flc_udpsocket_broadcast(fd, pointer, Int32(packet.count), toPort)
      if sendsize == Int32(packet.count) {
        return .success
      } else {
        let errcode = UDPSocket.getSocketErrorCode(fd);
        if (errcode == 9) {
          print("socket error_code: bad file descriptor");
          self.close();
          return .failure(UDPSocketError.connectionClosed)
        }
        return .failure(UDPSocketError.unknownError)
      }
    }

  open func bind(port: Int32) -> UDPResult {
    if isBound {
      return .failure(UDPSocketError.alreadyBound)
    }
    guard let fd = self.fd else { return .failure(UDPSocketError.connectionClosed) }
    if flc_udpsocket_bind(fd, port) == 0 {
      isBound = true
      return .success
    } else {
      return .failure(UDPSocketError.unknownError)
    }
  }

  open func recv(_ expectlen: Int) -> ([UInt8]?, String, Int) {
    if let fd = self.fd {
      var buff: [UInt8] = [UInt8](repeating: 0x0,count: expectlen)
      var remoteipbuff: [Int8] = [Int8](repeating: 0x0,count: 16)
      var remoteport: Int32 = 0
      let readLen: Int32 = c_flc_udpsocket_receive(fd, buff: buff, len: Int32(expectlen), ip: &remoteipbuff, port: &remoteport)
      let port: Int = Int(remoteport)
      var address = ""
      if let ip = String(cString: remoteipbuff, encoding: String.Encoding.utf8) {
        address = ip
      }

      if readLen <= 0 {
        return (nil, address, port)
      }

      let rs = buff[0...Int(readLen-1)]
      let data: [UInt8] = Array(rs)
      return (data, address, port)
    }
    return (nil, "no ip", 0)
  }

  open func close() {
    guard let fd = self.fd else { return }
    _ = flc_udpsocket_close(fd)
    self.fd = nil
  }
}
