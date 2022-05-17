package com.ibm.aiops.connectors.template;

import javax.enterprise.context.ApplicationScoped;

import com.ibm.aiops.connectors.sdk.SDKCheck;

import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class ConnectorTemplateLivenessCheck extends SDKCheck {
    public ConnectorTemplateLivenessCheck() {
        super(ConnectorTemplateLivenessCheck.class.getName(), Type.LIVENESS);
    }
}
