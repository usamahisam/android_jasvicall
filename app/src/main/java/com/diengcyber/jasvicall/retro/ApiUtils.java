package com.diengcyber.jasvicall.retro;

public class ApiUtils {

    private ApiUtils() {}

    public static final String BASE_URL = "http://192.168.99.101:8080/jasvicall/api/";
    public static final String API_KEY = "2e4b32a616e57a9c98b3a7e48c330ed1";

    public static APIService getAPIService() {
        return RetrofitClient.getClient(BASE_URL).create(APIService.class);
    }
}