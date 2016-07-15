define(['app/onem2m-ui/js/services/module','next'],function(app){
    function NxService(){
        if(!nx)
            throw "No nx exist";
        return nx;
    }
    NxService.$inject=[];
    app.service('NxService',NxService);
});
