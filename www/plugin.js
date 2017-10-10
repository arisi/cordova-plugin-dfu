
var exec = require('cordova/exec');

var PLUGIN_NAME = 'cordovaPluginDfu';

var cordovaPluginDfu = {
  echo: function(phrase, cb) {
    exec(cb, null, PLUGIN_NAME, 'echo', [phrase]);
  },
  registerReceiver: function(act,cb) {
    exec(cb, null, PLUGIN_NAME, 'registerReceiver', [act]);
  }
};
console.log("cordovaPluginDfu loadad :)")
module.exports = cordovaPluginDfu;
