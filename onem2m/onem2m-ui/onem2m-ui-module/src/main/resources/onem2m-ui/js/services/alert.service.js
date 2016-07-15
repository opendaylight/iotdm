define(['app/onem2m-ui/js/services/module'],function(app){
  function AlertService($mdToast){
    return function(message,style){
      var element=angular.element( document.querySelector( '#toast-container' ) );
      $mdToast.show($mdToast.simple().textContent(message).theme(style).parent(element));
    };
  }
  AlertService.$inject=['$mdToast'];
  app.service("AlertService",AlertService);
});
