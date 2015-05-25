# appmartアプリ内課金V2: phonegap


このプラグインをご利用いただければ、簡単に[appmart](http://app-mart.jp)の[アプリ内課金V2](https://gist.github.com/info-appmart/1204bf0595e5fe7b6667)を実装することができます。 

## APPMARTアプリ内課金V2を導入


先ずは[githubプロジェクト](https://github.com/appmart-japan/appmart-inbilling-v2-library)をローカルにcloneしてください。

```shell
cd /home/user/your_directory
git clone https://github.com/appmart-japan/appmart-inbilling-v2-library.git
```

##### プロジェクトに追加 (eclipse)

androidプロジェクトとしてworkspaceに導入します：

+  File
+  Import
+  Existing Android Code Into Workspace
+  先ほどcloneしたプロジェクトを選択します。

> **注意点**　Eclipseにうまく読み込まれないために、workspace以外のフォルダーにcloneしてください。

![Eclipse:appmart アプリ内課金V2](http://data.app-mart.jp/docs/dev/images/import-library-v2.gif "Eclipse:appmart アプリ内課金 V2")

#### プロジェクトに導入

libraryとしてプロジェクトを導入します：

+  プロジェクトに右クリック　
+  Properties 
+  Android
+  Libraries  :  Add ([appmart-inbilling-as-project]を選択)

![appmart V2:プロジェクトとしてインポート](http://data.app-mart.jp/docs/dev/images/import-as-project-v2.gif "appmart V2:プロジェクトとしてインポート")


## phonegapプラグインをインストール

githubからインストール可能です。

```
$ cordova plugin add https://github.com/appmart-japan/appmart-inbilling-v2-phonegap.git
```
    
## pluginの使い方



#### 初期設定

html内にプラグインのjsファイルをインポートしてください。

```html
<script type="text/javascript" src="appmart.js"></script>
```

 callbacksを用意し、**deviceready** イベントでプラグインの初期設定を行います。

```javascript
//appmart plugin
var mp ;

// Console callback
var callback_console  = function(message) { 
    console.log(message);  
}        
// Alert callback
var callback_alert = function(message) { 
    alert(message); 
}

document.addEventListener("deviceready", onDeviceReady, false);
function onDeviceReady() {
    //初期設定
    mp = cordova.require("appmart-plugin.appmart");
    mp.start_setup('your_application_id','your_license_key',callback_console, callback_console);
}
```
   
> アプリIDとライセンスIDを直してください。

##### start_setupのパラメータ

| n  | 型 |参考               |
|---|---|-------------------|
| 1  |文字列| アプリID  |
| 2  |文字列| ライセンスID| 
| 3  |関数| 成功時callback| 
| 4  |関数| 失敗時callback | 


#### Inventoryを取得

**inventory**を取得することによってエンドユーザーの**購入履歴**と**サービス情報**を取得することができます。

```html
<button value="settlement" onclick="get_inv()" >Inventoryを取得</button>
<div id="my_div"></div>
```

```javascript

//　Inventory取得のcallback
var show_inv = function(inventory){
                            
    // サービスリスト(JSON)
    var sku_details_list = inventory.skuDetails ;
    
    // 所有権を持っているサービスリスト(JSON)
    var purchases_list = inventory.purchases ;
    
    // UI update
    var div;            
    var myNode = document.getElementById("my_div")
    
    //既存ボタンを削除
    while (myNode.firstChild) {
        myNode.removeChild(myNode.firstChild);
    }
    
    //サービスリストをループ
    for(var i=0; i < sku_details_list.length; i++){
        
        // サービス情報を取得
        var service_json = JSON.parse(sku_details_list[i]);
        var div = document.createElement("div");
        
        // 購入ボタン
        var btn = document.createElement("button");
        var t = document.createTextNode(service_json.title+ "を購入");
        btn.appendChild(t);
        btn.setAttribute("onclick", "purchase('"+service_json.productId+"','my payload for phonegap')");
        div.appendChild(btn);
        
        // 消費ボタン
        var btn2 = document.createElement("button");
        var t2 = document.createTextNode(service_json.title+"を消費");
        btn2.appendChild(t2);
        btn2.setAttribute("onclick", "consume('"+service_json.productId+"')");
        div.appendChild(btn2);

       // UIにボタンを追加    
       myNode.appendChild(div);                        
    }
    

}

// Inventory情報を取得
function get_inv(){
    if (mp != null){
        mp.query_inventory_async("my_service_1,my_service_2", show_inv, callback_alert);
    }else{
        console.log("appmart pluginが設定されておりません。");
    }
}

```

##### query_inventory_asyncのパラメータ

| n  |参考               |
|---|-------------------|
| 1  | 取得するサービスのID(文字列、[,]区切り)  | 
| 2  | 成功時callback (JS関数)| 
| 3  | 失敗時callback (JS関数)| 

##### inventoryのJSON情報

```javascript
{
    "purchases": [
        "{\"developerPayload\":\"xxx\",\"orderId\":\"xxxxxxxxxxx\",\"purchaseTime\":\"2015\\/05\\/24 07:14\",\"productId\":\"my_pruduct_id_1\"}",
        "{\"developerPayload\":\"yyy\",\"orderId\":\"xxxxxxxxxxx\",\"purchaseTime\":\"2015\\/05\\/24 08:23\",\"productId\":\"my_product_id_2\"}"
    ],
    "skuDetails": [
        "{\"title\":\"200コイン\",\"price\":\"￥99\",\"description\":\"200コインdescription\",\"price_currency_code\":\"JPY\",\"productId\":\"xxxxxx\"}",
        "{\"title\":\"100コイン\",\"price\":\"￥800\",\"description\":\"100コイン　description\",\"price_currency_code\":\"JPY\",\"productId\":\"xxxxxx\"}"
    ]
}
```

> [skuDetails]と[purchases]配列のデータがJSON形式の文字列となっておりますのでご注意ください。

#### サービスを購入

サービスを購入する際に、launch_purchase_flowを使ってください。


```javascript
//サービス購入処理
function purchase(sku_id, payload){
    if (mp != null){
        mp.launch_purchase_flow(sku_id,payload, callback_console, callback_console);
    }else{
        console.log("appmart pluginが設定されておりません。");
    }
}
```

| n |  型  |                 参考                 |
|---|------|--------------------------------------|
| 1 |文字列|         購入希望のサービスID         | 
| 2 |文字列|デベロッパーに追加される文字列 (任意) | 
| 3 | 関数 |            成功時callback            | 
| 4 | 関数 |            失敗時callback            | 

> サービスを購入した後にはInventoryを更新するためにもう一度query_inventory_asyncを実行してください。

#### サービス消費

Googleアプリ内課金V3同様に全てのサービスが管理されており、同じサービスを購入する前に必ず購入を消費しなければなりません。 過去購入されたサービスを消費するにはConsumeメソッドを使ってください。

> 継続決済の場合は「消費」は不可能になります！


```javascript
function consume(sku_id){
    if (mp != null){
        mp.consume_async(sku_id, callback_console, callback_console);
    }else{
        console.log("appmart pluginが設定されておりません。");
    }
}
```

| n  |型|参考  |
|-------|----|--------------|
| 1  |文字列| 消費するサービスのID| 
| 2 |関数 | 成功時callback| 
| 3  |関数 | 失敗時callback| 

> サービスを消費した後にはInventoryを更新するためにもう一度query_inventory_asyncを実行してください。