var exec = require('cordova/exec');

exports.scanFace = function (argx, argy, argw, argh, success, error) {
    exec(success, error, 'FacePlugin', 'scanFace', [argx,argy,argw,argh]);
};
