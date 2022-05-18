package com.ibm.aiops.connectors.template;

import com.ibm.cp4waiops.connectors.sdk.Connector;
import com.ibm.cp4waiops.connectors.sdk.ConnectorFactory;

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
