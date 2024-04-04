package com.ibm.aiops.connectors.template;

import com.ibm.cp4waiops.connectors.sdk.SDKCheck;

import org.eclipse.microprofile.health.Readiness;

@Readiness
@jakarta.enterprise.context.ApplicationScoped
public class ConnectorTemplateReadinessCheck extends SDKCheck {
    public ConnectorTemplateReadinessCheck() {
        super(ConnectorTemplateReadinessCheck.class.getName(), Type.READINESS);
    }
}
