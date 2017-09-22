const exec = require('cordova/exec');
var nextId = 1;

function UdpSocket() {
  this.id = nextId++;
}

UdpSocket.prototype.send = function(ip, port, packet) {
  exec(
    function() {},
    function() {},
    'FlcUdpSocket',
    'send',
    [this.id, ip, port, packet]
  );
};

UdpSocket.prototype.broadcast = function(port, packet) {
  exec(
    function() {},
    function() {},
    'FlcUdpSocket',
    'broadcast',
    [this.id, port, packet]
  );
};

UdpSocket.prototype.receive = function(port, callback) {
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

module.exports = UdpSocket;
