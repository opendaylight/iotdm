define(['app/onem2m-ui/js/filters/module'], function(app) {
    function shortToLong(Onem2m) {
        return function(shortName) {
            return Onem2m.toLong(shortName);
        };
    }

    shortToLong.$inject = ['Onem2mHelperService'];
    app.filter("shortToLong", shortToLong);
});
