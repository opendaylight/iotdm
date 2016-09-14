define(['iotdm-gui.services.module'], function(app) {
    'use strict';
    var MIME = "application/json";
    var HOST = 'localhost';
    var PORT = '8181';

    function Onem2mCRUDService($http, $q, Onem2m) {
        this.CRUD = CRUD;
        this.setBaseDir = setBaseDir;
        this.retrieveCSE = retrieveCSE;
        this.discovery = discovery;
        this.retrieveChildren = retrieveChildren;

        function retrieveChildren(node) {
            var request = {};
            request.op = Onem2m.operation.retrieve;
            request.fr = Onem2m.assignFrom();
            request.rqi = Onem2m.assignRequestIdentifier();
            request.to = Onem2m.id(node);
            request.rcn = Onem2m.resultContent["attributes and child resources"];
            return CRUD(request);
        };

        function setBaseDir(host, port) {
            HOST = host;
            PORT = port;
        }

        function retrieveCSE(host, port, cseBase) {
            var request = {
                to: cseBase,
                op: Onem2m.operation.retrieve,
                rqi: Onem2m.assignRequestIdentifier(),
                fr: Onem2m.assignFrom(),
            };
            return CRUD(request, host, port);
        }

        function discovery(host, port, cseBase) {
            var request = {
                to: "",
                op: Onem2m.operation.retrieve,
                rqi: Onem2m.assignRequestIdentifier(),
                fr: Onem2m.assignFrom(),
                fc: {
                    fu: Onem2m.filterUsage["Discovery Criteria"]
                }
            };
            return CRUD(request, host, port);
        }

        function CRUD(request, host, port) {
            switch (request.op) {
                case Onem2m.operation.create:
                    return _create(request, host, port);
                case Onem2m.operation.retrieve:
                    return _retrieve(request, host, port);
                case Onem2m.operation.update:
                    return _update(request, host, port);
                case Onem2m.operation.delete:
                    return _delete(request, host, port);
            }
        }

        function _retrieve(request, host, port) {
            var httpRequest = parseRequest(request, host, port);
            return $http.get(httpRequest.url, {
                headers: httpRequest.headers
            }).then(function(httpResponse) {
                return parseHttpResponse(httpResponse);
            }, handleResponseError);
        }

        function _create(request, host, port) {
            var httpRequest = parseRequest(request, host, port);
            var attrsSent = httpRequest.payload;
            return $http.post(httpRequest.url, httpRequest.payload, {
                headers: httpRequest.headers
            }).then(function(httpResponse) {
                var attrsReceived = parseHttpResponse(httpResponse);
                var data = combineAttrs(attrsSent, attrsReceived);
                return data;
            }, handleResponseError);
        }

        function _update(request, host, port) {
            var httpRequest = parseRequest(request, host, port);
            var attrsSent = httpRequest.payload;
            var ri = request.to;
            return $http.put(httpRequest.url, httpRequest.payload, {
                headers: httpRequest.headers
            }).then(function(httpResponse) {
                var data = parseHttpResponse(httpResponse);
                var key = getWrapper(data);
                data[key].ri = ri;
                return data;
            }, handleResponseError);
        }

        function _delete(request, host, port) {
            var httpRequest = parseRequest(request, host, port);
            return $http.delete(httpRequest.url, {
                headers: httpRequest.headers
            }).then(function(httpResponse) {
                return parseHttpResponse(httpResponse);
            }, handleResponseError);
        }

        function handleResponseError(error) {
            var result = chain(error, 'data', 'error');
            return $q.reject("Error:" + result);
        }

        function chain(root) {
            var pointer = root;
            for (var i = 1; i < arguments.length; i++) {
                var arg = arguments[i];
                if (pointer[arg] === undefined || pointer[arg] === null)
                    break;
                pointer = pointer[arg];
            }
            return angular.toJson(pointer);
        }

        function parseRequest(request, host, port) {
            host = host ? host : HOST;
            port = port ? port : PORT;
            var url = "http://" + host + ":" + port + "/" + request.to;
            var query = {};
            query.rt = request.rt && request.rt.rtv;
            query.rp = request.rp;
            query.rcn = request.rcn;
            query.da = request.da;
            if (request.fc) {
                var fc = request.fc;
                query.crb = fc.crb;
                query.cra = fc.cra;

                query.ms = fc.ms;
                query.us = fc.us;

                query.sts = fc.sts;
                query.stb = fc.stb;

                query.exb = fc.exb;
                query.exa = fc.exa;

                query.lbl = fc.lbl && fc.lbl.join("+");
                query.ty = fc.ty;

                query.sza = fc.sza;
                query.szb = fc.szb;

                query.cty = fc.cty && fc.cty.join("+");
                query.lim = fc.lim;

                if (fc.atr) {
                    fc.atr.forEach(function(d) {
                        query[d.nm] = d.val;
                    });
                }
                query.fu = fc.fu;
            }
            query.drt = request.drt;

            var queryArray = [];
            for (var key in query) {
                if (query[key]) {
                    queryArray.push(key + "=" + query[key]);
                }
            }

            url = queryArray.length > 0 ? url + "?" + queryArray.join("&") : url;


            var headers = {};
            headers["Accept"] = MIME;
            if (request.op)
                headers["Content-Type"] = request.op == 1 ? MIME + ";ty=" + request.ty : MIME;

            if (request.fr)
                headers["X-M2M-Origin"] = request.fr;

            if (request.rqi)
                headers["X-M2M-RI"] = request.rqi;

            if (request.gid)
                headers["X-M2M-GID"] = request.gid;

            if (request.rt && request.rt.nu && request.rt.nu.length > 0)
                headers["X-M2M-RTU"] = request.rt.nu.join("&");

            if (request.ot)
                headers["X-M2M-OT"] = request.ot;

            if (request.rst)
                headers["X-M2M-RST"] = request.rst;

            if (request.ret)
                headers["X-M2M-RET"] = request.ret;

            if (request.oet)
                headers["X-M2M-OET"] = request.oet;

            if (request.ec)
                headers["X-M2M-EC"] = request.ec;

            var payload = request.pc;

            var httpRequest = {
                url: url,
                payload: payload,
                headers: headers
            };
            return httpRequest;
        }

        function parseHttpResponse(httpResponse) {
            var response = {};
            var headers = httpResponse.headers();

            response.rsc = headers["X-M2M-RSC".toLowerCase()];
            response.rqi = headers["X-M2M-RI".toLowerCase()];
            response.pc = httpResponse.data;
            response.fr = httpResponse["X-M2M-Origin".toLowerCase()];
            response.ot = headers["X-M2M-OT".toLowerCase()];
            response.rst = headers["X-M2M-RST".toLowerCase()];
            response.ec = headers["X-M2M-EC".toLowerCase()];
            return response.pc;
        }

        function getWrapper(object) {
            return Object.keys(object)[0];
        }

        function combineAttrs(oldAttrs, newAtts) {
            var key = getWrapper(oldAttrs);
            for (var k in newAtts[key]) {
                oldAttrs[key][k] = newAtts[key][k];
            }
            return oldAttrs;
        }
    }

    Onem2mCRUDService.$inject = ['$http', '$q', 'Onem2mHelperService'];
    app.service('Onem2mCRUDService', Onem2mCRUDService);
});
