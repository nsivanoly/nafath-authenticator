package com.client.custom.nafath.authenticator.exeption;

public class NafathAuthenticatorClientException extends Exception{

    public NafathAuthenticatorClientException(String msg) {
        super(msg);
    }

    public NafathAuthenticatorClientException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
