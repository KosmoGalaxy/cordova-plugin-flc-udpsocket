const exec = require('cordova/exec');
var nextId = 1;

function UdpSocket() {
  this.id = nextId++;
  this.isClosed = false;
}

UdpSocket.prototype.send = function(ip, port, packet) {
  if (this.isClosed) {
    return;
  }
  exec(
    function() {},
    function() {},
    'FlcUdpSocket',
    'send',
    [this.id, ip, port, packet]
  );
};

UdpSocket.prototype.broadcast = function(port, packet) {
  if (this.isClosed) {
    return;
  }
  exec(
    function() {},
    function() {},
    'FlcUdpSocket',
    'broadcast',
    [this.id, port, packet]
  );
};

UdpSocket.prototype.receive = function(port, callback) {
  if (this.isClosed) {
    return;
  }
  exec(
    function(payload) {
      callback(payload.packet, payload.ip, payload.port);
    },
    function() {},
    'FlcUdpSocket',
    'receive',
    [this.id, port]
  );
};

UdpSocket.prototype.close = function() {
  if (this.isClosed) {
    return;
  }
  this.isClosed = true;
  exec(
    function() {},
    function() {},
    'FlcUdpSocket',
    'close',
    [this.id]
  );
};

module.exports = UdpSocket;
