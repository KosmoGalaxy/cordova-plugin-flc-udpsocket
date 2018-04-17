const cmd = require('node-cmd');
const copyDirectory = require('./copy-directory');

copyDirectory(
  './android/app/src/main/java/pl/fulllegitcode/udpsocket',
  '../src/android/pl/fulllegitcode/udpsocket',
  ['FlcUdpSocketPlugin.java', 'Socket.java']
);
cmd.get(
  'cd cordova && cordova plugin remove cordova-plugin-flc-udpsocket',
  (err, data, stderr) => {
    console.log(data);
    cmd.get(
      'cd cordova && cordova plugin add cordova-plugin-flc-udpsocket --searchpath=../..',
      (err, data, stderr) => {
        if (err) {
          console.error(err);
          return;
        }
        if (stderr) {
          console.error(stderr);
          return;
        }
        console.log(data);
      }
    );
  }
);
