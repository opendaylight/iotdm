define(['app/onem2m-ui/js/directives/module'], function(app) {
    function sidePanelCrud() {
        return {
            restrict: "E",
            scope: {
                operation: "="
            },
            templateUrl: "src/app/onem2m-ui/template/side-panel-crud.tplt.html"
        };
    }

    app.directive("sidePanelCrud", sidePanelCrud);
});
