require.config({
    paths: {
        'moment' : 'app/onem2m-ui/vendor/moment/min/moment.min',
        'next':'app/onem2m-ui/vendor/next-bower/js/next.min',
        'angular-material-datetimepicker':'app/onem2m-ui/js/directives/angular-material-datetimepicker'
    },
    shim:{
        'moment':{
          exports:'moment'
        },
        'next':{
          exports:'nx'
        },
        'angular-material-datetimepicker': ['moment','ngMaterial']
    }
});

define(['app/onem2m-ui/js/onem2m-ui.module']);
