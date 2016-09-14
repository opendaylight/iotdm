'use strict';
var modules = [
    'module',
    'angular',
    //'ngUIRoute',
    'ngAnimate',
    'ngAria',
    'ngMessages',
    'ngMaterial',
    'moment',
    'material-datetimepicker',
    'd3',
    'next',

    'iotdm-gui.controller',
    'side-panel-crud.controller',
    'side-panel-info.controller',
    'side-panel-retrieve-cse.controller',

    'onem2m-input-component-custom.directive',
    'onem2m-input.directive',
    'onem2m-slider.directive',

    'short-to-long.filter',

    'nx.provider',
    'path.provider',

    'alert.service',
    'datastore-onem2m-data-adaptor.service',
    'datastore.service',
    'onem2m-crud.service',
    'onem2m-description.service',
    'onem2m-helper.service',
    'onem2m-input-component.service',
    'topology-helper.service',
    'topology.service',
    'tree-layout.service'
];



define(modules, function(module, ng) {
    var baseUrl = module.config().baseUrl;
    var angular_modules = [
        'ngMaterialDatePicker',
        'ngMaterial',
        'ui.router',
        'app.iotdm-gui.controllers',
        'app.iotdm-gui.providers',
        'app.iotdm-gui.directives',
        'app.iotdm-gui.filters',
        'app.iotdm-gui.services'
    ];

    var app = ng.module('app.iotdm-gui', angular_modules);

    app.config(
        function( $compileProvider,
                  $controllerProvider,
                  $provide,
                  $mdThemingProvider,
                  PathProvider,
                  NxProvider,
                  $stateProvider,
                  $urlRouterProvider,
                  NavHelperProvider) {
            PathProvider.setBase(baseUrl);

            app.register = {
                controller: $controllerProvider.register,
                directive: $compileProvider.directive,
                factory: $provide.factory,
                service: $provide.service
            };

            NavHelperProvider.addToMenu('IotDM', {
                "link": "#/iotdm",
                "active": "main.iotdm",
                "title": "IotDM",
                "icon": "", // Add navigation icon css class here
                "page": {
                    "title": "iotdm",
                    "description": "iotdm"
                }
            });

            //$urlRouterProvider.otherwise("/iotdm");

            var access = routingConfig.accessLevels;
            $stateProvider.state('main.iotdm', {
                url: "iotdm",
                access: access.admin,
                views: {
                    'content': {
                        templateUrl: PathProvider.base() + 'template/iotdm-gui.tplt.html',
                        controller: 'IotdmGuiCtrl',
                        controllerAs: "ctrl"
                    }
                }
            });

            $stateProvider.state('main.iotdm.retrieve-cse', {
                url: "/retrieve-cse",
                templateUrl: PathProvider.base() + 'template/side-panel-retrieve-cse.tplt.html',
                controller: 'SidePanelRetrieveCSECtrl',
                controllerAs: "ctrl"
            });

            $stateProvider.state('main.iotdm.create', {
                url: "/create/{operation:int}",
                templateUrl: PathProvider.base() + 'template/side-panel-crud.tplt.html',
                controller: 'SidePanelCRUDCtrl',
                controllerAs: "ctrl"
            });

            $stateProvider.state('main.iotdm.retrieve', {
                url: "/retrieve/{operation:int}",
                templateUrl: PathProvider.base() + 'template/side-panel-crud.tplt.html',
                controller: 'SidePanelCRUDCtrl',
                controllerAs: "ctrl"
            });

            $stateProvider.state('main.iotdm.update', {
                url: "/update/{operation:int}",
                templateUrl: PathProvider.base() + 'template/side-panel-crud.tplt.html',
                controller: 'SidePanelCRUDCtrl',
                controllerAs: "ctrl"
            });

            $stateProvider.state('main.iotdm.delete', {
                url: "/delete/{operation:int}",
                templateUrl: PathProvider.base() + 'template/side-panel-crud.tplt.html',
                controller: 'SidePanelCRUDCtrl',
                controllerAs: "ctrl"
            });

            $stateProvider.state('main.iotdm.info', {
                url: "/info",
                templateUrl: PathProvider.base() + 'template/side-panel-info.tplt.html',
                controller: 'SidePanelInfoCtrl',
                controllerAs: "ctrl"
            });


            $mdThemingProvider.theme('default')
                .primaryPalette('blue')
                .accentPalette('light-blue');


            NxProvider.icons({
                icons: ["CSEBase", "AE", "container", "contentInstance", "group", "node", "accessControlPolicy", "observe", "unobserve", "subscription"],
                location: PathProvider.base() + "icon",
                format: "svg",
                width: 50,
                height: 50
            });
        }
    );
});
