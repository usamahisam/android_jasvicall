package com.diengcyber.jasvicall.retro;

public class UuidModel {
    String status;
    String message;

    public UuidModel(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}