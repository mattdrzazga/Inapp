package inapp.com.inapppurchases;

import android.support.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

public class InApp {
    @NonNull
    @SerializedName("productId")
    public final String sku;

    @NonNull public final String price;
    @NonNull public final String description;

    @NonNull
    @SerializedName("price_currency_code")
    public final String currencyCode;

    @NonNull public final String title;

    @SerializedName("price_amount_micros")
    public final long priceAmountMicros;

    public InApp(@NonNull String sku, @NonNull String price, @NonNull String description,
            @NonNull String currencyCode, @NonNull String title, long priceAmountMicros) {
        this.sku = sku;
        this.price = price;
        this.description = description;
        this.currencyCode = currencyCode;
        this.title = title;
        this.priceAmountMicros = priceAmountMicros;
    }

    public float getRealPriceAmountMicros() {
        return priceAmountMicros / 1000000f;
    }
}