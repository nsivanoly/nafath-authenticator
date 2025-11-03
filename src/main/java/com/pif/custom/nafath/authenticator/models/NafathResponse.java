package com.client.custom.nafath.authenticator.models;

public class NafathResponse {

    private final String transactionId;
    private final String randomText;

    public NafathResponse(String transactionId, String randomText) {
        this.transactionId = transactionId;
        this.randomText = randomText;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getRandomText() {
        return randomText;
    }

}
