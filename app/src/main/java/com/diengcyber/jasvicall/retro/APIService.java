package com.diengcyber.jasvicall.retro;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface APIService {

    @POST("reg/bill")
    @FormUrlEncoded
    Call<UuidModel> billingApi(@Field("uuid") String uuid,
                               @Field("ipaddress") String IPAddress,
                               @Field("status") String status,
                               @Field("time") String time,
                               @Field("X-API-KEY") String API_KEY);
}