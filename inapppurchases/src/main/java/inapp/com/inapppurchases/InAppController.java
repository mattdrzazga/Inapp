package inapp.com.inapppurchases;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.android.vending.billing.IInAppBillingService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static inapp.com.inapppurchases.utils.Objects.requireNonNull;

public class InAppController {
    public static final int REQUEST_PURCHASE_SKU = 32412;

    public static final int RESPONSE_OK = 0;
    public static final int RESPONSE_USER_CANCELED = 1;
    public static final int RESPONSE_BILLING_UNAVAILABLE = 3;
    public static final int RESPONSE_ITEM_UNAVAILABLE = 4;
    public static final int RESPONSE_DEVELOPER_ERROR = 5;
    public static final int RESPONSE_ERROR = 6;
    public static final int RESPONSE_ITEM_ALREADY_OWNED = 7;
    public static final int RESPONSE_ITEM_NOT_OWNED = 8;

    private static final String TAG = "InAppController";
    private final String mPackageName;
    private AppCompatActivity mContext;
    private InAppCallback mCallback;

    private IInAppBillingService mService;
    @NonNull private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            Log.d(TAG, "onServiceConnected: ");
            mCallback.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d(TAG, "onServiceDisconnected: ");
            mCallback.onServiceDisconnected();
        }
    };

    public InAppController(@NonNull AppCompatActivity context, @NonNull InAppCallback inAppCallback) {
        mContext = requireNonNull(context);
        mCallback = requireNonNull(inAppCallback);
        mPackageName = context.getPackageName();
    }

    public void onCreate() {
        bindService(mContext, mServiceConnection);
    }

    public static void bindService(@NonNull Context context, @NonNull ServiceConnection connection) {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        requireNonNull(context).bindService(serviceIntent, requireNonNull(connection), Context.BIND_AUTO_CREATE);
    }

    public void onDestroy() {
        if (mService != null) {
            mContext.unbindService(mServiceConnection);
        }
        mContext = null;
        mCallback = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE_SKU) {
            final int responseCode = data.getIntExtra(InAppConstants.RESPONSE_CODE, 0);
            final String purchaseData = data.getStringExtra(InAppConstants.INAPP_PURCHASE_DATA);

            if (resultCode == Activity.RESULT_OK) {
                final JsonObject object = new JsonParser().parse(purchaseData).getAsJsonObject();
                final String sku = object.get(InAppConstants.PRODUCT_ID).getAsString();
                if (responseCode == RESPONSE_OK) {
                    mCallback.onPurchaseSuccess(sku);
                }
                else {
                    mCallback.onPurchaseFail(sku);
                }
            }
        }
    }

    public void purchaseSku(@NonNull String sku) {
        new Thread(() -> {
            try {
                final Bundle
                        buyIntentBundle = mService.getBuyIntent(3, mPackageName, sku, InAppConstants.ITEM_TYPE, "");
                final PendingIntent pendingIntent = buyIntentBundle.getParcelable(InAppConstants.BUY_INTENT);
                if (pendingIntent != null) {
                    int flagsMask = 0;
                    int flagsValues = 0;
                    int extraFlags = 0;
                    mContext.startIntentSenderForResult(pendingIntent.getIntentSender(), REQUEST_PURCHASE_SKU, new Intent(), flagsMask, flagsValues, extraFlags);
                }
            } catch (RemoteException | IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void queryPurchases() {
        try {
            final Bundle purchasedSkus = mService.getPurchases(InAppConstants.API_VERSION, mPackageName, InAppConstants.ITEM_TYPE, null);
            final int responseCode = purchasedSkus.getInt(InAppConstants.RESPONSE_CODE);

            if (responseCode == RESPONSE_OK) {
                final ArrayList<String> purchasedItemIds = purchasedSkus.getStringArrayList(InAppConstants.INAPP_PURCHASE_ITEM_LIST);
                // Will never be null if RESPONSE_OK, but check for lint's sanity
                if (purchasedItemIds != null) {
                    mCallback.purchasedSkus(purchasedItemIds);
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void querySkuDetails(@NonNull String ...skus) {
        final Bundle querySkusBundle = new Bundle();
        final ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, skus);
        querySkusBundle.putStringArrayList(InAppConstants.ITEM_ID_LIST, list);

        try {
            final Bundle skuDetails = mService.getSkuDetails(InAppConstants.API_VERSION, mPackageName, InAppConstants.ITEM_TYPE, querySkusBundle);
            final int responseCode = skuDetails.getInt(InAppConstants.RESPONSE_CODE);

            if (responseCode == RESPONSE_OK) {
                final Gson gson = new Gson();
                final ArrayList<InApp> inApps = new ArrayList<>();
                final ArrayList<String> responseList = skuDetails.getStringArrayList(InAppConstants.DETAILS_LIST);
                // Will never be null if RESPONSE_OK, but check for lint's sanity
                if (responseList != null) {
                    for (String response : responseList) {
                        final JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
                        inApps.add(gson.fromJson(jsonObject, InApp.class));
                    }
                    mCallback.skuDetails(inApps);
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public interface InAppCallback {
        void onServiceConnected();
        default void onServiceDisconnected() {}

        void purchasedSkus(@NonNull List<String> purchasedItems);
        void skuDetails(@NonNull List<InApp> details);
        void onPurchaseSuccess(@NonNull String sku);
        void onPurchaseFail(@NonNull String sku);
    }
}

