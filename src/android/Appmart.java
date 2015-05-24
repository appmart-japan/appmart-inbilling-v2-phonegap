package jp.app_mart.inapp;

import java.util.ArrayList;
import java.util.List;

import jp.app_mart.billing.v2.AppmartIabHelper;
import jp.app_mart.billing.v2.IabResult;
import jp.app_mart.billing.v2.Inventory;
import jp.app_mart.billing.v2.Purchase;
import jp.app_mart.billing.v2.SkuDetails;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class Appmart extends CordovaPlugin {

    public Context mContext;
    public AppmartIabHelper mHelper;
    public Inventory mInventory;
    public final String TAG = this.getClass().getSimpleName();

    //メソッド
    public static final String START_SETUP              = "start_setup";
    public static final String QUERY_INVENTORY_ASYNC    = "query_inventory_async";
    public static final String LAUNCH_PURCHASE_FLOW     = "launch_purchase_flow";
    public static final String CONSUME_ASYNC            = "consume_async"; 

    //パラメター名
    public static final String LICENSE_KEY              = "licenceKey";
    public static final String APP_ID                   = "appId";
    public static final String SERVICE_ID               = "serviceId";
    public static final String SKUS_LIST                = "skusList";
    public static final String PAYLOAD                  = "payload";
    
    //その他
    public static final String RETURN_PURCHASES         = "purchases";
    public static final String RETURN_SKUS              = "skuDetails";
    
    //Callback
    public CallbackContext mCallbackContextPurchase;
    public CallbackContext mCallbackContextConsume;

    /**
     * Constructor
     */
    public Appmart() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        mContext = cordova.getActivity().getApplicationContext();
        super.initialize(cordova, webView);
    }

    /**
     * plugin main method
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        
        JSONObject obj = new JSONObject(args.getString(0));
        
        if (action.equals(START_SETUP)) {   //設定
            
            // 決済情報を取得
            final String licenceKey   = obj.getString(LICENSE_KEY);
            final String appId        = obj.getString(APP_ID);

            //初期設定
            if(licenceKey == null || appId == null){
                callbackContext.error("引数が正しくありません");
            }else{              
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        Appmart.this.startSetup(callbackContext, appId, licenceKey);
                    }
                });                
            }
            return true;
            
        }else if (action.equals(QUERY_INVENTORY_ASYNC)){  // Inventory取得
            
            String skusIdJSON   = obj.getString(SKUS_LIST);
            final String [] skus = skusIdJSON.split(",");
            
            if(skus == null){
                callbackContext.error("引数が正しくありません");
            }else{
                this.queryInventoryAsync(callbackContext, skus);
            }
            return true;
            
        }else if (action.equals(LAUNCH_PURCHASE_FLOW)){ //サービス購入
            
            final String skuId    = obj.getString(SERVICE_ID);
            final String payload  = obj.getString(PAYLOAD);
            
            if(skuId == null || payload == null){
                callbackContext.error("引数が正しくありません");
            }else{
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        Appmart.this.launchPurchaseFlow(callbackContext, skuId, payload);
                    }
                });                
            }
            return true;
            
        } else if (action.equals(CONSUME_ASYNC)){   //サービス消費
            
            final String skuId = obj.getString(SERVICE_ID);
            
            if(skuId == null){
                callbackContext.error("引数が正しくありません");
            }else{
                Appmart.this.consumeAsync(callbackContext, skuId);
            }
            return true;
        }
        return false;
    }




    /**
     * plugin初期化
     * @param callbackContext　javascript側のcallback
     * @param applicationId アプリID
     * @param licenseKey    ライセンスキー
     */
    private void startSetup(final CallbackContext callbackContext, String applicationId, String licenseKey){ 
        mHelper= new AppmartIabHelper(this.mContext, applicationId, licenseKey);
        mHelper.startSetup(new AppmartIabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()){
                    callbackContext.error(result.getMessage());
                }else{
                    callbackContext.success("startSetupが成功しました。");
                }
            }
        });
    }
    

    /**
     * 
     * @param callbackContext javascript側のcallback
     * @param skus 取得したいSkus情報
     */
    private void queryInventoryAsync(final CallbackContext callbackContext, String[] skus){
        List<String> additionalSkuList = new ArrayList<String>();
        for (int i=0; i<skus.length;i++) additionalSkuList.add(skus[i]);
        
        // Inventory取得
        mHelper.queryInventoryAsync(additionalSkuList, new AppmartIabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inventory){
                if (result.isFailure()) {
                    callbackContext.error(result.getMessage());
                    return;
                }
    
                // Inventoryを保存
                mInventory =  inventory ;
                
                //inventoryをjson化し、return
                JSONObject inventoryJson = new JSONObject();
                
                // purchases + skus
                JSONArray purchasesJSON = new JSONArray();
                JSONArray SkusJSON = new JSONArray();
                
                try {   
                    
                    for(Purchase pur: inventory.getAllPurchases()){purchasesJSON.put(pur.getOriginalJson());}
                    inventoryJson.put(RETURN_PURCHASES, purchasesJSON);
                    
                    for(SkuDetails sku: inventory.getAllSkuDetails()){SkusJSON.put(sku.getOriginalJson());}
                    inventoryJson.put(RETURN_SKUS, SkusJSON);
                    
                    callbackContext.success(inventoryJson);
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                }
           }
        }
        );
    }
    
    /**
     * サービス消費
     * @param callbackContext : JS callback
     * @param sku: 消費するサービスのID
     */
    private void consumeAsync(final CallbackContext callbackContext, String sku){
        if(!mHelper.isAsyncInProgress()){
            if(mInventory.hasPurchase(sku)){
                mCallbackContextConsume = callbackContext ;
                Purchase p = mInventory.getPurchase(sku);
                mHelper.consumeAsync(p, onConsumeFinishedListener);
            }else{
                Log.d(this.getClass().getSimpleName(), "未消費の情報がございません。");
                callbackContext.error("未消費の情報がございません。");
            }
        }
    }
    
    //　サービス消費後 callback
    AppmartIabHelper.OnConsumeFinishedListener onConsumeFinishedListener = new AppmartIabHelper.OnConsumeFinishedListener(){
        @Override
        public void onConsumeFinished(Purchase purchase,IabResult result) {     
            if(result.isFailure()){
                mCallbackContextConsume.error("アイテムが消費されませんでした。");
                Log.d(this.getClass().getSimpleName(), "アイテムが消費されませんでした。");
            }else{
                Log.d(this.getClass().getSimpleName(), "アイテムが消費されました。");
                mCallbackContextConsume.success("アイテムが消費されました。");
            }
        }
    };
    
    /**
     * サービスを購入する (Async)
     * @param callbackContext : JS callback
     * @param skuId : 購入するサービスのID
     * @param payload ユーザーの文字列
     */
    private void launchPurchaseFlow(CallbackContext callbackContext, String skuId, String payload){
        if(!mHelper.isAsyncInProgress()){
            if(!mInventory.hasPurchase(skuId)){
                mCallbackContextPurchase = callbackContext;
                this.cordova.setActivityResultCallback(this);
                mHelper.launchPurchaseFlow(cordova.getActivity(), skuId, 10001, mPurchaseFinishedListener, payload);
            }else{
                mCallbackContextPurchase = null ;
                Log.d(TAG, "このサービスは既に購入済みになっております。消費してから、もう一度購入してください。");
            }
        }
    }
    
    
    // 決済完了後 callback
    AppmartIabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new AppmartIabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase){
           if (result.isFailure()) {
             Log.d(TAG, "購入エラー: " + result);
             mCallbackContextPurchase.error("決済失敗："+ result.getMessage());
             return;
          }          
          Log.d(TAG, "購入成功: " + purchase.getSku());
          mCallbackContextPurchase.success("決済成功");          
       }
    };  
       
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }else {
            Log.d(TAG, "onActivityResult handled by AppmartIabHelper.");
        }   
    }

}