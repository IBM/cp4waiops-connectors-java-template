package com.ibm.aiops.connectors.template;

import jakarta.enterprise.context.ApplicationScoped;

import com.ibm.cp4waiops.connectors.sdk.SDKCheck;

import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class ConnectorTemplateLivenessCheck extends SDKCheck {
    public ConnectorTemplateLivenessCheck() {
        super(ConnectorTemplateLivenessCheck.class.getName(), Type.LIVENESS);
    }
}
