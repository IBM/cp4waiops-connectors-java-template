package com.ibm.aiops.connectors.template;

import javax.enterprise.context.ApplicationScoped;

import com.ibm.aiops.connectors.sdk.SDKCheck;

import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ConnectorTemplateReadinessCheck extends SDKCheck {
    public ConnectorTemplateReadinessCheck() {
        super(ConnectorTemplateReadinessCheck.class.getName(), Type.READINESS);
    }
}
