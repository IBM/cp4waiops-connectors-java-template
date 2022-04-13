package com.ibm.aiops.connectors.template;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.ibm.aiops.connectors.sdk.ConnectorException;
import com.ibm.aiops.connectors.sdk.StatusWriter;
import com.ibm.aiops.connectors.sdk.VaultHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.Mockito;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

/**
 * This test example does not use the MockBridgeServer utility
 */
public class TestConnectorTemplate {
    StatusWriter _mockStatusWriter;
    VaultHelper _mockVaultHelper;
    BlockingDeque<CloudEvent> _eventQueue;
    MeterRegistry _metricRegistry;

    ConnectorTemplate _connector;

    @BeforeEach
    void setup(TestInfo testInfo) throws ConnectorException {
        System.out.println("\n\nRunning test: " + testInfo.getDisplayName() + "\n=====");
        _mockStatusWriter = Mockito.mock(StatusWriter.class);
        _mockVaultHelper = Mockito.mock(VaultHelper.class);

        _eventQueue = new LinkedBlockingDeque<>(1024);
        _metricRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        _connector = new ConnectorTemplate();
        _connector.onInit("_conn_id", "connector", _eventQueue, _mockStatusWriter, _mockVaultHelper);
        _connector.registerMetrics(_metricRegistry);
    }

    @AfterEach
    void teardown() throws InterruptedException {
        System.out.println("\n\nTeardown:\n=====");
    }

    @Test
    @DisplayName("collectCPUSamples gathers data")
    void testCollectCPUSamples() throws IOException, InterruptedException {
        Map<String, Double> cpuSamples = _connector.collectCPUSamples();
        Assertions.assertTrue(cpuSamples.size() > 0, "expected cpu samples to be gathered");
    }
}
