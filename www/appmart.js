var exec = require('cordova/exec');
var MyPlugin = function() {};

//初期設定
MyPlugin.prototype.start_setup = function(application_id, license_key, successCallback,errorCallback){
    cordova.exec(
        successCallback, errorCallback, 
        'Appmart', 'start_setup', 
        [{                 
            "appId": application_id,
            "licenceKey": license_key
        }]
    );       
}

// Inventory取得
MyPlugin.prototype.query_inventory_async = function(skusList, successCallback,errorCallback){
    cordova.exec(
        successCallback, errorCallback, 
        'Appmart', 'query_inventory_async', 
        [{                 
            "skusList": skusList
        }]
    );       
}

//決済実行
MyPlugin.prototype.launch_purchase_flow = function(skuId, payload, successCallback, errorCallback){
    cordova.exec(
        successCallback, errorCallback, 
        'Appmart', 'launch_purchase_flow', 
        [{                 
            "serviceId": skuId,
            "payload": payload
        }]
    );       
}

//購入消費
MyPlugin.prototype.consume_async = function(skuId, successCallback, errorCallback){
    cordova.exec(
        successCallback, errorCallback, 
        'Appmart', 'consume_async',
        [{                 
            "serviceId": skuId
        }]
    );       
}

var myplugin = new MyPlugin();
module.exports = myplugin;