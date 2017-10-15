
var exec = require('cordova/exec');

var PLUGIN_NAME = 'cordovaPluginDfu';

var cordovaPluginDfu = {
  massErase: function(cb) {
    exec(cb, null, PLUGIN_NAME, 'massErase', []);
  },
  writeSerial: function(buf,cb) {
    exec(cb, null, PLUGIN_NAME, 'writeSerial', [{"buf":buf}]);
  },
  readBytes: function(startAddr,bytes,cb) {
    exec(cb, null, PLUGIN_NAME, 'readBytes', [startAddr,bytes]);
  },
  registerReceiver: function(act,cb) {
    exec(cb, null, PLUGIN_NAME, 'registerReceiver', [act]);
  },
  getAuth: function(type,invalidate,cb) {
    exec(cb, null, PLUGIN_NAME, 'getAuth', [type,invalidate]);
  }
};
console.log("cordovaPluginDfu loadad :)")
module.exports = cordovaPluginDfu;
