define(['iotdm-gui.controllers.module'], function(app) {
    'use strict';

    function IotdmGuiCtrl($scope, $timeout, Topology, DataStore, TreeLayout, Path, $state, CRUD) {
        var _this = this;

        _this.isSelect = false;
        _this.sidePanel = {
            hide: true
        };

        _this.openSidePanel = openSidePanel;
        _this.closeSidePanel = closeSidePanel;

        function openSidePanel() {
            _this.sidePanel.hide = false;
        }

        function closeSidePanel() {
            _this.sidePanel.hide = true;
        }

        function init() {
            $scope.$on('closeSidePanel', function() {
                closeSidePanel();
            });

            $scope.$on('dblclick', function(event, args) {
                var id = args;
                var node = DataStore.retrieveNode(id);
                CRUD.retrieveChildren(node).then(function(data) {
                    DataStore.addNode(data);
                    Topology.update();
                });
            });


            $scope.$on('selectNode',function(event,id){
                _this.isSelect = true;
                openSidePanel();
                $scope.$apply();
                $state.go('main.iotdm.info');
            });

            $scope.$on('unSelectNode',function(event,id){
                _this.isSelect = false;
                closeSidePanel();
                $scope.$apply();
            });


            var treeLayout = TreeLayout.init(50, 50);
            Topology.initTopology('topology');
            Topology.layout(treeLayout);
            Topology.setDataStoreAccessKey(DataStore.getAccessKey());
        }
        init();
    }

    IotdmGuiCtrl.$inject = ['$scope', '$timeout', 'TopologyService', 'DataStoreService', 'TreeLayoutService', 'Path', '$state', 'Onem2mCRUDService'];
    app.controller('IotdmGuiCtrl', IotdmGuiCtrl);
});
