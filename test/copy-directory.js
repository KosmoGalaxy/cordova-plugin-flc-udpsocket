const fs = require('fs');
const path = require('path');

module.exports = function copyDirectory(source, destination, files) {
  if (!fs.existsSync(destination)) {
    fs.mkdirSync(destination);
  }
  fs.readdirSync(source).forEach(fileName => {
    const fileSource = path.join(source, fileName);
    const stats = fs.statSync(fileSource);
    if (files && stats.isFile() && files.indexOf(fileName) === -1) {
      return;
    }
    const fileDestination = path.join(destination, fileName);
    if (stats.isDirectory()) {
      copyDirectory(fileSource, fileDestination, files);
    } else {
      fs.copyFileSync(fileSource, fileDestination);
    }
  });
};
