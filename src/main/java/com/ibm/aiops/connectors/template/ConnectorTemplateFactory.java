package com.ibm.aiops.connectors.template;

import com.ibm.aiops.connectors.sdk.Connector;
import com.ibm.aiops.connectors.sdk.ConnectorFactory;

public class ConnectorTemplateFactory implements ConnectorFactory {

    public ConnectorTemplateFactory() {
    }

    @Override
    public String GetConnectorName() {
        return "connector-template";
    }

    @Override
    public String GetComponentName() {
        return "connector";
    }

    @Override
    public Connector Create() {
        return new ConnectorTemplate();
    }

}
