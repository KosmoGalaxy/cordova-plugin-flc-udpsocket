using System;
using System.Collections.Generic;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;
using Windows.Foundation;

namespace FullLegitCode.UdpSocket
{
    public sealed class Socket
    {
        static Dictionary<int, Socket> sockets { get; } = new Dictionary<int, Socket>();

        public static void Create(int id)
        {
            if (!_AddSocket(id))
            {
                throw new Exception(string.Format("socket id={0} already exists", id));
            }
        }

        public static IAsyncAction Send(int id, string ip, int port, string packet)
        {
            Socket socket = _GetSocket(id);
            if (socket == null)
            {
                throw new Exception(string.Format("socket id={0} not found", id));
            }
            return socket.SendAsync(ip, port, packet);
        }

        static bool _AddSocket(int id)
        {
            if (_SocketExists(id))
            {
                return false;
            }
            sockets.Add(id, new Socket(id));
            return true;
        }

        static Socket _GetSocket(int id)
        {
            return _SocketExists(id) ? sockets[id] : null;
        }

        static bool _SocketExists(int id)
        {
            return sockets.ContainsKey(id);
        }


        public int id { get; private set; }
        UdpClient client { get; } = new UdpClient();

        Socket(int id)
        {
            this.id = id;
        }

        public IAsyncAction SendAsync(string ip, int port, string packet)
        {
            return Task.Run(() =>
            {
                byte[] bytes = Encoding.UTF8.GetBytes(packet);
                client.SendAsync(bytes, bytes.Length, ip, port);
            })
            .AsAsyncAction();
        }
    }
}
