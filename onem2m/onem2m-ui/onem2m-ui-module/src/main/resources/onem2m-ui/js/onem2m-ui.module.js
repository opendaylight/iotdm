var modules = [
    'angular',
    'ngMaterial',
    'angular-material-datetimepicker',
    'app/onem2m-ui/js/controllers/index',
    'app/onem2m-ui/js/directives/index',
    'app/onem2m-ui/js/filters/index',
    'app/onem2m-ui/js/services/index'
];

define(modules, function(ng) {
    var app = angular.module('app.onem2m-ui', ['ngMaterial', 'ngMaterialDatePicker', 'app.onem2m-ui.controllers', 'app.onem2m-ui.directives', 'app.onem2m-ui.filters', 'app.onem2m-ui.services']);

    app.config(function($stateProvider,
        $compileProvider,
        $controllerProvider,
        $provide,
        NavHelperProvider,
        $translateProvider) {
        app.register = {
            controller: $controllerProvider.register,
            directive: $compileProvider.directive,
            factory: $provide.factory,
            service: $provide.service
        };

        NavHelperProvider.addToMenu('onem2m-ui', {
            "link": "#/onem2m-ui",
            "active": "main.onem2m-ui",
            "title": "onem2m-ui",
            "icon": "", // Add navigation icon css class here
            "page": {
                "title": "onem2m-ui",
                "description": "onem2m-ui"
            }
        });

        var access = routingConfig.accessLevels;

        $stateProvider.state('main.onem2m-ui', {
            url: 'onem2m-ui',
            access: access.admin,
            views: {
                'content': {
                    templateUrl: 'src/app/onem2m-ui/template/onem2m-ui.tplt.html'
                }
            }
        });

    });
    app.config(
        function($mdThemingProvider) {
            $mdThemingProvider.theme('default')
                .primaryPalette('blue')
                .accentPalette('light-blue');
        }
    );
    app.run(function(NxService) {
        // register icons
        var icons = ["CSEBase", "AE", "container", "contentInstance", "group", "node", "accessControlPolicy", "observe", "unobserve", "subscription"];
        var location = "src/app/onem2m-ui/icon";
        var format = "svg";
        var width = 50;
        var height = 50;
        icons.forEach(function(icon) {
            var file = location + "/" + icon + "." + format;
            NxService.graphic.Icons.registerIcon(icon.toLowerCase(), file, width, height);
        });
        // end register icons
    });
    return app;
});
