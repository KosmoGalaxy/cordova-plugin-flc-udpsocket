//
//  ViewController.swift
//  cordova-plugin-flc-udpsocket-iostest
//
//  Created by wojcieszki on 06/04/2018.
//  Copyright © 2018 wojcieszki. All rights reserved.
//

import UIKit

class ViewController: UIViewController {
  
  // MARK: Outlets
  @IBOutlet weak var serverIpAddressLabel: UILabel!
  @IBOutlet weak var ipAddressTextField: UITextField!
  @IBOutlet weak var portTextField: UITextField!
  @IBOutlet weak var broadcastSwitch: UISwitch!
  @IBOutlet weak var consoleTextView: UITextView!
  
  var flcUdpSocket: FlcUdpSocket = FlcUdpSocket()
  
  override func viewDidLoad() {
    super.viewDidLoad()
    serverIpAddressLabel.text = FlcUdpSocket.getMyIp()!
    ipAddressTextField.text = "192.168.1.144"
    portTextField.text = String(1337)
  }
  
  func startReceiving() {
    DispatchQueue.global().async {
      do {
        try self.flcUdpSocket.receive { ip, port, packet in
          let consoleMessage: String = "Socket receive (from = \(ip):\(port)) msg = \(packet)"
          DispatchQueue.main.async {
            self.printToConsole(text: consoleMessage)
          }
        }
      } catch FlcUdpSocketError.socketClosed {
        print("Socket is closed")
      } catch {
        print("Recieve packet unexpected error: \(error).")
      }
    }
  }
  
  func printToConsole(text: String) {
    consoleTextView.text = self.consoleTextView.text + text + "\n"
    let range: NSRange = NSMakeRange(consoleTextView.text.count, 0)
    consoleTextView.scrollRangeToVisible(range)
  }
  
  // MARK: Actions
  @IBAction func useMyIp(_ sender: UIButton) {
    let myIp: String = FlcUdpSocket.getMyIp()!
    ipAddressTextField.text = myIp
  }
  
  @IBAction func createSocket(_ sender: UIButton) {
    if !flcUdpSocket.closed {
      try! flcUdpSocket.close()
    }
    let serverIp: String = FlcUdpSocket.getMyIp()!
    let port: Int32 = Int32(portTextField.text!)!
    flcUdpSocket.create(port: port)
    let consoleMessage: String = "Socket created server IP = \(serverIp) PORT = \(port)"
    self.printToConsole(text: consoleMessage)
    startReceiving()
  }
  
  @IBAction func sendPacket(_ sender: UIButton) {
    do {
      let clientIp: String = ipAddressTextField.text!
      let broadcast: Bool = broadcastSwitch.isOn
      if broadcast {
        try flcUdpSocket.broadcast(message: "BROADCAST WOLOLO MESssage")
      } else {
        try flcUdpSocket.send(to: clientIp, message: "WOLOLO TEST MESSAGE wolololo pretty nice")
      }
    } catch FlcUdpSocketError.socketClosed {
      print("Socket is closed")
    } catch FlcUdpSocketError.sendFailed {
      print("Send packet failed")
    } catch {
      print("Send packet unexpected error: \(error).")
    }
  }
  
  @IBAction func clearConsole(_ sender: UIButton) {
    consoleTextView.text = ""
  }
}

