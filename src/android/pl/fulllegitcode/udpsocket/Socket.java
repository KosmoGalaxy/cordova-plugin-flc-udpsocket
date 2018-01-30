package pl.fulllegitcode.udpsocket;

import org.apache.cordova.PluginResult;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Locale;

public class Socket extends DatagramSocket {

    public interface ReceiveCallback {

        void next(String ip, int port, String packet);

        void error(String message);

    }



    private int _id;
    public int id() { return _id; }

    public Socket(int id) throws SocketException {
        FlcUdpSocketPlugin.log(String.format(Locale.ENGLISH, "create. id=%d", id));
        _id = id;
        _setBroadcast();
        _setReuseAddress();
        _setTrafficClass();
    }

    public String send(String ip, int port, String packetString) {
        if (isClosed()) {
            return "socket is closed";
        }
        try {
            FlcUdpSocketPlugin.logDebug(String.format(Locale.ENGLISH, "send. id=%d address=%s:%d packet=%s", id(), ip, port, packetString.substring(0, 100)));
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
            bind(new InetSocketAddress(port));
            byte[] bytes = new byte[8 * 1024];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            while (!isClosed()) {
                receive(packet);
                String inPacketString = new String(packet.getData(), 0, packet.getLength());
                String inIp = packet.getAddress().getHostAddress();
                int inPort = packet.getPort();
                FlcUdpSocketPlugin.log(String.format(Locale.ENGLISH, "receive. id=%d address=%s:%d packet=%s", id(), inIp, inPort, inPacketString.substring(0, 100)));
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
            FlcUdpSocketPlugin.log(String.format("setBroadcast: %b", getBroadcast()));
        } catch (SocketException e) {
            FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "setBroadcast error. message=%s", e.getMessage()));
        }
    }

    private void _setReuseAddress() {
        try {
            setReuseAddress(true);
            FlcUdpSocketPlugin.log(String.format("setReuseAddress: %b", getReuseAddress()));
        } catch (SocketException e) {
            FlcUdpSocketPlugin.logError(String.format(Locale.ENGLISH, "setReuseAddress error. message=%s", e.getMessage()));
        }
    }

    private void _setTrafficClass() {
        try {
            setTrafficClass(0x10);
            FlcUdpSocketPlugin.log(String.format(Locale.ENGLISH, "setTrafficClass: %d", getTrafficClass()));
        } catch (SocketException e) {
            FlcUdpSocketPlugin.logError(String.format("setTrafficClass error. message=%s", e.getMessage()));
        }
    }

}
