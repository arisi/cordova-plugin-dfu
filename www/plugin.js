
var exec = require('cordova/exec');

var PLUGIN_NAME = 'cordovaPluginDfu';

var cordovaPluginDfu = {
  massErase: function(cb) {
    exec(cb, null, PLUGIN_NAME, 'massErase', []);
  },
  registerReceiver: function(act,cb) {
    exec(cb, null, PLUGIN_NAME, 'registerReceiver', [act]);
  }
};
console.log("cordovaPluginDfu loadad :)")
module.exports = cordovaPluginDfu;
