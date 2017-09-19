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

module.exports = UdpSocket;
