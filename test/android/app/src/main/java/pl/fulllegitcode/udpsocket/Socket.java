package pl.fulllegitcode.udpsocket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Locale;

public class Socket extends DatagramSocket {

  public interface ReceiveCallback {

    void next(String ip, int port, String packet);

    void error(String message);

  }


  private int _id;

  public int id() {
    return _id;
  }

  public Socket(SocketAddress bindaddr, int id) throws SocketException {
    super(bindaddr);
    FlcUdpSocketPlugin.log(String.format(Locale.ENGLISH, "create. id=%d", id));
    _id = id;
    _setBroadcast();
    _setReceiveBufferSize();
    _setReuseAddress();
    _setTrafficClass();
  }

  public String send(String ip, int port, String packetString) {
    if (isClosed()) {
      return "socket is closed";
    }
    try {
//      FlcUdpSocketPlugin.logDebug(String.format(Locale.ENGLISH, "send. id=%d address=%s:%d packet=%s", id(), ip, port, packetString.substring(0, 100)));
      byte[] bytes = packetString.getBytes();
      DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(ip), port);
      send(packet);
      return null;
    } catch (Exception e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "send error. id=%d message=%s", id(), e.getMessage()));
      return e.getMessage();
    }
  }

  public String broadcast(int port, String packetString) {
    if (isClosed()) {
      return "socket is closed";
    }
    InetAddress address = null;
    try {
//      FlcUdpSocketPlugin.logDebug(String.format(Locale.ENGLISH, "broadcast. id=%d port=%d packet=%s", id(), port, packetString.substring(0, 100)));
      address = FlcUdpSocketPlugin.getBroadcastAddress();
      byte[] bytes = packetString.getBytes();
      DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
      send(packet);
      return null;
    } catch (Exception e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "broadcast error. id=%d ip=%s port=%d packet=%s message=%s", id(), address, port, packetString, e.getMessage()));
      return e.getMessage();
    }
  }

  public void receive(int port, ReceiveCallback callback) {
    if (isClosed()) {
      callback.error("socket is closed");
      return;
    }
    try {
      if (!isBound()) {
        bind(new InetSocketAddress(port));
      } else {
        FlcUdpSocketPlugin.logDebug(String.format(Locale.ENGLISH, "already bound. id=%d port=%d", id(), getLocalPort()));
      }
      FlcUdpSocketPlugin.logDebug(String.format(Locale.ENGLISH, "receive start. id=%d port=%d", id(), getLocalPort()));
      byte[] bytes = new byte[1024];
      DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
      long startTime = System.currentTimeMillis();
      boolean accept = false;
      while (!isClosed()) {
        receive(packet);
        if (!accept) {
          if (System.currentTimeMillis() - startTime > 1000) {
            accept = true;
          } else {
            continue;
          }
        }
        String inPacketString = new String(packet.getData(), 0, packet.getLength());
        String inIp = packet.getAddress().getHostAddress();
        int inPort = packet.getPort();
        String myIp = FlcUdpSocketPlugin.getOwnIp();
        if (!FlcUdpSocketPlugin.receiveFromOwnIp() && inIp.equals(myIp)) {
          continue;
        }
//        FlcUdpSocketPlugin.logDebug(String.format(Locale.ENGLISH, "receive. id=%d myIp=%s address=%s:%d packet=%s", id(), myIp, inIp, inPort, inPacketString.substring(0, 100)));
        callback.next(inIp, inPort, inPacketString);
      }
    } catch (Exception e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "receive error. id=%d port=%d message=%s", id(), port, e.getMessage()));
      callback.error(e.getMessage());
    }
  }

  private void _setBroadcast() {
    try {
      setBroadcast(true);
      FlcUdpSocketPlugin.log(String.format(Locale.ENGLISH, "setBroadcast. id=%d value=%b", id(), getBroadcast()));
    } catch (SocketException e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "setBroadcast error. id=%d message=%s", id(), e.getMessage()));
    }
  }

  private void _setReceiveBufferSize() {
    try {
      setReceiveBufferSize(getReceiveBufferSize() * 2);
      FlcUdpSocketPlugin.log(String.format(Locale.ENGLISH, "setReceiveBufferSize. id=%d value=%d", id(), getReceiveBufferSize()));
    } catch (SocketException e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "setReceiveBufferSize error. id=%d message=%s", id(), e.getMessage()));
    }
  }

  private void _setReuseAddress() {
    try {
      setReuseAddress(true);
      FlcUdpSocketPlugin.log(String.format(Locale.ENGLISH, "setReuseAddress. id=%d value=%b", id(), getReuseAddress()));
    } catch (SocketException e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "setReuseAddress error. id=%d message=%s", id(), e.getMessage()));
    }
  }

  private void _setTrafficClass() {
    try {
      setTrafficClass(0x04);
      FlcUdpSocketPlugin.log(String.format(Locale.ENGLISH, "setTrafficClass. id=%d value=%d", id(), getTrafficClass()));
    } catch (SocketException e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "setTrafficClass error. id=%d message=%s", id(), e.getMessage()));
    }
  }

}
