define(['iotdm-gui.directives.module'], function(app) {
    'use strict';
    function onem2mInput($compile, $parse,Onem2mInputComponent) {

        return {
            restrict: 'E',
            scope: {},
            require: 'ngModel',
            link: function(scope, element, attrs, ngModelCtrl) {
                scope.labelName = attrs.labelName;
                scope.name = attrs.name;

                var componentScope = Onem2mInputComponent.scope(scope.name);
                var componentTemplate = Onem2mInputComponent.template(scope.name);

                angular.extend(scope, componentScope);
                element.append($compile(componentTemplate)(scope));

                scope.$watch('value', function() {
                    ngModelCtrl.$setViewValue(scope.value);
                });

                ngModelCtrl.$render = function() {
                    scope.value = ngModelCtrl.$viewValue;
                };

                ngModelCtrl.$formatters.push(Onem2mInputComponent.handler(scope.name).toView);
                ngModelCtrl.$parsers.push(Onem2mInputComponent.handler(scope.name).toModel);
            }
        };
    }
    onem2mInput.$inject = ['$compile', '$parse','Onem2mInputComponentService'];
    app.directive('onem2mInput', onem2mInput);
});
