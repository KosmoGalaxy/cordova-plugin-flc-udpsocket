using FullLegitCode.UdpSocket;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;

// The Blank Page item template is documented at https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace FlcUdpSocketTest
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        public MainPage()
        {
            this.InitializeComponent();
            Test();
        }

        void Test()
        {
            Socket.Create(1);
            Socket.Listen(1, 3060).Progress = (asyncInfo, progressInfo) =>
            {
                try
                {
                    string data = (string) progressInfo[2];
                    Debug.WriteLine(string.Format(
                        "[FlcUdpSocketTest] receive. (thread)={0} (address)={1}:{2} (data)=\"{3}\"",
                        Environment.CurrentManagedThreadId,
                        (string) progressInfo[0],
                        (int) progressInfo[1],
                        data
                    ));
                    if (data == "asd")
                    {
                        Socket.Send(1, "192.168.43.165", 3060, "fgh");
                    }
                }
                catch (Exception e) { Debug.Fail(e.Message); }
            };
            //Socket.Send(1, "192.168.1.142", 3062, "FlcUdpSocket send");
            //await System.Threading.Tasks.Task.Delay(1000);
            //await Socket.Send(1, "192.168.1.142", 3065, "FlcUdpSocket send");
            //await System.Threading.Tasks.Task.Delay(2000);
            //await Socket.Send(1, "192.168.1.142", 3060, "FlcUdpSocket send");
            //await Socket.Broadcast(1, 3060, "FlcUdpSocket broadcast");
            //Socket.Close(1);
            //await Socket.Send(1, "192.168.1.142", 3060, "FlcUdpSocket send");
        }
    }
}
