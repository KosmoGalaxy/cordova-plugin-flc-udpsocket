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

UdpSocket.prototype.sendBytes = function(ip, port, buffer, successCallback, errorCallback) {
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
    'sendBytes',
    [this.id, ip, parseInt(port), buffer]
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

UdpSocket.prototype.broadcastBytes = function(port, buffer, successCallback, errorCallback) {
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
    'broadcastBytes',
    [this.id, parseInt(port), buffer]
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
    receiveCallback,
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

UdpSocket.prototype.receiveBytes = function(port, nextCallback, errorCallback) {
  if (this.isClosed) {
    return;
  }
  function receiveCallback(payload) {
    if (nextCallback) {
      const dv = new DataView(payload);
      const ipLength = dv.getInt8(0);
      const ip = String.fromCharCode.apply(null, new Uint8Array(payload.slice(1, 1 + ipLength)));
      const port = dv.getInt32(1 + ipLength);
      const buffer = payload.slice(1 + ipLength + 4);
      nextCallback(ip, port, buffer);
    }
  }
  exec(
    receiveCallback,
    function(message) {
      if (errorCallback) {
        errorCallback(message);
      }
    },
    'FlcUdpSocket',
    'receiveBytes',
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
