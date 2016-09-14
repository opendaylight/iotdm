define(['iotdm-gui.directives.module'], function(app) {
    'use strict';

    function SiderDirectiveCtrl($scope) {
        $scope.min = 0;
        $scope.max = $scope.modes.length - 1;
        $scope.select=$scope.modes.indexOf($scope.mode);

        $scope.$watch("select", function(index) {
            if (index!==null&&index!==undefined)
                $scope.mode = $scope.modes[index];
        });
    }

    app.directive('onem2m-slider', function($compile) {
        return {
            restrict: 'E',
            scope: {
                modes: "=",
                mode: "="
            },
            controller: SiderDirectiveCtrl,
            template: '<md-slider min="{{min}}" max="{{max}}" step="1" ng-model="select"><md-slider>',
        };
    });
});
