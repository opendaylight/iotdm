define(['iotdm-gui.controllers.module'], function(app) {
    'use strict';
    function SidePanelRetrieveCSECtrl($scope, DataStore, Topology, CRUD, Alert) {
        var _this = this;

        _this.submit = submit;
        _this.host = "localhost";
        _this.port = "8282";
        _this.CSEName = "InCSE1";
        _this.allDescendant = false;

        function submit(host, port, cseBase) {
            var retrieveFn = _this.allDescendant ? getCSEAndDescendant : getCSE;

            retrieveFn(host, port, cseBase).then(function(data) {
                CRUD.setBaseDir(host, port, cseBase);
                DataStore.reset();
                DataStore.addNode(data);
                Topology.update();
                $scope.$emit("closeSidePanel");
                Alert("Retrieve CSE Successfully", 'success');
            }, function(error) {
                Alert(error,'warn');
            });
        }

        function getCSE(host, port, cseBase) {
            return CRUD.retrieveCSE(host, port, cseBase);
        }

        function getCSEAndDescendant(host, port, cseBase) {
            return CRUD.discovery(host, port, cseBase);
        }
    }

    SidePanelRetrieveCSECtrl.$inject = ['$scope', 'DataStoreService', 'TopologyService', 'Onem2mCRUDService', 'AlertService'];
    app.controller('SidePanelRetrieveCSECtrl', SidePanelRetrieveCSECtrl);
});
