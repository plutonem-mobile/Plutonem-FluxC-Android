package com.plutonem.android.fluxc.network.rest.plutonem.buyer;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.plutonem.android.fluxc.Dispatcher;
import com.plutonem.android.fluxc.generated.BuyerActionBuilder;
import com.plutonem.android.fluxc.generated.endpoint.PLUTONEMREST;
import com.plutonem.android.fluxc.model.BuyerModel;
import com.plutonem.android.fluxc.model.BuyersModel;
import com.plutonem.android.fluxc.network.BaseRequest.BaseNetworkError;
import com.plutonem.android.fluxc.network.BaseRequest.GenericErrorType;
import com.plutonem.android.fluxc.network.UserAgent;
import com.plutonem.android.fluxc.network.rest.plutonem.BasePlutonemRestClient;
import com.plutonem.android.fluxc.network.rest.plutonem.PlutonemGsonRequest;
import com.plutonem.android.fluxc.network.rest.plutonem.PlutonemGsonRequest.PlutonemErrorListener;
import com.plutonem.android.fluxc.network.rest.plutonem.PlutonemGsonRequest.PlutonemGsonNetworkError;
import com.plutonem.android.fluxc.network.rest.plutonem.auth.AccessToken;
import com.plutonem.android.fluxc.network.rest.plutonem.buyer.BuyerPNRestResponse.BuyersResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuyerRestClient extends BasePlutonemRestClient {
    private static final String BUYER_FIELDS = "ID,name,description,visible,icon";

    public BuyerRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                           UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchBuyers() {
        Map<String, String> params = new HashMap<>();
        params.put("fields", BUYER_FIELDS);
        String url = PLUTONEMREST.me.buyers.getUrlV1_1();
        final PlutonemGsonRequest<BuyersResponse> request = PlutonemGsonRequest.buildGetRequest(url, params,
                BuyersResponse.class,
                new Listener<BuyersResponse>() {
                    @Override
                    public void onResponse(BuyersResponse response) {
                        if (response != null) {
                            List<BuyerModel> buyerArray = new ArrayList<>();

                            for (BuyerPNRestResponse buyerResponse : response.buyers) {
                                buyerArray.add(buyerResponseToBuyerModel(buyerResponse));
                            }
                            mDispatcher.dispatch(BuyerActionBuilder.newFetchedBuyersAction(new BuyersModel(buyerArray)));
                        } else {
                            AppLog.e(T.API, "Received empty response to /me/buyers/");
                            BuyersModel payload = new BuyersModel(Collections.<BuyerModel>emptyList());
                            payload.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(BuyerActionBuilder.newFetchedBuyersAction(payload));
                        }
                    }
                },
                new PlutonemErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull PlutonemGsonNetworkError error) {
                        BuyersModel payload = new BuyersModel(Collections.<BuyerModel>emptyList());
                        payload.error = error;
                        mDispatcher.dispatch(BuyerActionBuilder.newFetchedBuyersAction(payload));
                    }
                }
        );
        add(request);
    }

    public void fetchBuyer(final BuyerModel buyer) {
        Map<String, String> params = new HashMap<>();
        params.put("fields", BUYER_FIELDS);
        String url = PLUTONEMREST.buyers.getUrlV1_1() + buyer.getBuyerId();
        final PlutonemGsonRequest<BuyerPNRestResponse> request = PlutonemGsonRequest.buildGetRequest(url, params,
                BuyerPNRestResponse.class,
                new Listener<BuyerPNRestResponse>() {
                    @Override
                    public void onResponse(BuyerPNRestResponse response) {
                        if (response != null) {
                            BuyerModel newBuyer = buyerResponseToBuyerModel(response);
                            // local ID is not copied into the new model, let's make sure it is
                            // otherwise the call that updates the DB can add a new row?
                            if (buyer.getId() > 0) {
                                newBuyer.setId(buyer.getId());
                            }
                            mDispatcher.dispatch(BuyerActionBuilder.newUpdateBuyerAction(newBuyer));
                        } else {
                            AppLog.e(T.API, "Received empty response to /buyers/$buyer/ for " + buyer.getBuyerId());
                            BuyerModel payload = new BuyerModel();
                            payload.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(BuyerActionBuilder.newUpdateBuyerAction(payload));
                        }
                    }
                },
                new PlutonemErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull PlutonemGsonNetworkError error) {
                        BuyerModel payload = new BuyerModel();
                        payload.error = error;
                        mDispatcher.dispatch(BuyerActionBuilder.newUpdateBuyerAction(payload));
                    }
                }
        );
        add(request);
    }

    // Utils

    private BuyerModel buyerResponseToBuyerModel(BuyerPNRestResponse from) {
        BuyerModel buyer = new BuyerModel();
        buyer.setBuyerId(from.ID);
        buyer.setName(StringEscapeUtils.unescapeHtml4(from.name));
        buyer.setDescription(StringEscapeUtils.unescapeHtml4(from.description));
        buyer.setIsVisible(from.visible);
        if (from.icon != null) {
            buyer.setIconUrl(from.icon.img);
        }
        buyer.setIsPN(true);
        buyer.setOrigin(BuyerModel.ORIGIN_PN_REST);
        return buyer;
    }
}
