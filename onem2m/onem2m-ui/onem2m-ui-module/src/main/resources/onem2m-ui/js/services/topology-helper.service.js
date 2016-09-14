define(['iotdm-gui.services.module'], function(app) {
    'use strict';
  function TopologyHelperService(DataStore,Topology){
    this.getSelectedNode=getSelectedNode;

    function getSelectedNode(){
      var id=Topology.getSelectedNodeId();
      return DataStore.retrieveNode(id);
    }
  }
  TopologyHelperService.$inject=['DataStoreService','TopologyService'];
  app.service("TopologyHelperService",TopologyHelperService);
});
