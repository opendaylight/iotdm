define(['app/onem2m-ui/js/services/module'],function(app){


    function TopologyService(nx, Onem2m) {
        var _layout = null;
        var _dataStoreAccessKey = null;
        var _selectNodeListeners = null;
        var _unSelectNodeListeners = null;
        var _topo = null;
        var _selectedNodeId = null;

        //todo: isSelectNodeAction is stopPropagation Flag since Event.stopPropagation not work. Need to ask
        this.initTopology = initTopology;
        this.layout = layout;
        this.setDataStoreAccessKey = setDataStoreAccessKey;
        this.update = update;
        this.getSelectedNodeId = getSelectedNodeId;
        this.addSelectNodeListener = addSelectNodeListener();
        this.addUnSelectNodeListener = addUnSelectNodeListener();
        this.removeSelectNodeListener = removeSelectNodeListener;
        this.removeUnSelectNodeListener = removeUnSelectNodeListener;
        this.adaptToContainer = adaptToContainer;

        init();

        function init() {
            _layout = function(data) {};

            _dataStoreAccessKey = {
                getData: function() {
                    return null;
                }
            };

            _selectNodeListeners = {};
            _unSelectNodeListeners = {};

            _topo = new nx.graphic.Topology({
                adaptive: true,
                nodeConfig: {
                    iconType: function(vertex) {
                        return Onem2m.icon(vertex.getData());
                    },
                    label: function(vertex) {
                        return Onem2m.label(vertex.getData());
                    }
                },
                tooltipManagerConfig: {
                       showNodeTooltip: false,
                       showLinkTooltip:false
                },
                showIcon: true,
                identityKey: Onem2m.id()
            });

            nx.define('onem2m.Tree', nx.ui.Application, {
                methods: {
                    start: function() {
                        _topo.attach(this);
                    }
                }
            });
        }

        function initTopology(htmlElementId) {
            var application = new onem2m.Tree();
            var isSelectNodeAction = false;
            application.container(document.getElementById(htmlElementId));
            application.start();
            _topo.on('topologyGenerated', function(sender, event) {
                sender.eachNode(function(node) {
                    node.onclickNode(function(sender, event) {
                        selectNode(sender.id());
                        isSelectNodeAction = true;
                    });
                });
                _topo.adaptToContainer();
            });
            _topo.on('clickStage', function(sender, event) {
                if (!isSelectNodeAction) {
                    unSelectNode();
                }
                isSelectNodeAction = false;
            });
        }

        function addSelectNodeListener() {
            var counter = 0;
            return function(listener) {
                _selectNodeListeners[counter++] = listener;
                return counter;
            };
        }

        function addUnSelectNodeListener() {
            var counter = 0;
            return function(listener) {
                _unSelectNodeListeners[counter++] = listener;
                return counter;
            };
        }

        function removeSelectNodeListener(key) {
            delete _selectNodeListeners[key];
        }

        function removeUnSelectNodeListener(key) {
            delete _unSelectNodeListeners[key];
        }

        function layout(layout) {
            _layout = layout;
        }

        function setDataStoreAccessKey(dataStoreAccessKey) {
            _dataStoreAccessKey = dataStoreAccessKey;
        }

        function selectNode(id) {
            _selectedNodeId = id;
            notifyListeners(_selectNodeListeners, _selectedNodeId);
        }

        function unSelectNode() {
            _selectedNodeId = null;
            notifyListeners(_unSelectNodeListeners);
        }

        function notifyListeners(listeners, notification) {
            for (var key in listeners) {
                listeners[key](notification);
            }
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

    TopologyService.$inject = ['NxService', 'Onem2mHelperService'];
    app.service('TopologyService', TopologyService);
});
