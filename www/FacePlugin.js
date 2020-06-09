var exec = require('cordova/exec');

exports.scanFace = function (arg0, success, error) {
    exec(success, error, 'FacePlugin', 'scanFace', [arg0]);
};
