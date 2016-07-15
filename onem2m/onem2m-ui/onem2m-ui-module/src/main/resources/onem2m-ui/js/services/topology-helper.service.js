define(['app/onem2m-ui/js/services/module'],function(app){
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
