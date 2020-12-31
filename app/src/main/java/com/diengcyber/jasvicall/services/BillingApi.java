package com.diengcyber.jasvicall.services;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import com.diengcyber.jasvicall.helpers.DeviceInfo;
import com.diengcyber.jasvicall.retro.APIService;
import com.diengcyber.jasvicall.retro.ApiUtils;
import com.diengcyber.jasvicall.retro.UuidModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.diengcyber.jasvicall.retro.ApiUtils.API_KEY;

public class BillingApi extends ContextWrapper {

    private APIService mAPIService;
    private DeviceInfo dev_info;

    public BillingApi(Context base) {
        super(base);
        mAPIService = ApiUtils.getAPIService();
        dev_info = new DeviceInfo();
    }

    private String getTanggal() {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate = df.format(c);
        System.out.println("Formate date => " + formattedDate);
        return formattedDate;
    }

    public void startBilling(Callback<UuidModel> callback) {
        mAPIService.billingApi(dev_info.deviceId(this.getContentResolver()), dev_info.getIPAddress(true), "on", getTanggal(), API_KEY).enqueue(callback);
    }

    public void closeBilling(Callback<UuidModel> callback) {
        mAPIService.billingApi(dev_info.deviceId(this.getContentResolver()), dev_info.getIPAddress(true), "off", getTanggal(), API_KEY).enqueue(callback);
    }
}
