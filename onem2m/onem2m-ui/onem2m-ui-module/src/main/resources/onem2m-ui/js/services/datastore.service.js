define(['iotdm-gui.services.module'], function(app) {
    'use strict';
    var cacheNodesById = {};
    var cacheLinksBySourceId = {};

    var selectNodeListeners = [];
    var unSelectNodeListeners = [];
    var root = null;

    function DataStoreService(Onem2m, Onem2mDataAdaptor, CRUD) {

        this.getAccessKey = getAccessKey;
        this.addNode = addNode;
        this.updateNode = updateNode;
        this.removeNode = removeNode;
        this.retrieveNode = retrieveNode;
        this.reset=reset;

        function getAccessKey() {
            return {
                getData: function() {
                    return {
                        nodes: values(cacheNodesById),
                        links: values(cacheLinksBySourceId)
                    };
                },
                getRoot: function() {
                    return root;
                }
            };
        }

        function addNode(node) {
            var nodes = Onem2mDataAdaptor(node);

            nodes.forEach(function(node) {
                var id = Onem2m.id(node);
                var parentId = Onem2m.parentId(node);
                var parentNode = retrieveNode(parentId);

                if (id) {
                    if (cacheNodesById[id]) {
                        cacheNodesById[id].value = node.value;
                    } else {
                        cacheNodesById[id] = node;
                    }
                }

                if (parentId && id) {
                    cacheLinksBySourceId[id] = {
                        source: id,
                        target: parentId
                    };
                    var children = parentNode.children ? parentNode.children : [];
                    children.push(node);
                    parentNode.children = children;
                    node.parent = parentNode;
                }

                if (!parentId) {
                    if (root) {
                        root.value = node.value;
                    } else {
                        root = node;
                    }
                }
            });
        }

        function updateNode(node) {
            var nodes = Onem2mDataAdaptor(node);
            nodes.forEach(function(node) {
                var id = Onem2m.id(node);
                if (id) {
                    var old = cacheNodesById[id];
                    for (var key in node.value) {
                        old.value[key] = node.value[key];
                    }
                }
            });
        }

        function removeNode(node) {
            node = Onem2mDataAdaptor(node)[0];
            var id = Onem2m.id(node);
            node = cacheNodesById[id];
            var array = [node];
            while (array.length > 0) {
                node = array.pop();
                id = Onem2m.id(node);
                if (cacheNodesById[id]) {
                    delete cacheNodesById[id];
                }
                if (cacheLinksBySourceId[id]) {
                    delete cacheLinksBySourceId[id];
                }
                if (node.children) {
                    array = array.concat(array, node.children);
                }
            }

            var parent = node.parent;
            if (parent) {
                var index = parent.children.indexOf(node);
                parent.children.splice(index, 1);
                if (parent.children.length === 0) {
                    delete parent.children;
                }
            }
        }


        function retrieveNode(id) {
            return cacheNodesById[id];
        }



        function reset() {
            cacheNodesById = {};
            cacheLinksBySourceId = {};
            root = null;
            selectNodeListeners = [];
            unSelectNodeListeners = [];
        }

        function values(object) {
            var array = [];
            for (var key in object) {
                array.push(object[key]);
            }
            return array;
        }
    }

    DataStoreService.$inject = ['Onem2mHelperService', 'DataStoreOnem2mDataAdaptorService', 'Onem2mCRUDService'];
    app.service('DataStoreService', DataStoreService);
})
