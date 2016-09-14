define(['iotdm-gui.services.module'], function (app) {
    'use strict';

    function TopologyService($rootScope, Nx, Onem2m) {
        var _layout = null;
        var _dataStoreAccessKey = null;
        var _topo = null;
        var _selectedNodeId = null;

        this.initTopology = initTopology;
        this.layout = layout;
        this.setDataStoreAccessKey = setDataStoreAccessKey;
        this.update = update;
        this.getSelectedNodeId = getSelectedNodeId;
        this.adaptToContainer = adaptToContainer;

        init();

        function init() {
            _layout = function (data) {
            };

            _dataStoreAccessKey = {
                getData: function () {
                    return null;
                }
            };


            _topo = new Nx.graphic.Topology({
                adaptive: true,
                nodeConfig: {
                    iconType: function (vertex) {
                        return Onem2m.icon(vertex.getData());
                    },
                    label: function (vertex) {
                        return Onem2m.label(vertex.getData());
                    }
                },
                tooltipManagerConfig: {
                    showNodeTooltip: false,
                    showLinkTooltip: false
                },
                showIcon: true,
                identityKey: Onem2m.id()
            });

            Nx.define('onem2m.Tree', Nx.ui.Application, {
                methods: {
                    start: function () {
                        _topo.attach(this);
                    }
                }
            });
        }

        function initTopology(htmlElementId) {
            var application = new onem2m.Tree();
            application.container(document.getElementById(htmlElementId));
            application.start();
            _topo.on('topologyGenerated', function (sender, event) {
                sender.eachNode(function (node) {
                    node.onclickNode(function (sender, event) {
                        selectNode(sender.id());
                    });
                });
            });
            _topo.stage().on('dblclick', function (sender, event) {
                var target = event.target;
                var nodesLayerDom = _topo.getLayer('nodes').dom().$dom;
                var linksLayerDom = _topo.getLayer('links').dom().$dom;
                var nodeSetLayerDom = _topo.getLayer('nodeSet').dom().$dom;
                var id;


                //db click node
                if (nodesLayerDom.contains(target)) {
                    while (!target.classList.contains('node')) {
                        target = target.parentElement;
                    }
                    id = target.getAttribute('data-id');
                    $rootScope.$broadcast('dblclick', id);
                    return;
                }
            });

            // var popup = new Nx.ui.Popover({
            //     width: 300,
            //     height: 200,
            //     offset: 5
            // });
            //
            // _topo.on('contextmenu', function(sender, event) {
            //     popup.open({
            //         target: {
            //             x: event.offsetX,
            //             y: event.offsetY
            //         }
            //     });
            // });

            _topo.on('clickStage', function (sender, event) {
                var target = event.target;
                var nodesLayerDom = _topo.getLayer('nodes').dom().$dom;
                if (!nodesLayerDom.contains(target)) {
                    unSelectNode();
                }
            });
        }

        function layout(layout) {
            _layout = layout;
        }

        function setDataStoreAccessKey(dataStoreAccessKey) {
            _dataStoreAccessKey = dataStoreAccessKey;
        }

        function selectNode(id) {
            $rootScope.$broadcast('selectNode', id);
            _selectedNodeId = id;
        }

        function unSelectNode() {
            $rootScope.$broadcast('unSelectNode', _selectedNodeId);
            _selectedNodeId = null;
        }

        function update() {
            var root = _dataStoreAccessKey.getRoot();
            _layout(root);
            var data = _dataStoreAccessKey.getData();
            _topo.data(data);
        }

        function adaptToContainer() {
            _topo.adaptToContainer();
        }

        function getSelectedNodeId() {
            return _selectedNodeId;
        }
    }

    TopologyService.$inject = ['$rootScope', 'Nx', 'Onem2mHelperService'];
    app.service('TopologyService', TopologyService);
});
