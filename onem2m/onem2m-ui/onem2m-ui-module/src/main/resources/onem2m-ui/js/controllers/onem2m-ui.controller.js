
define(['app/onem2m-ui/js/controllers/module'], function(app) {
    function Onem2mUICtrl($scope, $timeout, Topology, DataStore, TreeLayout) {
        var _this = this;

        _this.isSelect = false;
        _this.sidePanel = {
            hide: true,
            mode: "",
            template: ""
        };

        _this.openSidePanel = openSidePanel;
        _this.closeSidePanel = closeSidePanel;

        function openSidePanel(mode) {
            _this.sidePanel.mode = mode;
            _this.sidePanel.template = 'src/app/onem2m-ui/template/side-panel-{{0}}.tplt.html'.replace("{{0}}", mode);
            _this.sidePanel.hide = false;
        }

        function closeSidePanel() {
            _this.sidePanel.mode = "";
            _this.sidePanel.template = "";
            _this.sidePanel.hide = true;
        }

        function init() {
            $scope.$on('closeSidePanel', function() {
                closeSidePanel();
            });

            $scope.$watch(function() {
                return _this.sidePanel.hide;
            }, function() {
                $timeout(function() {
                    Topology.adaptToContainer();
                }, 1000, false);
            });

            Topology.addSelectNodeListener(function() {
                _this.isSelect = true;
                openSidePanel('info');
                $scope.$apply();
            });

            Topology.addUnSelectNodeListener(function() {
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

    Onem2mUICtrl.$inject = ['$scope', '$timeout', 'TopologyService', 'DataStoreService', 'TreeLayoutService'];
    app.controller('Onem2mUICtrl', Onem2mUICtrl);
});
