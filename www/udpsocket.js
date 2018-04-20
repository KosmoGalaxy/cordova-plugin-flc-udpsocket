const exec = require('cordova/exec');
let nextId = 1;

function UdpSocket() {
  this.id = nextId++;
  this.isClosed = false;
}

UdpSocket.setDebug = function(value) {
  exec(
    function() {},
    function() {},
    'FlcUdpSocket',
    'setDebug',
    [value]
  );
};

UdpSocket.receiveFromOwnIp = function(value) {
  exec(
    function() {},
    function() {},
    'FlcUdpSocket',
    'receiveFromOwnIp',
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
        errorCallback(message);
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
        errorCallback(message);
      }
    },
    'FlcUdpSocket',
    'send',
    [this.id, ip, parseInt(port), packet]
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
        errorCallback(message);
      }
    },
    'FlcUdpSocket',
    'broadcast',
    [this.id, parseInt(port), packet]
  );
};

UdpSocket.prototype.receive = function(port, nextCallback, errorCallback) {
  if (this.isClosed) {
    return;
  }
  function receiveCallback(payload) {
    if (nextCallback) {
      nextCallback(payload.ip, payload.port, payload.packet);
    }
  }
  exec(
    function() {},
    function(message) {
      if (errorCallback) {
        errorCallback(message);
      }
    },
    'FlcUdpSocket',
    'receive',
    [this.id, parseInt(port), receiveCallback]
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
        errorCallback(message);
      }
    },
    'FlcUdpSocket',
    'close',
    [this.id]
  );
};

module.exports = UdpSocket;
