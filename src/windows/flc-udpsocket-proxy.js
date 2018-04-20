const component = FullLegitCode.UdpSocket.Socket;

function broadcast(successCallback, errorCallback, args) {
  try {
    const id = args[0], port = args[1], packet = args[2];
    component.broadcast(id, port, packet).then(successCallback, errorCallback);
  } catch (e) { errorCallback(e) }
}

function create(successCallback, errorCallback, args) {
  try {
    const id = args[0];
    component.create(id);
    successCallback();
  } catch (e) { errorCallback(e) }
}

function listen(successCallback, errorCallback, args) {
  try {
    const id = args[0], port = args[1], receiveCallback = args[2];
    component.listen(id, port)
    .then(
      () => {},
      errorCallback,
      payload => {
        if (payload.length !== 3) {
          console.error('[FlcUdpSocket.receive] payload invalid (payload)=', payload);
          return;
        }
        receiveCallback({ip: payload[0], port: payload[1], packet: payload[2]});
      }
    );
  } catch (e) { errorCallback(e) }
}

function send(successCallback, errorCallback, args) {
  try {
    const id = args[0], ip = args[1], port = args[2], packet = args[3];
    component.send(id, ip, port, packet).then(successCallback, errorCallback);
  } catch (e) { errorCallback(e) }
}

function close(successCallback, errorCallback, args) {
  try {
    const id = args[0];
    component.close(id).then(successCallback, errorCallback);
  } catch (e) { errorCallback(e) }
}


module.exports = {
  broadcast: broadcast,
  close: close,
  create: create,
  listen: listen,
  send: send,

  /**
   * backwards compatibility alias for listen
   */
  receive: listen,

  /**
   * placeholders
   */
  setDebug: function(successCallback) { successCallback() },
  receiveFromOwnIp: function(successCallback) { successCallback() }
};

require('cordova/exec/proxy').add('FlcUdpSocket', module.exports);
