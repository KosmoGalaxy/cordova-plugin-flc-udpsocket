using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text;
using System.Threading.Tasks;
using Windows.Foundation;
using Windows.Networking;
using Windows.Networking.Connectivity;
using Windows.Networking.Sockets;
using Windows.Storage.Streams;
using Windows.System.Threading;

namespace FullLegitCode.UdpSocket
{
    public sealed class Socket
    {
        struct IpAndSubnetMask
        {
            public IPAddress ip;
            public IPAddress subnetMask;
        }


        const string TAG = "[FlcUdpSocket] ";


        static Dictionary<int, Socket> Sockets { get; } = new Dictionary<int, Socket>();

        public static IAsyncAction Broadcast(int id, int port, string packet)
        {
            Socket socket = _GetSocket(id);
            if (socket == null)
            {
                throw new Exception(string.Format("socket id={0} not found", id));
            }
            return socket.BroadcastAsync(port, packet);
        }

        public static void Close(int id)
        {
            if (!_RemoveSocket(id))
            {
                throw new Exception(string.Format("socket id={0} not found", id));
            }
        }

        public static void Create(int id)
        {
            if (!_AddSocket(id))
            {
                throw new Exception(string.Format("socket id={0} already exists", id));
            }
        }

        public static IAsyncActionWithProgress<IList<dynamic>> Listen(int id, int port)
        {
            Socket socket = _GetSocket(id);
            if (socket == null)
            {
                throw new Exception(string.Format("socket id={0} not found", id));
            }
            return socket.ListenAsync(port);
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
            Sockets.Add(id, new Socket(id));
            return true;
        }

        static Socket _GetSocket(int id)
        {
            return _SocketExists(id) ? Sockets[id] : null;
        }

        static bool _RemoveSocket(int id)
        {
            Socket socket = _GetSocket(id);
            if (socket == null)
            {
                return false;
            }
            socket.CloseSync();
            Sockets.Remove(id);
            return true;
        }

        static bool _SocketExists(int id)
        {
            return Sockets.ContainsKey(id);
        }

        #region util

        /**
         * https://stackoverflow.com/questions/25281099/how-to-get-the-local-ip-broadcast-address-dynamically-c-sharp
         */
        static IPAddress _GetBroadcastIp()
        {
            IpAndSubnetMask ipAndSubnetMask = _GetIpAndSubnetMask();
            if (ipAndSubnetMask.ip != null && ipAndSubnetMask.subnetMask != null)
            {
                byte[] broadcastIPBytes = new byte[4];
                byte[] hostBytes = ipAndSubnetMask.ip.GetAddressBytes();
                byte[] maskBytes = ipAndSubnetMask.subnetMask.GetAddressBytes();
                for (int i = 0; i < 4; i++)
                {
                    broadcastIPBytes[i] = (byte)(hostBytes[i] | (byte)~maskBytes[i]);
                }
                IPAddress ip = new IPAddress(broadcastIPBytes);
                //Debug.WriteLine(TAG + string.Format("get broadcast ip (ip)={0}", ip));
                return ip;
            }
            Debug.Fail(TAG + string.Format("get broadcast ip failed"));
            return null;
        }

        static IpAndSubnetMask _GetIpAndSubnetMask()
        {
            IReadOnlyList<HostName> hostNames = NetworkInformation.GetHostNames();
            foreach (HostName hostName in hostNames)
            {
                if (hostName.Type == HostNameType.Ipv4)
                {
                    byte? prefixLength = hostName.IPInformation.PrefixLength;
                    if (prefixLength != null)
                    {
                        string ip = hostName.RawName;
                        int prefixLengthInt = (int)prefixLength;
                        IPAddress subnetMask = _PrefixLengthToSubnetMask(prefixLengthInt);
                        //Debug.WriteLine(TAG + string.Format("get ip and subnet mask (ip)={0} (subnet mask)={1}", ip, subnetMask));
                        return new IpAndSubnetMask
                        {
                            ip = IPAddress.Parse(ip),
                            subnetMask = subnetMask
                        };
                    }
                }
            }
            Debug.Fail(TAG + string.Format("get ip and subnet mask failed"));
            return new IpAndSubnetMask();
        }

        static IPAddress _PrefixLengthToSubnetMask(int prefixLength)
        {
            UInt32 mask = 0xFFFFFFFF << (32 - prefixLength);
            byte[] binary = new byte[4];
            binary[0] = (byte)((mask & 0xFF000000) >> 24);
            binary[1] = (byte)((mask & 0x00FF0000) >> 16);
            binary[2] = (byte)((mask & 0x0000FF00) >> 8);
            binary[3] = (byte)((mask & 0x000000FF) >> 0);
            IPAddress address = new IPAddress(binary);
            return address;
        }

        #endregion


        public int Id { get; private set; }
        //private UdpClient Client { get; } = new UdpClient();
        private DatagramSocket _InternalSocket { get; }
        private Dictionary<string, StreamWriter> _StreamWriters { get; } = new Dictionary<string, StreamWriter>();
        private bool _isClosed;

