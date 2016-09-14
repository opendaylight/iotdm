define(['iotdm-gui.directives.module'], function(app) {
    'use strict';

    function sidePanelCrud(Path) {
        return {
            restrict: "E",
            scope: {
                operation: "="
            },
            templateUrl:Path+"template/side-panel-crud.tplt.html"
        };
    }

    app.directive("sidePanelCrud", sidePanelCrud);
});
