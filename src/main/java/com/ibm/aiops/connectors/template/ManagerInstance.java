package com.ibm.aiops.connectors.template;

import javax.enterprise.context.ApplicationScoped;

import com.ibm.cp4waiops.connectors.sdk.ConnectorManager;
import com.ibm.cp4waiops.connectors.sdk.StandardConnectorManager;

@ApplicationScoped
public class ManagerInstance {
    private ConnectorManager manager = new StandardConnectorManager(new ConnectorTemplateFactory());

    public ConnectorManager getConnectorManager() {
        return manager;
    }
}
