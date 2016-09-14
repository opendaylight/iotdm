define(['iotdm-gui.providers.module'], function(app) {
    'use strict';
    app.provider("Path", function() {
        var _base = "/";
        this.setBase = function(base) {
            _base = base;
        };
        this.base = function() {
            return _base;
        };

        this.$get = [function() {
            return _base;
        }];
    });
});
