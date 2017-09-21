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

module.exports = UdpSocket;
