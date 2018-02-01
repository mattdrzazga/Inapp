package inapp.com.inapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import inapp.com.inapppurchases.InApp;
import inapp.com.inapppurchases.InAppController;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TEST_SKU = "god_mode";
    private static final String TAG = "MainActivity";

    private InAppController controller;

    private InAppController.InAppCallback inAppCallback = new InAppController.InAppCallback() {
        @Override public void onServiceConnected() {
            updatePurchaseInfo();
        }

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

        @Override public void onPurchaseFail(@NonNull String sku) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        controller = new InAppController(this, inAppCallback);
        controller.onCreate();

        findViewById(R.id.buyButton).setOnClickListener(v -> purchaseInapp());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controller.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        controller.onActivityResult(requestCode, resultCode, data);
    }

    private void updatePurchaseInfo() {
        controller.queryPurchases();
        controller.querySkuDetails(TEST_SKU);
    }

    private void purchaseInapp() {
        controller.purchaseSku(TEST_SKU);
    }
}
