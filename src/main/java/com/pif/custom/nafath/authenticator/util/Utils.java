package com.client.custom.nafath.authenticator.util;

import static com.client.custom.nafath.authenticator.CustomNafathAuthenticatorConstants.NAFATH_PRE_PROD_URL;
import static com.client.custom.nafath.authenticator.CustomNafathAuthenticatorConstants.NAFATH_PROD_URL;

public class Utils {

    public static String getNafathEndpoint(boolean isPrePro) {
        if (isPrePro) {
            return NAFATH_PRE_PROD_URL;
        } else  {
            return NAFATH_PROD_URL;
        }
    }
}
