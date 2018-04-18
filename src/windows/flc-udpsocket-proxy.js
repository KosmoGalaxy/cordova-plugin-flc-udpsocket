const component = FullLegitCode.UdpSocket.Socket;

module.exports = {

  create: function(successCallback, errorCallback, id) {
    try {
      component.create(id);
      successCallback();
    } catch (e) { errorCallback(e) }
  },

  send: function(successCallback, errorCallback, id, ip, port, packet) {
    try {
      component.send(id, ip, port, packet).then(successCallback, errorCallback);
    } catch (e) { errorCallback(e) }
  }

};

require('cordova/exec/proxy').add('FlcUdpSocket', module.exports);
