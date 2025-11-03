package com.client.custom.nafath.authenticator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.client.custom.nafath.authenticator.exeption.NafathAuthenticatorClientException;
import com.client.custom.nafath.authenticator.exeption.NafathAuthenticatorServerException;
import com.client.custom.nafath.authenticator.models.NafathResponse;
import com.client.custom.nafath.authenticator.util.Utils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.owasp.encoder.Encode;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;

public class CustomNafathAuthenticator extends AbstractApplicationAuthenticator
        implements FederatedApplicationAuthenticator {

    private static final Log LOG = LogFactory.getLog(CustomNafathAuthenticator.class);

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {

        String accessToken = (String) context.getProperty(CustomNafathAuthenticatorConstants.ACCESS_TOKEN);

        if (StringUtils.isBlank(accessToken)) {
            LOG.error("Access token is missing in the authentication context");
            throw new AuthenticationFailedException("Access token is missing");
        }

        try {
            String jwtToken = getUserInfo(accessToken);
            if (StringUtils.isBlank(jwtToken)) {
                LOG.error("Failed to retrieve JWT token from user info");
                throw new AuthenticationFailedException("Failed to retrieve user information");
            }

            Map<String, Object> userAttributes = extractPayload(jwtToken);
            Map<ClaimMapping, String> claims = null;
            if (userAttributes != null) {
                claims = buildClaims(userAttributes);
            }

            String userId = userAttributes.get(CustomNafathAuthenticatorConstants.USER_ID).toString();

            if (StringUtils.isBlank(userId)) {
                LOG.error("User ID is missing in the JWT payload");
                throw new AuthenticationFailedException("User ID is missing in the authentication response");
            }

            AuthenticatedUser authenticatedUser = AuthenticatedUser
                    .createFederateAuthenticatedUserFromSubjectIdentifier(userId);
            authenticatedUser.setUserAttributes(claims);
            context.setSubject(authenticatedUser);

            LOG.info("Successfully authenticated user: " + userId);
        } catch (IOException e) {
            throw new AuthenticationFailedException("Error occurred while processing authentication response", e);
        }
    }

    /*
     * Check whether the authentication or logout request can be handled by the
     * authenticator
     *
     * @param request
     * @return boolean
     */
    @Override
    public boolean canHandle(HttpServletRequest request) {
        return !StringUtils.isBlank(request.getParameter(CustomNafathAuthenticatorConstants.NAFATH_ID)) ||
                !StringUtils.isBlank(request.getParameter(CustomNafathAuthenticatorConstants.REQUEST_STATUS));
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {
        return request.getParameter(CustomNafathAuthenticatorConstants.SESSION_DATA_KEY);
    }

    @Override
    public String getName() {
        return CustomNafathAuthenticatorConstants.AUTHENTICATOR_NAME;
    }

    @Override
    public String getFriendlyName() {
        return CustomNafathAuthenticatorConstants.FRIENDLY_NAME;
    }

    @Override
    public AuthenticatorFlowStatus process(HttpServletRequest request, HttpServletResponse response,
                                           AuthenticationContext context)
            throws AuthenticationFailedException, LogoutFailedException {

        if (context.isLogoutRequest()) {
            return AuthenticatorFlowStatus.SUCCESS_COMPLETED; }

        if (StringUtils.isBlank(request.getParameter(CustomNafathAuthenticatorConstants.NAFATH_ID)) &&
                StringUtils.isBlank((String) context.getProperty(CustomNafathAuthenticatorConstants.NAFATH_ID))) {
            initiateAuthenticationRequest(request, response, context);
            return AuthenticatorFlowStatus.INCOMPLETE;
        } else if(StringUtils.isBlank((String) context.getProperty(CustomNafathAuthenticatorConstants.NAFATH_RANDOM_TEXT))) {
            initiateAuthenticationRequest(request, response, context);
            return AuthenticatorFlowStatus.INCOMPLETE;
        } else if(CustomNafathAuthenticatorConstants.REQUEST_STATUS_EXPIRED.equals(context.getProperty(CustomNafathAuthenticatorConstants.REQUEST_STATUS)) ||
                CustomNafathAuthenticatorConstants.REQUEST_STATUS_REJECTED.equals(context.getProperty(CustomNafathAuthenticatorConstants.REQUEST_STATUS))) {
            LOG.error("Authentication failed!! Request status: " + context.getProperty(CustomNafathAuthenticatorConstants.REQUEST_STATUS));

            context.removeProperty(CustomNafathAuthenticatorConstants.NAFATH_ID);
            context.removeProperty(CustomNafathAuthenticatorConstants.NAFATH_TRANSACTION_ID);
            context.removeProperty(CustomNafathAuthenticatorConstants.NAFATH_RANDOM_TEXT);
            context.removeProperty(CustomNafathAuthenticatorConstants.REQUEST_STATUS);

            context.setProperty(CustomNafathAuthenticatorConstants.RETRY, true);

            initiateAuthenticationRequest(request, response, context);
            return AuthenticatorFlowStatus.INCOMPLETE;
        }

        return super.process(request, response, context);
    }

    protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context)
            throws AuthenticationFailedException {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authentication request has initialized.");
            }
            if (StringUtils.isBlank(request.getParameter(CustomNafathAuthenticatorConstants.NAFATH_ID))) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authentication request does not contain a nafath Id.");
                }
                handleInitialRequest(response, context);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authentication request contains a nafath Id.");
                }
                handleNafathIdSubmission(request, response, context);
            }
        } catch (NafathAuthenticatorServerException | NafathAuthenticatorClientException e) {
            LOG.error("Error during authentication request initiation", e);
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
    }


    private void handleInitialRequest(HttpServletResponse response, AuthenticationContext context)
            throws NafathAuthenticatorServerException {

        try {
            String queryString = buildQueryString(context);
            if(context.getProperty(CustomNafathAuthenticatorConstants.RETRY) != null && (Boolean) context.getProperty(CustomNafathAuthenticatorConstants.RETRY)) {
                queryString += "&retry=true";
            }
            String nafathLoginPage = ServiceURLBuilder.create().addPath(CustomNafathAuthenticatorConstants.NAFATH_LOGIN_PAGE).
                    build().getAbsolutePublicURL();
            response.sendRedirect(FrameworkUtils.appendQueryParamsStringToUrl(nafathLoginPage, queryString));
        } catch (URLBuilderException e) {
            throw new NafathAuthenticatorServerException("Error building the Nafath login page", e);
        } catch (IOException e) {
            throw new NafathAuthenticatorServerException("Error redirecting to the Nafath login page", e);
        }
    }

    private void handleNafathIdSubmission(HttpServletRequest request, HttpServletResponse response,
                                          AuthenticationContext context)
            throws NafathAuthenticatorClientException, NafathAuthenticatorServerException {
        String nafathId = request.getParameter(CustomNafathAuthenticatorConstants.NAFATH_ID);

        if (StringUtils.isBlank(nafathId)) {
            throw new NafathAuthenticatorClientException("Nafath Id cannot be empty");
        }

        if(StringUtils.isNotBlank(nafathId)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Nafath Id received from the authentication request: " + nafathId);
            }
        }

        NafathResponse nafathResponse = getRandomTextFromNafath(Utils.getNafathEndpoint(
                Boolean.parseBoolean(context.getAuthenticatorProperties().get(CustomNafathAuthenticatorConstants.NAFATH_PRE_PROD_INTEGRATION))), nafathId);

        context.setProperty(CustomNafathAuthenticatorConstants.NAFATH_ID, nafathId);
        context.setProperty(CustomNafathAuthenticatorConstants.NAFATH_TRANSACTION_ID, nafathResponse.getTransactionId());
        context.setProperty(CustomNafathAuthenticatorConstants.NAFATH_RANDOM_TEXT, nafathResponse.getRandomText());

        String queryString = buildQueryString(context) + "&" + CustomNafathAuthenticatorConstants.NAFATH_RANDOM_TEXT + "=" + nafathResponse.getRandomText();
        String randomTextPageURI = buildNafathLoginPageURL(queryString);
        try {
            response.sendRedirect(randomTextPageURI);
        } catch (IOException e) {
            throw new NafathAuthenticatorServerException("Error redirecting user to Nafath random text page", e);
        }
    }

    private String buildQueryString(AuthenticationContext context) {
        return String.format("t=%s&sessionDataKey=%s&sp=%s",
                context.getLoginTenantDomain(),
                context.getContextIdentifier(),
                Encode.forUriComponent(context.getServiceProviderName()));
    }

    private String buildNafathLoginPageURL(String queryString) throws
            NafathAuthenticatorServerException {
        try {
            String nafathRandomTextPage = ServiceURLBuilder.create().addPath(CustomNafathAuthenticatorConstants.NAFATH_RANDOM_TEXT_PAGE).
                    build().getAbsolutePublicURL();
            return FrameworkUtils.appendQueryParamsStringToUrl(nafathRandomTextPage, queryString);
        } catch (URLBuilderException e) {
            throw new NafathAuthenticatorServerException("Error building Nafath random page", e);
        }
    }

    private NafathResponse getRandomTextFromNafath(String nafathEndpoint, String nafathId)
            throws NafathAuthenticatorServerException {

        Map<String, Object> payload = new HashMap<>();
        payload.put("Action", "SpRequest");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("service", "AdvancedLogin");
        parameters.put(CustomNafathAuthenticatorConstants.USER_ID, nafathId);

        payload.put("Parameters", parameters);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newHttpClient();

            // Create the POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(nafathEndpoint))  // Replace with actual URL
                    .header(CustomNafathAuthenticatorConstants.CONTENT_TYPE, CustomNafathAuthenticatorConstants.APPLICATION_JSON_TYPE)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode == 200) {
                // Parse the successful response
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                return new NafathResponse(jsonResponse.get(CustomNafathAuthenticatorConstants.NAFATH_TRANSACTION_ID).asText(),
                        jsonResponse.get(CustomNafathAuthenticatorConstants.NAFATH_RANDOM_TEXT).asText());
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

        } catch (JsonProcessingException e) {
            throw new NafathAuthenticatorServerException("Error processing Nafath request payload", e);
        } catch (IOException | InterruptedException e) {
            throw new NafathAuthenticatorServerException("Error sending Nafath request", e);
        }
    }

    private String getUserInfo(String accessToken) throws IOException {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token is null");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpGet request = new HttpGet(CustomNafathAuthenticatorConstants.GET_USER_INFO_URI);
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("Accept", CustomNafathAuthenticatorConstants.APPLICATION_JWT_TYPE);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    String jwtToken = EntityUtils.toString(response.getEntity());
                    LOG.info("JWT Token received successfully.");
                    return jwtToken;
                } else {
                    LOG.error("Error response from API. Status code: " + statusCode);
                    throw new IOException("Failed to get user info. Status code: " + statusCode);
                }
            } catch (IOException e) {
                LOG.error("IO error while calling Nafath API", e);
                throw new IOException("Failed to communicate with Nafath API", e);
            }
        }
    }

    public static Map<String, Object> extractPayload(String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new IllegalArgumentException("JWT token cannot be null or empty");
        }

        try {
            // JWT is in three parts: header.payload.signature
            String[] splitToken = jwtToken.split("\\.");
            if (splitToken.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token");
            }

            // Get the payload (second part of the token)
            String payload = splitToken[1];

            // Base64URL decode the payload
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            JSONObject jsonObject = new JSONObject(decodedPayload);
            Map<String, Object> userInfo = new HashMap<>();

            for (Object key : jsonObject.keySet()) {
                if (CustomNafathAuthenticatorConstants.USER_INFO.equals(key)) {
                    JSONObject user = jsonObject.getJSONObject(CustomNafathAuthenticatorConstants.USER_INFO);
                    for (Object userInfoKey : user.keySet()) {
                        userInfo.put((String) userInfoKey, user.get((String) userInfoKey));
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Adding user attribute : " + user.get((String) userInfoKey) + " <> " + user.get((String) userInfoKey) + " : " + user.get((String) userInfoKey));
                        }
                    }
                } else {
                    userInfo.put((String) key, jsonObject.get((String) key));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding user attribute : " + (String) key + " <> " + (String) key + " : " + jsonObject.get((String) key));
                    }
                }
            }

            return userInfo;
        } catch (IllegalArgumentException | org.json.JSONException e) {
            LOG.error("Error decoding JWT token: " + e.getMessage(), e);
            return null;
        }
    }

    public Map<ClaimMapping, String> buildClaims(Map<String, Object> userAttributes) {
        Map<ClaimMapping, String> claims = new HashMap<>();

        for(Map.Entry<String, Object> entry: userAttributes.entrySet()) {
            claims.put(ClaimMapping.build(entry.getKey(), entry.getKey(), null, false), entry.getValue().toString());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding claim mapping : " + entry.getKey() + " <> " + entry.getKey() + " : " + entry.getValue().toString());
            }
        }

        return claims;
    }

    public List<Property> getConfigurationProperties() {
        List<Property> configProperties = new ArrayList<Property>();

        Property apiKey = new Property();
        apiKey.setName(CustomNafathAuthenticatorConstants.NAFATH_API_KEY);
        apiKey.setDisplayName("SP API Key");
        apiKey.setRequired(true);
        apiKey.setDescription("Enter the API Key of Nafath");
        apiKey.setDisplayOrder(0);
        configProperties.add(apiKey);

        Property apiSecret = new Property();
        apiSecret.setName(CustomNafathAuthenticatorConstants.NAFATH_API_SECRET);
        apiSecret.setDisplayName("SP API Secret");
        apiSecret.setRequired(true);
        apiSecret.setConfidential(true);
        apiSecret.setDescription("Enter the API Secret");
        apiSecret.setDisplayOrder(1);
        configProperties.add(apiSecret);

        Property isPreProd = new Property();
        isPreProd.setName(CustomNafathAuthenticatorConstants.NAFATH_PRE_PROD_INTEGRATION);
        isPreProd.setDisplayName("Nafath Pre Prod Integration");
        isPreProd.setRequired(false);
        isPreProd.setDescription("Select if the integration is done with Nafath pre prod environment");
        isPreProd.setDisplayOrder(2);
        isPreProd.setType("boolean");

        return configProperties;
    }
}