        Socket(int id)
        {
            Id = id;
            _InternalSocket = new DatagramSocket();
            //Client.EnableBroadcast = true;
            //Client.ExclusiveAddressUse = false;
            //Client.MulticastLoopback = true;
            //Client.Client.IOControl(-1744830452, new byte[] { 0, 0, 0, 0 }, null);
            Debug.WriteLine(TAG + "socket created. id=" + id);
        }

        public IAsyncAction BroadcastAsync(int port, string packet)
        {
            return Task.Run(async () =>
            {
                IPAddress ip = _GetBroadcastIp();
                if (ip == null)
                {
                    throw new Exception("cannot resolve ip");
                }
                StreamWriter writer = await _GetStreamWriter(ip.ToString(), port);
                await writer.WriteAsync(packet);
                await writer.FlushAsync();
                //byte[] bytes = Encoding.UTF8.GetBytes(packet);
                //await Client.SendAsync(bytes, bytes.Length, ip.ToString(), port);
                //Debug.WriteLine(TAG + string.Format("socket broadcast. address={0}:{1} size={2}", ip, port, bytes.Length));
            })
            .AsAsyncAction();
        }

        public void CloseSync()
        {
            try
            {
                _isClosed = true;
                //Client.Client.Shutdown(SocketShutdown.Both);
                //Client.Dispose();
                _InternalSocket.Dispose();
                foreach (KeyValuePair<string, StreamWriter> kvp in _StreamWriters)
                {
                    StreamWriter writer = kvp.Value;
                    writer.BaseStream.Dispose();
                    writer.Dispose();

                }
            }
            catch (Exception e)
            {
                Debug.Fail(e.ToString());
            }
        }

        /*public IAsyncActionWithProgress<IList<dynamic>> ListenAsync(int port)
        {
            return AsyncInfo.Run<IList<dynamic>>((token, progress) =>
            {
                return Task.Run(async () =>
                {
                    Client.Client.Bind(new IPEndPoint(IPAddress.Any, port));
                    Debug.WriteLine(TAG + string.Format("socket listening. port={0}", port));
                    DateTime startTime = DateTime.Now;
                    bool accept = false;
                    while (!token.IsCancellationRequested && !_isClosed)
                    {
                        UdpReceiveResult result = await Client.ReceiveAsync();
                        if (!accept)
                        {
                            if ((DateTime.Now - startTime).TotalMilliseconds > 1000.0)
                            {
                                accept = true;
                            }
                            else
                            {
                                continue;
                            }
                        }
                        List<dynamic> payload = new List<dynamic>
                        {
                            result.RemoteEndPoint.Address.ToString(),
                            result.RemoteEndPoint.Port,
                            Encoding.UTF8.GetString(result.Buffer)
                        };
                        //Debug.WriteLine(TAG + string.Format("socket received. address={0}:{1} size={2}", (string)payload[0], (int)payload[1], ((string)payload[2]).Length));
                        progress.Report(payload);
                    }
                }, token);
            });
        }*/

        public IAsyncActionWithProgress<IList<dynamic>> ListenAsync(int port)
        {
            return AsyncInfo.Run<IList<dynamic>>((token, progress) =>
            {
                return Task.Run(async () =>
                {
                    _InternalSocket.MessageReceived += (sender, args) =>
                    {
                        string data;
                        using (DataReader dataReader = args.GetDataReader())
                        {
                            data = dataReader.ReadString(dataReader.UnconsumedBufferLength);
                        }
                        List<dynamic> payload = new List<dynamic>
                        {
                            args.RemoteAddress.ToString(),
                            int.Parse(args.RemotePort),
                            data
                        };
                        progress.Report(payload);
                    };
                    await _InternalSocket.BindServiceNameAsync(port.ToString());
                    /*await ThreadPool.RunAsync(workItem =>
                    {
                        InternalSocket.MessageReceived += (s, e) =>
                        {
                            
                        };
                    },
                    WorkItemPriority.High,
                    WorkItemOptions.TimeSliced);*/
                },
                token);
            });
        }

        public IAsyncAction SendAsync(string ip, int port, string packet)
        {
            return Task.Run(async () =>
            {
                StreamWriter writer = await _GetStreamWriter(ip, port);
                await writer.WriteAsync(packet);
                await writer.FlushAsync();
                //byte[] bytes = Encoding.UTF8.GetBytes(packet);
                //await Client.SendAsync(bytes, bytes.Length, ip, port);
                //Debug.WriteLine(TAG + string.Format("socket sent. address={0}:{1} size={2}", ip, port, bytes.Length));
            })
            .AsAsyncAction();
        }

        private async Task<StreamWriter> _GetStreamWriter(string ip, int port)
        {
            string key = ip + port;
            if (!_StreamWriters.ContainsKey(key))
            {
                Stream stream = (await _InternalSocket.GetOutputStreamAsync(new HostName(ip), port.ToString())).AsStreamForWrite();
                StreamWriter writer = new StreamWriter(stream);
                _StreamWriters.Add(key, writer);
            }
            return _StreamWriters[key];
        }
    }
}
