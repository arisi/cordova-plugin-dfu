
var exec = require('cordova/exec');

var PLUGIN_NAME = 'cordovaPluginDfu';

var cordovaPluginDfu = {
  echo: function(phrase, cb) {
    exec(cb, null, PLUGIN_NAME, 'echo', [phrase]);
  },
  getDate: function(cb) {
    exec(cb, null, PLUGIN_NAME, 'getDate', []);
  }
};
console.log("plogin loadad")
module.exports = cordovaPluginDfu;
