This library does everything you would have to to by hand if NOT using this library, so you can focus only on callbacks ;)

Usage
======
Declare your callbacks
````
private InAppController controller;
    
private InAppController.ConnectionCallback connectionCallback = () -> {
    controller.queryPurchases();
    controller.querySkuDetails(TEST_SKU);
};
    
private InAppController.InAppCallback inAppCallback = new InAppController.InAppCallback() {
    @Override public void purchasedSkus(@NonNull List<String> purchasedItems) {
        for (String sku : purchasedItems) {
            Log.d(TAG, "purchasedSkus: " + sku);
        }
    }

    @Override public void skuDetails(@NonNull List<InApp> details) {
        for (InApp inApp : details) {
            Log.d(TAG, "skuDetails: " + inApp.price);
        }
    }

    @Override public void onPurchaseSuccess(@NonNull String sku) {
        Log.d(TAG, "onPurchaseSuccess: " + sku);
    }

    @Override public void onPurchaseFail(@NonNull String sku, int code) {

    }
};
````

Connect with the InAppBillingService
````
controller = new InAppController(this, connectionCallback, inAppCallback);
controller.connect();

findViewById(R.id.buyButton).setOnClickListener(v -> controller.purchaseSku(TEST_SKU));
````
Disconnect when you're done
````
@Override
protected void onDestroy() {
    super.onDestroy();
    controller.release();
}
````
When purchasing an item, pass ````onActivityResult```` to the controller
````
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    controller.onActivityResult(requestCode, resultCode, data);
}
````
