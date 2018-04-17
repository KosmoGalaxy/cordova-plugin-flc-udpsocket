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
let app = {
  // Application Constructor
  initialize: function() {
    document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
  },

  // deviceready Event Handler
  //
  // Bind any cordova events here. Common events are:
  // 'pause', 'resume', etc.
  onDeviceReady: function() {
    this.receivedEvent('deviceready');
    setTimeout(() => this.test(), 5000);
  },

  // Update DOM on a Received Event
  receivedEvent: function(id) {
    let parentElement = document.getElementById(id);
    let listeningElement = parentElement.querySelector('.listening');
    let receivedElement = parentElement.querySelector('.received');

    listeningElement.setAttribute('style', 'display:none;');
    receivedElement.setAttribute('style', 'display:block;');

    document.getElementById('test').setAttribute('style', 'display:block;');

    console.log('Received Event: ' + id);
  },

  socket: null,

  test: function() {
    this.create();
  },

  create: function() {
    let self = this;
    cordova.plugins.FlcUdpSocket.create(
      socket => {
        self.socket = socket;
        console.log('Socket create success, id: ' + socket.id);

        let sendButton = document.getElementById('button-send');
        sendButton.onclick = function() {
          self.send();
          return false;
        };

        // self.startReceiving();
      },
      e => {
        console.error(e);
      }
    );
  },

  send: function() {
    let self = this;
    if (!self.socket) {
      console.error('Socket not yet created');
    }

    let ip = document.getElementById('input-ip').value;
    let port = parseInt(document.getElementById('input-port').value);
    let msg = document.getElementById('input-msg').value;

    console.log(self.socket.id, ip, port, msg);
    self.socket.send(ip, port, msg, () => {
      console.log('Send success');
    }, e => {
      console.error(e);
    });
  },

  startReceiving: function() {
    let self = this;
    if (!self.socket) {
      console.error('Socket not yet created');
    }

    let consoleTextarea = document.getElementById('console');
    let listenPort = parseInt(document.getElementById('input-port').value);

    self.socket.receive(
      listenPort,
      (ip, port, msg) => {
        let text = consoleTextarea.value;
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