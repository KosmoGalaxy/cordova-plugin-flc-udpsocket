package pl.fulllegitcode.udpsocket;

import android.util.SparseArray;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpSocket extends CordovaPlugin {

    private SparseArray<DatagramSocket> _sockets = new SparseArray<DatagramSocket>();

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        DatagramSocket socket = _getSocket(args.getInt(0));
        if (socket != null) {
            if ("send".equals(action)) {
                _send(socket, args.getString(1), args.getInt(2), args.getString(3));
                callbackContext.success();
                return true;
            } else if ("broadcast".equals(action)) {

            }
        }
        callbackContext.error("error");
        return false;
    }

    private DatagramSocket _getSocket(int id) {
        DatagramSocket socket = _sockets.get(id);
        if (socket == null) {
            try {
                socket = new DatagramSocket();
                _sockets.put(id, socket);
            } catch (SocketException e) {}
        }
        return socket;
    }

    private void _send(DatagramSocket socket, String ip, int port, String packetString) {
        try {
            byte[] bytes = packetString.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(ip), port);
            socket.send(packet);
        } catch (Exception e) {}
    }
}
