package com.ibm.aiops.connectors.template;

import javax.enterprise.context.ApplicationScoped;

import com.ibm.aiops.connectors.sdk.ConnectorManager;
import com.ibm.aiops.connectors.sdk.StandardConnectorManager;

@ApplicationScoped
public class ManagerInstance {
    private ConnectorManager manager = new StandardConnectorManager(new ConnectorTemplateFactory());

    public ConnectorManager getConnectorManager() {
        return manager;
    }
}
