package com.ibm.aiops.connectors.template;

import com.ibm.cp4waiops.connectors.sdk.ConnectorManager;
import com.ibm.cp4waiops.connectors.sdk.StandardConnectorManager;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ManagerInstance {
    private ConnectorManager manager = new StandardConnectorManager(new ConnectorTemplateFactory());

    public ConnectorManager getConnectorManager() {
        return manager;
    }
}
