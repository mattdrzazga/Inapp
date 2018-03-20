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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.android.vending.billing.IInAppBillingService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

    @IntDef({
            RESPONSE_OK, RESPONSE_USER_CANCELED, RESPONSE_BILLING_UNAVAILABLE,
            RESPONSE_ITEM_UNAVAILABLE, RESPONSE_DEVELOPER_ERROR, RESPONSE_ERROR,
            RESPONSE_ITEM_ALREADY_OWNED, RESPONSE_ITEM_NOT_OWNED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResponseCode {}

    private static final String TAG = "InAppController";
    private final String packageName;
    private AppCompatActivity context;
    private ConnectionCallback connectionCallback;
    private InAppCallback inAppCallback;

    private IInAppBillingService service;
    @NonNull private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            InAppController.this.service = IInAppBillingService.Stub.asInterface(service);
            Log.d(TAG, "onServiceConnected: ");
            connectionCallback.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            Log.d(TAG, "onServiceDisconnected: ");
            connectionCallback.onServiceDisconnected();
        }
    };

    public InAppController(@NonNull AppCompatActivity context, @NonNull ConnectionCallback connectionCallback, @NonNull InAppCallback inAppCallback) {
        this.context = requireNonNull(context);
        this.connectionCallback = requireNonNull(connectionCallback);
        this.inAppCallback = requireNonNull(inAppCallback);
        packageName = context.getPackageName();
    }

    public void connect() {
        bindService(context, serviceConnection);
    }

    public static void bindService(@NonNull Context context, @NonNull ServiceConnection connection) {
        final Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        requireNonNull(context).bindService(serviceIntent, requireNonNull(connection), Context.BIND_AUTO_CREATE);
    }

    public void release() {
        if (service != null) {
            context.unbindService(serviceConnection);
        }
        context = null;
        inAppCallback = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE_SKU) {
            final int responseCode = data.getIntExtra(InAppConstants.RESPONSE_CODE, 0);
            final String purchaseData = data.getStringExtra(InAppConstants.INAPP_PURCHASE_DATA);

            if (resultCode == Activity.RESULT_OK) {
                final JsonObject object = new JsonParser().parse(purchaseData).getAsJsonObject();
                final String sku = object.get(InAppConstants.PRODUCT_ID).getAsString();
                if (responseCode == RESPONSE_OK) {
                    inAppCallback.onPurchaseSuccess(sku);
                }
                else {
                    inAppCallback.onPurchaseFail(sku, responseCode);
                }
            }
        }
    }

    public void purchaseSku(@NonNull String sku) {
        new Thread(() -> {
            try {
                final Bundle
                        buyIntentBundle = service.getBuyIntent(3, packageName, sku, InAppConstants.ITEM_TYPE_INAPP, "");
                final PendingIntent pendingIntent = buyIntentBundle.getParcelable(InAppConstants.BUY_INTENT);
                if (pendingIntent != null) {
                    int flagsMask = 0;
                    int flagsValues = 0;
                    int extraFlags = 0;
                    context.startIntentSenderForResult(pendingIntent.getIntentSender(), REQUEST_PURCHASE_SKU, new Intent(), flagsMask, flagsValues, extraFlags);
                }
            } catch (RemoteException | IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void queryPurchases() {
        try {
            final Bundle purchasedSkus = service.getPurchases(InAppConstants.API_VERSION,
                    packageName, InAppConstants.ITEM_TYPE_INAPP, null);
            final int responseCode = purchasedSkus.getInt(InAppConstants.RESPONSE_CODE);

            if (responseCode == RESPONSE_OK) {
                final List<String> purchasedItemIds = unpackList(purchasedSkus, InAppConstants.INAPP_PURCHASE_ITEM_LIST);
                inAppCallback.purchasedSkus(purchasedItemIds);
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
            final Bundle skuDetails = service.getSkuDetails(InAppConstants.API_VERSION,
                    packageName, InAppConstants.ITEM_TYPE_INAPP, querySkusBundle);
            final int responseCode = skuDetails.getInt(InAppConstants.RESPONSE_CODE);

            if (responseCode == RESPONSE_OK) {
                final Gson gson = new Gson();
                final List<InApp> inApps = new ArrayList<>();
                final List<String> responseList = unpackList(skuDetails, InAppConstants.DETAILS_LIST);
                final JsonParser jsonParser = new JsonParser();
                for (String response : responseList) {
                    final JsonObject jsonObject = jsonParser.parse(response).getAsJsonObject();
                    inApps.add(gson.fromJson(jsonObject, InApp.class));
                }
                inAppCallback.skuDetails(inApps);
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static @NonNull List<String> unpackList(@NonNull Bundle bundle, String key) {
        final List<String> list = requireNonNull(bundle).getStringArrayList(key);
        if (list != null) {
            return list;
        }
        else {
            return new ArrayList<>();
        }
    }

    public interface ConnectionCallback {
        void onServiceConnected();
        default void onServiceDisconnected() {}
    }

    public interface InAppCallback {
        void purchasedSkus(@NonNull List<String> purchasedItems);
        void skuDetails(@NonNull List<InApp> details);
        void onPurchaseSuccess(@NonNull String sku);
        void onPurchaseFail(@NonNull String sku, @ResponseCode int code);
    }
}

