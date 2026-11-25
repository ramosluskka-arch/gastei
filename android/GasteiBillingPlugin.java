package com.gastei.app;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import android.os.Build;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Compras do Gastify Premium pelo Google Play.
 * Produtos que precisam existir no Play Console:
 *   - assinatura "gastei_premium" com os planos básicos "mensal" e "anual"
 *   - produto único "gastei_vitalicio"
 */
@CapacitorPlugin(name = "GasteiBilling")
public class GasteiBillingPlugin extends Plugin implements PurchasesUpdatedListener {
  static final String SUB = "gastei_premium";
  static final String LIFE = "gastei_vitalicio";
  private BillingClient client;
  private PluginCall pendingBuy;
  private final Map<String, ProductDetails> details = new HashMap<>();

  @Override
  public void load() {
    client = BillingClient.newBuilder(getContext())
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build();
  }

  private void ready(final PluginCall call, final Runnable ok) {
    if (client.isReady()) {
      ok.run();
      return;
    }
    client.startConnection(new BillingClientStateListener() {
      @Override
      public void onBillingSetupFinished(BillingResult r) {
        if (r.getResponseCode() == BillingClient.BillingResponseCode.OK) ok.run();
        else call.reject("indisponivel", String.valueOf(r.getResponseCode()));
      }

      @Override
      public void onBillingServiceDisconnected() {}
    });
  }

  private QueryProductDetailsParams params(String id, String type) {
    List<QueryProductDetailsParams.Product> list = new ArrayList<>();
    list.add(QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build());
    return QueryProductDetailsParams.newBuilder().setProductList(list).build();
  }

  @PluginMethod
  public void products(final PluginCall call) {
    ready(call, () -> client.queryProductDetailsAsync(params(SUB, BillingClient.ProductType.SUBS), (r1, res1) -> {
      final List<ProductDetails> subs = res1.getProductDetailsList();
      client.queryProductDetailsAsync(params(LIFE, BillingClient.ProductType.INAPP), (r2, res2) -> {
        JSArray plans = new JSArray();
        for (ProductDetails pd : subs) {
          details.put(pd.getProductId(), pd);
          List<ProductDetails.SubscriptionOfferDetails> offers = pd.getSubscriptionOfferDetails();
          if (offers == null) continue;
          for (ProductDetails.SubscriptionOfferDetails o : offers) {
            List<ProductDetails.PricingPhase> ph = o.getPricingPhases().getPricingPhaseList();
            if (ph.isEmpty()) continue;
            JSObject p = new JSObject();
            p.put("id", pd.getProductId());
            p.put("plan", o.getBasePlanId());
            p.put("offer", o.getOfferId());
            p.put("token", o.getOfferToken());
            p.put("price", ph.get(ph.size() - 1).getFormattedPrice());
            p.put("trial", ph.size() > 1 && ph.get(0).getPriceAmountMicros() == 0);
            plans.put(p);
          }
        }
        for (ProductDetails pd : res2.getProductDetailsList()) {
          details.put(pd.getProductId(), pd);
          ProductDetails.OneTimePurchaseOfferDetails o = pd.getOneTimePurchaseOfferDetails();
          if (o == null) continue;
          JSObject p = new JSObject();
          p.put("id", pd.getProductId());
          p.put("plan", "vitalicio");
          p.put("token", "");
          p.put("price", o.getFormattedPrice());
          plans.put(p);
        }
        JSObject out = new JSObject();
        out.put("plans", plans);
        call.resolve(out);
      });
    }));
  }

  @PluginMethod
  public void buy(final PluginCall call) {
    final String id = call.getString("id", "");
    final String token = call.getString("token", "");
    ready(call, () -> {
      ProductDetails pd = details.get(id);
      if (pd == null) {
        call.reject("produto");
        return;
      }
      BillingFlowParams.ProductDetailsParams.Builder b = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd);
      if (token != null && !token.isEmpty()) b.setOfferToken(token);
      final BillingFlowParams flow = BillingFlowParams.newBuilder()
          .setProductDetailsParamsList(Collections.singletonList(b.build()))
          .build();
      pendingBuy = call;
      getActivity().runOnUiThread(() -> {
        BillingResult r = client.launchBillingFlow(getActivity(), flow);
        if (r.getResponseCode() != BillingClient.BillingResponseCode.OK && pendingBuy == call) {
          pendingBuy = null;
          call.reject("falhou", String.valueOf(r.getResponseCode()));
        }
      });
    });
  }

  @Override
  public void onPurchasesUpdated(BillingResult r, List<Purchase> list) {
    PluginCall c = pendingBuy;
    pendingBuy = null;
    if (r.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
      JSArray a = handle(list);
      if (c != null) {
        JSObject o = new JSObject();
        o.put("purchases", a);
        c.resolve(o);
      }
    } else if (c != null) {
      if (r.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) c.reject("cancelado", "cancel");
      else c.reject("falhou", String.valueOf(r.getResponseCode()));
    }
  }

  private JSArray handle(List<Purchase> list) {
    JSArray a = new JSArray();
    for (Purchase p : list) {
      JSObject o = new JSObject();
      JSArray ids = new JSArray();
      for (String id : p.getProducts()) ids.put(id);
      o.put("products", ids);
      o.put("state", p.getPurchaseState());
      o.put("time", p.getPurchaseTime());
      a.put(o);
      if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged()) {
        client.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(),
            br -> {});
      }
    }
    return a;
  }

  @PluginMethod
  public void source(final PluginCall call) {
    String s = null;
    try {
      String pkg = getContext().getPackageName();
      if (Build.VERSION.SDK_INT >= 30) s = getContext().getPackageManager().getInstallSourceInfo(pkg).getInstallingPackageName();
      else s = getContext().getPackageManager().getInstallerPackageName(pkg);
    } catch (Exception e) {
      s = null;
    }
    JSObject o = new JSObject();
    o.put("installer", s == null ? "" : s);
    call.resolve(o);
  }

  @PluginMethod
  public void restore(final PluginCall call) {
    ready(call, () -> client.queryPurchasesAsync(
        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        (r1, l1) -> client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            (r2, l2) -> {
              List<Purchase> all = new ArrayList<>(l1);
              all.addAll(l2);
              JSObject o = new JSObject();
              o.put("ok", r1.getResponseCode() == BillingClient.BillingResponseCode.OK
                  && r2.getResponseCode() == BillingClient.BillingResponseCode.OK);
              o.put("purchases", handle(all));
              call.resolve(o);
            })));
  }
}
