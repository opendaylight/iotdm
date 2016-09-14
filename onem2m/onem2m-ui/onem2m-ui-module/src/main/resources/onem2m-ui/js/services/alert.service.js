define(['iotdm-gui.services.module'], function(app) {
    'use strict';
  function AlertService($mdToast){
    return function(message,style){
      var element=angular.element( document.querySelector( '#iotdm' ) );
      $mdToast.show($mdToast.simple().textContent(message).theme(style).parent(element));
    };
  }
  AlertService.$inject=['$mdToast'];
  app.service("AlertService",AlertService);
})
