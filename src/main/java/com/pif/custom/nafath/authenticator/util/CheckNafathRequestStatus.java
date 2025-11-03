package com.client.custom.nafath.authenticator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.client.custom.nafath.authenticator.exeption.NafathAuthenticatorServerException;
import com.client.custom.nafath.authenticator.CustomNafathAuthenticatorConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class CheckNafathRequestStatus {

    private static final Log LOG = LogFactory.getLog(CheckNafathRequestStatus.class);

    public String getRequestStatus(String sessionDataKey) throws IOException, NafathAuthenticatorServerException {
        if (StringUtils.isBlank(sessionDataKey)) {
            LOG.warn("Session data key is null");
            return "";
        }

        AuthenticationContext context = FrameworkUtils.getAuthenticationContextFromCache(sessionDataKey);
        if (context == null) {
            LOG.warn("Authentication context is null for session data key: " + sessionDataKey);
            return "";
        }

        String transId = (String) context.getProperty(CustomNafathAuthenticatorConstants.NAFATH_TRANSACTION_ID);
        String nafathId = (String) context.getProperty(CustomNafathAuthenticatorConstants.NAFATH_ID);
        String random = (String) context.getProperty(CustomNafathAuthenticatorConstants.NAFATH_RANDOM_TEXT);

        LOG.debug(String.format("Session: %s, Nafath: %s, TransID: %s, Random: %s",
                sessionDataKey, nafathId, transId, random));

        Map<String, Object> payload = new HashMap<>();
        payload.put(CustomNafathAuthenticatorConstants.ACTION, CustomNafathAuthenticatorConstants.CHECK_SP_REQUEST);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(CustomNafathAuthenticatorConstants.NAFATH_TRANSACTION_ID, transId);  // Replace with actual Transaction ID
        parameters.put(CustomNafathAuthenticatorConstants.USER_ID, nafathId);      // Replace with actual Target User ID
        parameters.put(CustomNafathAuthenticatorConstants.NAFATH_RANDOM_TEXT, random);   // Replace with actual Random value from the first call

        payload.put(CustomNafathAuthenticatorConstants.PARAMETERS, parameters);

        // Convert the Map to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload = objectMapper.writeValueAsString(payload);

        // Create HttpClient
        HttpClient client = HttpClient.newHttpClient();

        // Create the POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Utils.getNafathEndpoint(
                        Boolean.parseBoolean(context.getAuthenticatorProperties().get(CustomNafathAuthenticatorConstants.NAFATH_PRE_PROD_INTEGRATION)))))  // Replace with actual URL
                .header(CustomNafathAuthenticatorConstants.CONTENT_TYPE, CustomNafathAuthenticatorConstants.APPLICATION_JSON_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //Handle the response
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode == 200) {
            // Parse the successful response
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            String status = jsonResponse.get(CustomNafathAuthenticatorConstants.REQUEST_STATUS).asText();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Nafath request status: " + status);
            }

            if (CustomNafathAuthenticatorConstants.REQUEST_STATUS_COMPLETED.equals(status)) {
                context.setProperty(CustomNafathAuthenticatorConstants.REQUEST_STATUS, status);

                // Check for person and accessToken in COMPLETED status
                if (jsonResponse.has(CustomNafathAuthenticatorConstants.PERSON)) {
                    JsonNode person = jsonResponse.get(CustomNafathAuthenticatorConstants.PERSON);
                    context.setProperty(CustomNafathAuthenticatorConstants.PERSON, person);
                }
                if(jsonResponse.has(CustomNafathAuthenticatorConstants.ACCESS_TOKEN)){
                    String accessToken = jsonResponse.get(CustomNafathAuthenticatorConstants.ACCESS_TOKEN).asText();
                    context.setProperty(CustomNafathAuthenticatorConstants.ACCESS_TOKEN, accessToken);
                }

            } else if(CustomNafathAuthenticatorConstants.REQUEST_STATUS_REJECTED.equals(status) ||
                    CustomNafathAuthenticatorConstants.REQUEST_STATUS_EXPIRED.equals(status)) {
                context.setProperty(CustomNafathAuthenticatorConstants.REQUEST_STATUS, status);
            }

            return status;
        } else {
            // Handle error response
            JsonNode errorResponse = objectMapper.readTree(responseBody);
            String errorCode = errorResponse.get("Code").asText();
            String requestedURL = errorResponse.get("RequestedURL").asText();
            String message = errorResponse.get("Message").asText();
            String trace = errorResponse.get("Trace").asText();

            String errorMsg = String.format("Nafath endpoint returned an error, Error Code: %s, " +
                            "Requested URL: %s, Message: %s, Trace: %s",
                    errorCode, requestedURL, message, trace);

            throw new NafathAuthenticatorServerException(errorMsg);
        }
    }
}
