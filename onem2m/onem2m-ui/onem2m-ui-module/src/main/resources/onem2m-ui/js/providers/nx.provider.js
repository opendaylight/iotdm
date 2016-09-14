define(['iotdm-gui.providers.module', 'next'], function(app,next) {
    'use strict';
    app.provider('Nx', function() {
        this.icons = function(config) {
            var icons = config.icons;
            var location = config.location;
            var format = config.format;
            var width = config.width;
            var height = config.height;
            if (next) {
                icons.forEach(function(icon) {
                    var file = location + "/" + icon + "." + format;
                    next.graphic.Icons.registerIcon(icon.toLowerCase(), file, width, height);
                });
            }
        };
        this.$get = function() {
            if (next)
                return next;
        };
    });
})
