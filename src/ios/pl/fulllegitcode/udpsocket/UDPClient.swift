import Foundation

@_silgen_name("flc_udpsocket_receive") func c_flc_udpsocket_receive(_ fd:Int32,buff:UnsafePointer<Byte>,len:Int32,ip:UnsafePointer<Int8>,port:UnsafePointer<Int32>) -> Int32

open class UDPClient: Socket {
  
  var isBound = false
  
  public override init() {
    super.init()
    
    let fd = flc_udpsocket_create()
    if fd > 0 {
      self.fd = fd
    }
  }
  
  open func send(toIp: String, toPort: Int32, packet: String) -> Result {
    guard let fd = self.fd else { return .failure(SocketError.connectionClosed) }
    
    let sendsize = flc_udpsocket_sendto(fd, packet, Int32(strlen(packet)), toIp, toPort)
    if sendsize == Int32(strlen(packet)) {
      return .success
    } else {
      return .failure(SocketError.unknownError)
    }
  }
  
  open func broadcast(toPort: Int32, packet: String) -> Result {
    guard let fd = self.fd else { return .failure(SocketError.connectionClosed) }
    
    let sendsize = flc_udpsocket_broadcast(fd, packet, Int32(strlen(packet)), address, toPort)
    if sendsize == Int32(strlen(packet)) {
      return .success
    } else {
      return .failure(SocketError.unknownError)
    }
  }
  
  open func bind(port: Int32) -> Result {
    if isBound {
      return .failure(SocketError.alreadyBound)
    }
    guard let fd = self.fd else { return .failure(SocketError.connectionClosed) }
    if flc_udpsocket_bind(fd, port) == 0 {
      isBound = true
      return .success
    } else {
      return .failure(SocketError.unknownError)
    }
  }
  
  open func recv(_ expectlen: Int) -> ([Byte]?, String, Int) {
    if let fd = self.fd {
      var buff: [Byte] = [Byte](repeating: 0x0,count: expectlen)
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
      let data: [Byte] = Array(rs)
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
