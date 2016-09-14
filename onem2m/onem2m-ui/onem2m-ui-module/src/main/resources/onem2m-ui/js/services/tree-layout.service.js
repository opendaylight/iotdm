define(['iotdm-gui.services.module'], function(app) {
    'use strict';
    function TreeLayoutService(Onem2m) {
        if (!d3)
            throw "ThreeLayoutService dependent on d3, which is not exist";

        this.init = init;

        function init(hGap, vGap) {
            var _hGap = hGap ? hGap : 50;
            var _vGap = vGap ? vGap : 50;
            return function(root) {
                var tree = d3.layout.tree();

                var nodes = tree.nodes(root);
                nodes.forEach(function(node) {
                    var y = node.y;
                    node.y = _hGap * node.x;
                    node.x = _vGap * y;
                });
            };
        }
    }
    TreeLayoutService.$inject = ['Onem2mHelperService'];
    app.service('TreeLayoutService', TreeLayoutService);
})
