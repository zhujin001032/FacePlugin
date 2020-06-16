var exec = require('cordova/exec');

exports.scanFace = function (argx, argy, argw, argh, success, error) {
    exec(success, error, 'FacePlugin', 'scanFace', [argx,argy,argw,argh]);
}
exports.hideCamera = function (success, error) {
    exec(success, error, 'FacePlugin', 'hideCamera');
}
exports.showCamera = function (success, error) {
    exec(success, error, 'FacePlugin', 'showCamera');
};
