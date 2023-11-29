package pl.fulllegitcode.udpsocket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Locale;

public class Socket extends DatagramSocket {

  public interface ReceiveCallback {

    void next(String ip, int port, byte[] bytes);

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

  public String send(String ip, int port, byte[] bytes) {
    if (isClosed()) {
      return "socket is closed";
    }
    try {
      if (FlcUdpSocketPlugin._isDebug)
        FlcUdpSocketPlugin.logDebug(String.format("send. id=%d address=%s:%d", id(), ip, port));
      DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(ip), port);
      send(packet);
      return null;
    } catch (Exception e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "send error. id=%d address=%s:%d message=%s", id(), ip, port, e.getMessage()));
      return e.getMessage();
    }
  }

  public String broadcast(int port, byte[] bytes) {
    if (isClosed()) {
      return "socket is closed";
    }
    try {
      InetAddress address = FlcUdpSocketPlugin.getBroadcastAddress();
      if (FlcUdpSocketPlugin._isDebug)
        FlcUdpSocketPlugin.logDebug(String.format("broadcast. id=%d address=%s:%d", id(), address.getHostAddress(), port));
      DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
      send(packet);
      return null;
    } catch (Exception e) {
      FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "broadcast error. id=%d port=%d message=%s", id(), port, e.getMessage()));
      return null;
    }
  }

  public void receive(int port, ReceiveFormat format, ReceiveCallback callback) {
    if (isClosed()) {
      callback.error("socket is closed");
      return;
    }
    try {
      if (!isBound())
        bind(new InetSocketAddress(port));
      else if (FlcUdpSocketPlugin._isDebug)
        FlcUdpSocketPlugin.logDebug(String.format(Locale.ENGLISH, "already bound. id=%d port=%d", id(), getLocalPort()));
      if (FlcUdpSocketPlugin._isDebug)
        FlcUdpSocketPlugin.logDebug(String.format(Locale.ENGLISH, "receive start. id=%d port=%d", id(), getLocalPort()));
      byte[] buffer = new byte[65507];
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      /*long startTime = System.currentTimeMillis();
      boolean accept = false;*/
      while (!isClosed()) {
        receive(packet);
        /*if (!accept) {
          if (System.currentTimeMillis() - startTime > 1000) {
            accept = true;
          } else {
            continue;
          }
        }*/

        String inIp = packet.getAddress().getHostAddress();

        if (!FlcUdpSocketPlugin.receiveFromOwnIp() && inIp.equals(FlcUdpSocketPlugin.getOwnIp()))
          continue;

        int inPort = packet.getPort();

        if (format == ReceiveFormat.String) {
          String inPacketString = new String(packet.getData(), 0, packet.getLength());
          callback.next(inIp, inPort, inPacketString);
        } else {
          byte[] bytes = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
          callback.next(inIp, inPort, bytes);
        }
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
