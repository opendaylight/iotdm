define(['iotdm-gui.directives.module'], function(app) {
    'use strict';
    function Onem2mInputComponentCustom(Onem2mInputComponent) {
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function(scope, element, attrs, ngModel) {
                //format text going to user (model to view)
                var name = attrs.onem2mInputComponentCustom;
                ngModel.$formatters.push(Onem2mInputComponent.handler(name).toView);
                // format text from the user (view to model)
                ngModel.$parsers.push(Onem2mInputComponent.handler(name).toModel);
            }
        };
    }
    Onem2mInputComponentCustom.$inject = ['Onem2mInputComponentService'];
    app.directive('onem2mInputComponentCustom', Onem2mInputComponentCustom);
})
