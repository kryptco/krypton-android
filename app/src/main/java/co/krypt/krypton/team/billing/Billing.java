package co.krypt.krypton.team.billing;

import com.google.gson.annotations.SerializedName;

import co.krypt.krypton.protocol.JSON;

/**
 * Created by Kevin King on 2/28/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Billing {
    public static class Tier {
        @SerializedName("name")
        @JSON.JsonRequired
        public String name;
        @SerializedName("price")
        @JSON.JsonRequired
        public long price;

        @SerializedName("limit")
        @JSON.JsonRequired
        public Usage limit;
    }
    public static class Usage {
        @SerializedName("members")
        @JSON.JsonRequired
        public long members;

        @SerializedName("hosts")
        @JSON.JsonRequired
        public long hosts;

        @SerializedName("logs_last_30_days")
        @JSON.JsonRequired
        public long logsLast30Days;
    }

    @SerializedName("current_tier")
    @JSON.JsonRequired
    public Tier currentTier;

    @SerializedName("usage")
    @JSON.JsonRequired
    public Usage usage;

    public static class GetBillingUrlOutput {
        @SerializedName("billing_url")
        @JSON.JsonRequired
        public String billingUrl;
    }
}
