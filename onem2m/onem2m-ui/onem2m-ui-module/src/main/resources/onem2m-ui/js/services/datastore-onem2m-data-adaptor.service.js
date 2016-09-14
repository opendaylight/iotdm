define(['iotdm-gui.services.module'], function(app) {
    'use strict';
    function DataStoreOnem2mDataAdaptorService(Onem2m) {
        function onem2mDataAdaptor(data) {
            var array = [];

            function helper(data) {
                if (angular.isArray(data)) {
                    data.forEach(function(d) {
                        helper(d);
                    });
                } else if (angular.isObject(data)) {
                    data = adaptData(data);
                    var children = Onem2m.children(data);
                    if (children) {
                        children.forEach(function(child) {
                            helper(child);
                        });
                        delete data.value.ch;
                    }
                    array.push(data);
                }
            }
            helper(data);
            return array;
        }

        function adaptData(data) {
            if (!data.key && angular.isObject(data)) {
                var key = Object.keys(data)[0];
                var value = data[key];
                return {
                    key: key,
                    value: value
                };
            }
            return data;
        }
        return onem2mDataAdaptor;
    }

    DataStoreOnem2mDataAdaptorService.$inject = ['Onem2mHelperService'];
    app.service('DataStoreOnem2mDataAdaptorService', DataStoreOnem2mDataAdaptorService);
})
