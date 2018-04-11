/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
  initialize: function() {
    document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
  },
  
  onDeviceReady: function() {
    this.receivedEvent('deviceready');
    this.test();
  },
  
  receivedEvent: function(id) {
    var parentElement = document.getElementById(id);
    var listeningElement = parentElement.querySelector('.listening');
    var receivedElement = parentElement.querySelector('.received');
    
    listeningElement.setAttribute('style', 'display:none;');
    receivedElement.setAttribute('style', 'display:block;');
    
    document.getElementById('test').setAttribute('style', 'display:block;');
    
    console.log('Received Event: ' + id);
  },
  
  socket: null,
  
  test: function() {
    var self = this;
    cordova.plugins.FlcUdpSocket.create(
      socket => {
        self.socket = socket;
        console.log('Socket create success');
                                        
        var sendButton = document.getElementById('button-send');
        sendButton.onclick = function() {
          self.send();
          return false;
        }
                                        
        self.startReceiving();
      },
      e => {
        console.error(e);
      }
    );
  },
  
  send: function() {
    var self = this;
    if (!self.socket) {
      console.error('Socket not yet created');
    }
    
    var ip = document.getElementById('input-ip').value;
    var port = parseInt(document.getElementById('input-port').value);
    var msg = document.getElementById('input-msg').value;
    
    self.socket.send(ip, port, msg, () => {
      console.log('Send success');
    }, e => {
      console.error(e);
    });
  },
  
  startReceiving: function() {
    var self = this;
    if (!self.socket) {
      console.error('Socket not yet created');
    }
    
    var consoleTextarea = document.getElementById('console');
    var listenPort = parseInt(document.getElementById('input-port').value);
    
    self.socket.receive(
      listenPort,
      (ip, port, msg) => {
        var text = consoleTextarea.value;
        text += "(" + ip + ":" + port + "): " + msg + "\n";
        consoleTextarea.value = text;
      },
      e => {
        console.error(e);
      }
    );
  }
  
};

app.initialize();
