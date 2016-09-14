define(['iotdm-gui.services.module'], function(app) {
    'use strict';

    var stringTemplate = '' +
        '<md-input-container class="md-block">' +
        '   <ng-form name="form">' +
        '       <label>{{labelName|shortToLong}}</label>' +
        '       <input ng-model="value" ng-disabled="disabled">' +
        '   </ng-form>' +
        '</md-input-container>';

    var numberTemplate = '' +
        '<md-input-container class="md-block">' +
        '   <ng-form name="form">' +
        '       <label>{{labelName|shortToLong}}</label>' +
        '       <input ng-model="value">' +
        '   </ng-form>' +
        '</md-input-container>';

    var textareaTemplate = '' +
        '<md-input-container class="md-block">' +
        '   <ng-form name="form">' +
        '       <label>{{labelName|shortToLong}}</label>' +
        '       <textarea ng-model="value"></textarea>' +
        '   </ng-form>' +
        '</md-input-container>';

    var selectTemplate = '' +
        '<md-input-container class="md-block">' +
        '   <ng-form name="form">' +
        '       <label>{{labelName|shortToLong}}</label>' +
        '       <md-select ng-model="value">' +
        '           <md-option ng-value="k" ng-repeat="(k,v) in options">{{k}}</md-option>' +
        '       </md-select>' +
        '   </ng-form>' +
        '</md-input-container>';

    var mulSelectTemplate = '' +
        '<md-input-container class="md-block">' +
        '   <ng-form name="form">' +
        '       <label>{{labelName|shortToLong}}</label>' +
        '       <md-select ng-model="value" multiple>' +
        '           <md-option ng-value="k" ng-repeat="(k,v) in options">{{k}}</md-option>' +
        '       </md-select>' +
        '   </ng-form>' +
        '</md-input-container>';

    var timeTemplate = '' +
        '<md-input-container class="md-block">' +
        '   <ng-form name="form">' +
        '       <div class="icons-right">' +
        '           <span class="material-icons">date_range</span>' +
        '       </div>' +
        '       <label>{{labelName|shortToLong}}</label>' +
        '       <input ng-model="value" mdc-datetime-picker format="YYYYMMDDTHHmmss" date="true" time="true">' +
        '   </ng-form>' +
        '</md-input-container>';


    function Onem2mInputComponentService($log, $filter, Onem2m) {
        var collection = {};

        var defaultHandler = handleString();
        var defaultScope = {
            disabled: true
        };
        var defaultTemplate = stringTemplate;

        this.register = register;
        this.handler = handler;
        this.scope = scope;
        this.template = template;

        function register(name, template, handler, scope) {
            collection[name] = {
                template: template,
                handler: handler,
                scope: scope
            };
        }

        function handler(name) {
            if (collection[name]) {
                return collection[name].handler;
            }
            $log.warn("No such attr: " + name + " avaible,use default handler");
            return defaultHandler;
        }

        function scope(name) {
            if (collection[name]) {
                return collection[name].scope;
            }
            $log.warn("No such attr: " + name + " avaible,use default scope");
            return defaultScope;
        }

        function template(name) {
            if (collection[name]) {
                return collection[name].template;
            }
            $log.warn("No such attr: " + name + " avaible,use default template");
            return defaultTemplate;
        }


        register("op", selectTemplate, handleEnum(Onem2m.operation), {
            options: Onem2m.operation
        });
        register("to", stringTemplate, handleString());
        register("fr", stringTemplate, handleString());
        register("rqi", stringTemplate, handleString());
        register("ty", selectTemplate, handleEnum(Onem2m.resourceType), {
            options: Onem2m.resourceType
        });
        register("rol", stringTemplate, handleString());
        register("rset", timeTemplate, handleTime());
        register("oet", timeTemplate, handleTime());
        register("rqet", timeTemplate, handleTime());
        register("rtv", selectTemplate, handleEnum(Onem2m.responseType), {
            options: Onem2m.responseType
        });
        register("nu", stringTemplate, handleString());
        register("rp", timeTemplate, handleTime());
        register("rcn", selectTemplate, handleEnum(Onem2m.resultContent), {
            options: Onem2m.resultContent
        });
        register("ec", selectTemplate, handleEnum(Onem2m.stdEventCats), {
            options: Onem2m.stdEventCats
        });
        register("da", selectTemplate, handleEnum(Onem2m.boolean), {
            options: Onem2m.boolean
        });
        register("gid", stringTemplate, handleString());
        register("crb", timeTemplate, handleTime());
        register("cra", timeTemplate, handleTime());
        register("ms", timeTemplate, handleTime());
        register("us", timeTemplate, handleTime());
        register("sts", numberTemplate, handleNumber());
        register("stb", numberTemplate, handleNumber());
        register("exb", timeTemplate, handleTime());
        register("exA", timeTemplate, handleTime());
        register("lbl", stringTemplate, handleString());
        register("sza", numberTemplate, handleNumber());
        register("szb", numberTemplate, handleNumber());
        register("cty", stringTemplate, handleString());
        register("name", stringTemplate, handleString());
        register("fu", selectTemplate, handleEnum(Onem2m.filterUsage), {
            options: Onem2m.filterUsage
        });
        register("lim", numberTemplate, handleNumber());
        register("drt", selectTemplate, handleEnum(Onem2m.discResType), {
            options: Onem2m.discResType
        });
        register("ct", timeTemplate, handleTime());
        register("srt", selectTemplate, handleEnum(Onem2m.resourceType), {
            options: Onem2m.resourceType
        });
        register("cst", stringTemplate, handleString());
        register("csi", stringTemplate, handleString());
        register("ri", stringTemplate, handleString());
        register("lt", timeTemplate, handleTime());
        register("ot", timeTemplate, handleTime());
        register("rn", stringTemplate, handleString());
        register("pi", stringTemplate, handleString());
        register("ri", stringTemplate, handleString());
        register("acpi", stringTemplate, handleString());
        register("et", timeTemplate, handleTime());
        register("at", stringTemplate, handleString());
        register("aa", stringTemplate, handleString());
        register("apn", stringTemplate, handleString());
        register("api", stringTemplate, handleString());
        register("aei", stringTemplate, handleString());
        register("poa", stringTemplate, handleString());
        register("or", stringTemplate, handleString());
        register("rr", selectTemplate, handleEnum(Onem2m.boolean), {
            options: Onem2m.boolean
        });
        register("st", numberTemplate, handleNumber());
        register("cr", stringTemplate, handleString());
        register("mni", numberTemplate, handleNumber());
        register("mbs", numberTemplate, handleNumber());
        register("mia", numberTemplate, handleNumber());
        register("cni", numberTemplate, handleNumber());
        register("cbs", numberTemplate, handleNumber());
        register("li", stringTemplate, handleString());
        register("disr", selectTemplate, handleEnum(Onem2m.boolean), {
            options: Onem2m.boolean
        });
        register("la", stringTemplate, handleString());
        register("ol", stringTemplate, handleString());
        register("cnf", stringTemplate, handleString());
        register("cs", numberTemplate, handleNumber());
        register("acor", stringTemplate, handleString());
        register("acop", mulSelectTemplate, function() {
            return {
                toView: function(value) {
                    if (sanityCheck(value)) {
                        var bits = value.toString(2);
                        var rst = [];
                        var toView = handleEnum(Onem2m.accessControlOperations).toView;

                        for (var i = bits.length - 1; i >= 0; i--) {
                            var length = bits.length - i - 1;
                            if (bits[i] === '1') {
                                var name = toView(Math.pow(2, length));
                                rst.push(name);
                            }
                        }
                        return rst.length > 0 ? rst.join(',') : value;
                    }
                    return value;
                },
                toModel: function(array) {
                    if (sanityCheck(array)) {
                        var sum = 0;
                        var toModel = handleEnum(Onem2m.accessControlOperations).toModel;
                        if (angular.isArray(array)) {
                            array.forEach(function(v) {
                                sum += toModel(v);
                            });
                        } else {
                            sum = toModel(array);
                        }
                        return sum;
                    }
                    return null;
                }
            };
        }(), {
            options: Onem2m.accessControlOperations
        });

        register("actv", stringTemplate, handleString());
        register("ipv4", stringTemplate, handleString());
        register("ipv6", stringTemplate, handleString());
        register("accc", stringTemplate, handleString());
        register("accr", numberTemplate, handleNumber());
        register("om", selectTemplate, handleEnum(Onem2m.resourceType), {
            options: Onem2m.resourceType
        });
        register("net", selectTemplate, handleEnum(Onem2m.notificationContentType), {
            options: Onem2m.notificationContentType
        });
        register("exc", numberTemplate, handleNumber());
        register("gpi", stringTemplate, handleString());
        register("nfu", stringTemplate, handleString());
        register("num", numberTemplate, handleNumber());
        register("dur", stringTemplate, handleString());
        register("mnn", numberTemplate, handleNumber());
        register("tww", stringTemplate, handleString());
        register("psn", numberTemplate, handleNumber());
        register("pn", selectTemplate, handleEnum(Onem2m.pendingNotification), {
            options: Onem2m.pendingNotification
        });
        register("nsp", numberTemplate, handleNumber());
        register("ln", selectTemplate, handleEnum(Onem2m.boolean), {
            options: Onem2m.boolean
        });
        register("nct", selectTemplate, handleEnum(Onem2m.notificationContentType), {
            options: Onem2m.notificationContentType
        });
        register("nec", numberTemplate, handleNumber());
        register("su", stringTemplate, handleString());
        register("mt", selectTemplate, handleEnum(Onem2m.memberType), {
            options: Onem2m.memberType
        });
        register("cnm", numberTemplate, handleNumber());
        register("mnm", numberTemplate, handleNumber());
        register("mid", stringTemplate, handleString());
        register("macp", stringTemplate, handleString());
        register("csy", selectTemplate, handleEnum(Onem2m.consistencyStrategy), {
            options: Onem2m.consistencyStrategy
        });
        register("gn", stringTemplate, handleString());
        register("ni", stringTemplate, handleString());
        register("hcl", stringTemplate, handleString());
        register("con", textareaTemplate, handleString());


        function sanityCheck(value) {
            return value !== null && value !== undefined;
        }

        function handleNumber() {
            return {
                toView: function(num) {
                    if (sanityCheck(num))
                        return num.toString();
                },
                toModel: function(str) {
                    if (sanityCheck(str))
                        return angular.isNumber(num) ? Number(num) : num;
                }
            };
        }

        function handleString() {
            var handler = function(str) {
                if (sanityCheck(str))
                    return str.toString();
            };
            return {
                toView: handler,
                toModel: handler
            };
        }

        function handleTime() {
            return {
                toView: function(tt) {
                    var t = $filter('date')(tt, 'yyyyMMddTHHmmss', 'UTC');
                    return $filter('date')(t, 'MMM d, y h:mm:ss a Z');
                },
                toModel: function(tt) {
                    var t = new Date(tt);
                    var year = t.getUTCFullYear();
                    var month = t.getUTCMonth();
                    var date = t.getUTCDate();
                    var hour = t.getUTCHours();
                    var min = t.getUTCMinutes();
                    var second = t.getUTCSeconds();
                    return "" + year + month + date + "T" + hour + min + second;
                }
            };
        }

        function handleEnum(collection) {
            return {
                toView: function(value) {
                    for (var key in collection) {
                        if (collection[key] === value) {
                            return key;
                        }
                    }
                    return value;
                },
                toModel: function(key) {
                    if (key in collection)
                        return collection[key];
                    return key;
                }
            };
        }
    }

    Onem2mInputComponentService.$inject = ['$log', '$filter', 'Onem2mHelperService'];
    app.service('Onem2mInputComponentService', Onem2mInputComponentService);
});
