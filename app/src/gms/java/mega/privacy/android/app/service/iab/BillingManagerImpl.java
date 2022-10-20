/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mega.privacy.android.app.service.iab;

import static com.android.billingclient.api.BillingFlowParams.ProrationMode.DEFERRED;
import static com.android.billingclient.api.BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logInfo;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.billing.PaymentUtils.getProductLevel;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.middlelayer.iab.BillingManager;
import mega.privacy.android.app.middlelayer.iab.BillingUpdatesListener;
import mega.privacy.android.app.middlelayer.iab.MegaPurchase;
import mega.privacy.android.app.middlelayer.iab.MegaSku;
import mega.privacy.android.app.middlelayer.iab.QuerySkuListCallback;
import mega.privacy.android.app.utils.billing.PaymentUtils;
import mega.privacy.android.app.utils.billing.Security;
import nz.mega.sdk.MegaApiJava;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManagerImpl implements PurchasesUpdatedListener, BillingManager {

    /** SKU for our subscription PRO_I monthly */
    public static final String SKU_PRO_I_MONTH = "mega.android.pro1.onemonth";

    /** SKU for our subscription PRO_I yearly */
    public static final String SKU_PRO_I_YEAR = "mega.android.pro1.oneyear";

    /** SKU for our subscription PRO_II monthly */
    public static final String SKU_PRO_II_MONTH = "mega.android.pro2.onemonth";

    /** SKU for our subscription PRO_II yearly */
    public static final String SKU_PRO_II_YEAR = "mega.android.pro2.oneyear";

    /** SKU for our subscription PRO_III monthly */
    public static final String SKU_PRO_III_MONTH = "mega.android.pro3.onemonth";

    /** SKU for our subscription PRO_III yearly */
    public static final String SKU_PRO_III_YEAR = "mega.android.pro3.oneyear";

    /** SKU for our subscription PRO_LITE monthly */
    public static final String SKU_PRO_LITE_MONTH = "mega.android.prolite.onemonth";

    /** SKU for our subscription PRO_LITE yearly */
    public static final String SKU_PRO_LITE_YEAR = "mega.android.prolite.oneyear";

    private static final String BASE64_ENCODED_PUBLIC_KEY_1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0bZjbgdGRd6/hw5/J2FGTkdG";
    private static final String BASE64_ENCODED_PUBLIC_KEY_2 = "tDTMdR78hXKmrxCyZUEvQlE/DJUR9a/2ZWOSOoaFfi9XTBSzxrJCIa+gjj5wkyIwIrzEi";
    private static final String BASE64_ENCODED_PUBLIC_KEY_3 = "55k9FIh3vDXXTHJn4oM9JwFwbcZf1zmVLyes5ld7+G15SZ7QmCchqfY4N/a/qVcGFsfwqm";
    private static final String BASE64_ENCODED_PUBLIC_KEY_4 = "RU3VzOUwAYHb4mV/frPctPIRlJbzwCXpe3/mrcsAP+k6ECcd19uIUCPibXhsTkNbAk8CRkZ";
    private static final String BASE64_ENCODED_PUBLIC_KEY_5 = "KOy+czuZWfjWYx3Mp7srueyQ7xF6/as6FWrED0BlvmhJYj0yhTOTOopAXhGNEk7cUSFxqP2FKYX8e3pHm/uNZvKcSrLXbLUhQnULhn4WmKOQIDAQAB";

    /** Public key for verify purchase. */
    private static final String PUBLIC_KEY = BASE64_ENCODED_PUBLIC_KEY_1 + BASE64_ENCODED_PUBLIC_KEY_2 + BASE64_ENCODED_PUBLIC_KEY_3 + BASE64_ENCODED_PUBLIC_KEY_4 + BASE64_ENCODED_PUBLIC_KEY_5;
    public static final int PAY_METHOD_RES_ID = R.string.payment_method_google_wallet;
    public static final int PAY_METHOD_ICON_RES_ID = R.drawable.ic_google_wallet;
    public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    public static final int PAYMENT_GATEWAY = MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET;

    private BillingClient mBillingClient;
    private boolean mIsServiceConnected;
    private final BillingUpdatesListener mBillingUpdatesListener;
    private final Activity mActivity;
    private final List<Purchase> mPurchases = new ArrayList<>();
    private List<ProductDetails> mProducts;
    private final String obfuscatedAccountId;

    /**
     * Handles all the interactions with Play Store (via Billing library), maintains connection to
     * it through BillingClient and caches temporary states/data if needed.
     *
     * @param activity        The Context, here's {@link mega.privacy.android.app.lollipop.ManagerActivityLollipop}
     * @param updatesListener The callback, when billing status update. {@link BillingUpdatesListener}
     */
    public BillingManagerImpl(Activity activity, BillingUpdatesListener updatesListener) {
        mActivity = activity;
        mBillingUpdatesListener = updatesListener;
        obfuscatedAccountId = ((MegaApplication) mActivity.getApplication()).getMyAccountInfo().generateObfuscatedAccountId();

        //must enable pending purchases to use billing library
        mBillingClient = BillingClient.newBuilder(mActivity).enablePendingPurchases().setListener(this).build();

        // Start setup. This is asynchronous and the specified listener will be called
        // once setup completes.
        // It also starts to report all the new purchases through onPurchasesUpdated() callback.
        startServiceConnection(() -> {
            logInfo("service connected, query purchases");
            // Notifying the listener that billing client is ready
            mBillingUpdatesListener.onBillingClientSetupFinished();
            // IAB is fully set up. Now, let's get an inventory of stuff we own.
            queryPurchases();
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        int resultCode = billingResult.getResponseCode();
        logDebug("Purchases updated, response code is " + resultCode);
        if (resultCode == BillingResponseCode.OK) {
            mPurchases.clear();
            handlePurchaseList(purchases);
        }
        mBillingUpdatesListener.onPurchasesUpdated(
                resultCode != BillingResponseCode.OK,
                resultCode,
                Converter.convertPurchases(mPurchases));
    }

    @Override
    public boolean isPurchased(MegaPurchase purchase) {
        return purchase.getState() == Purchase.PurchaseState.PURCHASED;
    }

    @Override
    public void initiatePurchaseFlow(@Nullable String oldSku, @Nullable String purchaseToken, @NonNull MegaSku skuDetails) {
        logDebug("oldSku is:" + oldSku + ", new sku is:" + skuDetails);
        logDebug("Obfuscated account id is:" + obfuscatedAccountId);

        //if user is upgrading, it take effect immediately otherwise wait until current plan expired
        final int prorationMode = getProductLevel(skuDetails.getSku()) > getProductLevel(oldSku) ? IMMEDIATE_WITH_TIME_PRORATION : DEFERRED;
        logDebug("prorationMode is " + prorationMode);
        ProductDetails productDetails = getProductDetails(skuDetails);
        if (productDetails != null) {
            List<ProductDetails.SubscriptionOfferDetails> offerDetailsList =
                    productDetails.getSubscriptionOfferDetails();
            if (offerDetailsList != null) {
                Runnable purchaseFlowRequest = () -> {
                    String offerToken = productDetails.getSubscriptionOfferDetails()
                            .get(0)
                            .getOfferToken();

                    ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParams =
                            ImmutableList.of(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails)
                                            .setOfferToken(offerToken)
                                            .build()
                            );


                    BillingFlowParams.Builder purchaseParamsBuilder = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParams)
                            .setObfuscatedAccountId(obfuscatedAccountId);

                    // setSubscriptionUpdateParams asks that have to include the old sku information,
                    // otherwise throw an exception
                    if (oldSku != null && purchaseToken != null) {
                        BillingFlowParams.SubscriptionUpdateParams.Builder builder =
                                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                        .setReplaceProrationMode(prorationMode)
                                        .setOldPurchaseToken(purchaseToken);
                        purchaseParamsBuilder.setSubscriptionUpdateParams(builder.build());
                    }

                    /*
                        If do a full login, ManagerActivity's mIntent will be set as null.
                        Work around, check the intent's nullity first, if null, set an empty Intent, as we don't use "PROXY_PACKAGE",
                        otherwise billing library crashes internally.
                        @see com.android.billingclient.api.BillingClientImpl -> var1.getIntent().getStringExtra("PROXY_PACKAGE")
                    */
                    if (mActivity.getIntent() == null) {
                        mActivity.setIntent(new Intent());
                    }

                    mBillingClient.launchBillingFlow(mActivity, purchaseParamsBuilder.build());
                };
                executeServiceRequest(purchaseFlowRequest);
            }
        }
    }

    private ProductDetails getProductDetails(MegaSku skuDetails) {
        if (mProducts == null || mProducts.isEmpty()) {
            logWarning("Haven't init products!");
            return null;
        }
        for (ProductDetails details : mProducts) {
            if (details.getProductId().equals(skuDetails.getSku())) {
                return details;
            }
        }
        logWarning("Can't find sku with id: " + skuDetails.getSku());
        return null;
    }

    @Override
    public void destroy() {
        logInfo("on destroy");
        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }

    @Override
    public int getPurchaseResult(Intent data) {
        // Unused for GMS
        return -1;
    }

    /**
     * Unused for GMS
     */
    @Override
    public void updatePurchase() {

    }

    /**
     * Query all the available skus of MEGA in Play Store.
     *
     * @param listener Callback when query available product finished.
     * @see PaymentUtils
     */
    private void queryProductDetailsAsync(ProductDetailsResponseListener listener) {
        logDebug("querySkuDetailsAsync type is " + BillingClient.ProductType.SUBS);
        // Creating a runnable from the request to use it inside our connection retry policy below
        Runnable queryRequest = () -> {
            // Query the purchase async
            List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
            for (String sku : BillingManager.IN_APP_SKUS) {
                productList.add(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(sku)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                );
            }
            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build();

            mBillingClient.queryProductDetailsAsync(
                    params,
                    listener
            );
        };

        executeServiceRequest(queryRequest);
    }

    @Override
    public void getInventory(QuerySkuListCallback callback) {
        ProductDetailsResponseListener listener = (result, productDetailsList) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                logWarning("Failed to get SkuDetails, error code is " + result.getResponseCode());
            }
            if (productDetailsList.size() > 0) {
                mProducts = productDetailsList;
                callback.onSuccess(Converter.convertSkus(productDetailsList));
            }
        };
        //we only support subscription for google pay
        queryProductDetailsAsync(listener);
    }

    private void handlePurchaseList(List<Purchase> purchases) {
        if (purchases == null) {
            return;
        }
        for (Purchase purchase : purchases) {
            handlePurchase(purchase);
        }
        logDebug("total purchased are: " + mPurchases.size());
    }

    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * See {@link Security#verifyPurchase(String, String, String)}
     * </p>
     *
     * @param purchase Purchase to be handled
     */
    private void handlePurchase(Purchase purchase) {
        if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            logWarning("Invalid purchase found");
            return;
        }

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = billingResult -> {
                    if (billingResult.getResponseCode() == BillingResponseCode.OK) {
                        logInfo("purchase acknowledged");
                    } else {
                        logWarning("purchase acknowledge failed, " + billingResult.getDebugMessage());
                    }
                };
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
        }

        logDebug("new purchase added, " + purchase.getOriginalJson());
        mPurchases.add(purchase);
    }

    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     *
     * @param responseCode Purchase result response code
     * @param purchaseList Purchase list
     */
    private void onQueryPurchasesFinished(int responseCode, List<Purchase> purchaseList) {
        logDebug("onQueryPurchasesFinished, succeed? " + (responseCode == BillingResponseCode.OK));
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (mBillingClient == null || responseCode != BillingResponseCode.OK) {
            return;
        }

        // Update the UI and purchases inventory with new list of purchases
        mPurchases.clear();

        logDebug("Purchases updated, response code is " + responseCode);
        handlePurchaseList(purchaseList);
        mBillingUpdatesListener.onQueryPurchasesFinished(
                false,
                responseCode,
                Converter.convertPurchases(mPurchases));
    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    private boolean areSubscriptionsSupported() {
        int responseCode = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS).getResponseCode();
        logDebug("areSubscriptionsSupported " + (responseCode == BillingResponseCode.OK));
        return responseCode == BillingResponseCode.OK;
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    @Override
    public void queryPurchases() {
        Runnable queryToExecute = () -> mBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().
                        setProductType(BillingClient.ProductType.INAPP).build(),
                (resultINAPP, listINAPP) -> {
                    if (areSubscriptionsSupported()) {
                        mBillingClient.queryPurchasesAsync(
                                QueryPurchasesParams.newBuilder()
                                        .setProductType(BillingClient.ProductType.SUBS).build(),
                                (resultSUBS, listSUBS) -> {
                                    listINAPP.addAll(listSUBS);
                                    queryPurchasesFinished(resultINAPP, listINAPP);
                                }
                        );
                    } else {
                        queryPurchasesFinished(resultINAPP, listINAPP);
                    }
                }
        );

        executeServiceRequest(queryToExecute);
    }

    private void queryPurchasesFinished(BillingResult billingResult, List<Purchase> purchaseList) {
        // Verify all available purchases
        List<Purchase> list = new ArrayList<>();
        for (Purchase purchase : purchaseList) {
            if (purchase != null && verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())
                    && purchase.getAccountIdentifiers() != null
                    && purchase.getAccountIdentifiers().getObfuscatedAccountId() != null
                    && purchase.getAccountIdentifiers().getObfuscatedAccountId().equals(obfuscatedAccountId)) {
                list.add(purchase);
                logDebug("Purchase added, " + purchase.getOriginalJson());
            }
        }

        logDebug("Final purchase result is " + billingResult);
        onQueryPurchasesFinished(billingResult.getResponseCode(), list);
    }

    /**
     * Connect to Google billing library service.
     *
     * @param executeOnSuccess Once billing client setup finished, the runnable will be executed.
     */
    private void startServiceConnection(final Runnable executeOnSuccess) {
        mBillingClient.startConnection(new BillingClientStateListener() {

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                logDebug("Response code is: " + billingResult.getResponseCode());
                int BillingResponseCodeCode = billingResult.getResponseCode();

                if (BillingResponseCodeCode == BillingResponseCode.OK) {
                    mIsServiceConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                logInfo("billing service disconnected");
                mIsServiceConnected = false;
            }
        });
    }

    /***
     * Request to server, if hasn't connected to service, start to connect first, otherwise, execute directly.
     *
     * @param runnable The request will be executed.
     */
    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            logInfo("billing service was disconnected, retry once");
            startServiceConnection(runnable);
        }
    }

    /**
     * Verify signature of purchase.
     *
     * @param signedData the content of a purchase.
     * @param signature  the signature of a purchase.
     * @return If the purchase is valid.
     */
    @Override
    public boolean verifyValidSignature(String signedData, String signature) {
        try {
            return Security.verifyPurchase(signedData, signature, PUBLIC_KEY);
        } catch (IOException e) {
            logWarning("Purchase failed to valid signature", e);
            return false;
        }
    }

    /**
     * Converter for converting platform dependent objects to generic MEGA objects.
     */
    private static class Converter {

        /**
         * Convert Purchase object in GMS into generic MegaPurchase object.
         *
         * @param purchase Purchase object.
         * @return Generic MegaPurchase object.
         */
        public static MegaPurchase convert(Purchase purchase) {
            MegaPurchase p = new MegaPurchase();
            p.setSku(purchase.getProducts().get(0));
            p.setReceipt(purchase.getOriginalJson());
            p.setState(purchase.getPurchaseState());
            p.setToken(purchase.getPurchaseToken());
            return p;
        }

        /**
         * Convert Purchase objects in a list into generic MegaPurchase objects list.
         *
         * @param purchases Purchase objects list.
         * @return Generic MegaPurchase objects list.
         */
        public static List<MegaPurchase> convertPurchases(@Nullable List<Purchase> purchases) {
            if (purchases == null) {
                return null;
            }
            List<MegaPurchase> result = new ArrayList<>(purchases.size());
            for (Purchase purchase : purchases) {
                result.add(convert(purchase));
            }
            return result;
        }

        /**
         * Convert SkuDetails object in GMS into generic MegaSku object.
         *
         * @param product ProductDetails object.
         * @return Generic MegaSku object.
         */
        public static MegaSku convert(ProductDetails product) {
            List<ProductDetails.SubscriptionOfferDetails> offerDetailsList =
                    product.getSubscriptionOfferDetails();
            if (offerDetailsList != null && !offerDetailsList.isEmpty()) {
                ProductDetails.SubscriptionOfferDetails offerDetails =
                        product.getSubscriptionOfferDetails().get(0);
                if (offerDetails != null) {
                    ProductDetails.PricingPhase pricingPhase =
                            offerDetails.getPricingPhases().getPricingPhaseList().get(0);
                    return new MegaSku(product.getProductId(),
                            pricingPhase.getPriceAmountMicros(),
                            pricingPhase.getPriceCurrencyCode());
                }
            }

            return null;
        }

        /**
         * Convert SkuDetails objects in a list into generic MegaSku objects list.
         *
         * @param products ProductDetails objects list.
         * @return Generic MegaSku objects list.
         */
        public static List<MegaSku> convertSkus(@Nullable List<ProductDetails> products) {
            if (products == null) {
                return null;
            }
            List<MegaSku> result = new ArrayList<>(products.size());
            for (ProductDetails product : products) {
                result.add(convert(product));
            }
            return result;
        }
    }
}

