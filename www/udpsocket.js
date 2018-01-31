const exec = require('cordova/exec');
var nextId = 1;

function UdpSocket() {
  this.id = nextId++;
  this.isClosed = false;
}

UdpSocket.setDebug = function(value) {
  exec(
    function() {},
    function(message) {},
    'FlcUdpSocket',
    'setDebug',
    [value]
  );
};

UdpSocket.create = function(successCallback, errorCallback) {
  const socket = new UdpSocket();
  exec(
    function() {
      if (successCallback) {
        successCallback(socket);
      }
    },
    function(message) {
      if (errorCallback) {
        errorCallback(socket, message);
      }
    },
    'FlcUdpSocket',
    'create',
    [socket.id]
  );
};

UdpSocket.prototype.send = function(ip, port, packet, successCallback, errorCallback) {
  if (this.isClosed) {
    return;
  }
  exec(
    function() {
      if (successCallback) {
        successCallback(this);
      }
    },
    function(message) {
      if (errorCallback) {
        errorCallback(this, message);
      }
    },
    'FlcUdpSocket',
    'send',
    [this.id, ip, port, packet]
  );
};

UdpSocket.prototype.broadcast = function(port, packet, successCallback, errorCallback) {
  if (this.isClosed) {
    return;
  }
  exec(
    function() {
      if (successCallback) {
        successCallback(this);
      }
    },
    function(message) {
      if (errorCallback) {
        errorCallback(this, message);
      }
    },
    'FlcUdpSocket',
    'broadcast',
    [this.id, port, packet]
  );
};

UdpSocket.prototype.receive = function(port, nextCallback, errorCallback) {
  if (this.isClosed) {
    return;
  }
  exec(
    function(payload) {
      if (nextCallback) {
        nextCallback(payload.ip, payload.port, payload.packet);
      }
    },
    function(message) {
      if (errorCallback) {
        errorCallback(this, message);
      }
    },
    'FlcUdpSocket',
    'receive',
    [this.id, port]
  );
};

UdpSocket.prototype.close = function(successCallback, errorCallback) {
  if (this.isClosed) {
    return;
  }
  this.isClosed = true;
  exec(
    function() {
      if (successCallback) {
        successCallback(this);
      }
    },
    function(message) {
      if (errorCallback) {
        errorCallback(this, message);
      }
    },
    'FlcUdpSocket',
    'close',
    [this.id]
  );
};

module.exports = UdpSocket;
