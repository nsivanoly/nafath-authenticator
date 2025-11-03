package com.client.custom.nafath.authenticator.internal;

import com.client.custom.nafath.authenticator.CustomNafathAuthenticator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;

@Component(
        name = "custom.nafath.authenticator",
        immediate = true
)

public class CustomNafathAuthenticatorServiceComponent {
    private static final Log LOG = LogFactory.getLog(CustomNafathAuthenticatorServiceComponent.class);

    @Activate
    protected void activate(ComponentContext ctxt) {
        try {
            CustomNafathAuthenticator customFederatedAuthenticator = new CustomNafathAuthenticator();
            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), customFederatedAuthenticator, null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Custom Nafath Authenticator bundle is activated.");
            }
        } catch (Throwable e) {
            LOG.error(" Error while activating Custom Nafath Authenticator. ", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        LOG.info("Nafath authenticator deactivated");
    }
}
