package com.ibm.aiops.connectors.template;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.cp4waiops.connectors.sdk.ConnectorException;
import com.ibm.cp4waiops.connectors.sdk.EventLifeCycleEvent;
import com.ibm.cp4waiops.connectors.sdk.StatusWriter;
import com.ibm.cp4waiops.connectors.sdk.VaultHelper;

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
    @DisplayName("collectCPUSample gathers data")
    void testCollectCPUSample() throws IOException, InterruptedException {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            System.out.println("waiting for valid sample");
            double cpuSample = _connector.collectCPUSample();
            while (cpuSample < 0) {
                if (Thread.interrupted())
                    throw new InterruptedException();
                Thread.sleep(5000);
                cpuSample = _connector.collectCPUSample();
            }
            System.out.println("sample gathered: " + String.valueOf(cpuSample));
        });
    }

    @Test
    @DisplayName("can interrrupt checkIfRandomNumbersArePrime")
    void testCanInterruptCheckIfRandomNumbersArePrime() throws InterruptedException {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            Thread task = new Thread(() -> _connector.checkIfRandomNumbersArePrime());
            task.start();
            Thread.sleep(100);
            task.interrupt();
            task.join();
        });
    }

    @Test
    @DisplayName("inefficientIsPrime correctly detects primes")
    void testInefficientIsPrime() throws InterruptedException {
        Map<Integer, Boolean> values = new HashMap<>();
        values.put(-1, false);
        values.put(0, false);
        values.put(1, false);
        values.put(2, true);
        values.put(4, false);
        values.put(1223, true);
        values.put(1221, false);

        for (var entry : values.entrySet()) {
            Boolean actual = _connector.inefficientIsPrime(entry.getKey());
            Assertions.assertEquals(entry.getValue(), actual,
                    "expected different result for " + String.valueOf(entry.getKey()));
        }
    }

    @Test
    @DisplayName("getHostName returns a value that is not empty or null")
    void testGetHostName() {
        String value = _connector.getHostName();
        Assertions.assertNotNull(value);
        Assertions.assertNotEquals(0, value.length());
    }

    @Test
    @DisplayName("getIPAddress returns a value that is not empty or null")
    void testGetIPAddress() {
        String value = _connector.getIPAddress();
        Assertions.assertNotNull(value);
        Assertions.assertNotEquals(0, value.length());
    }

    @Test
    @DisplayName("newCPUThresholdLifeCycleEvent serializes")
    void testNewCPUThresholdLifeCycleEvent() throws JsonProcessingException {
        Configuration config = new Configuration();
        config.setSeverityLevel(4);
        config.setExpirySeconds(500);
        EventLifeCycleEvent evt = _connector.newCPUThresholdLifeCycleEvent(config, "localhost", "127.0.0.1", 30, 34);

        Assertions.assertEquals(EventLifeCycleEvent.EVENT_TYPE_PROBLEM, evt.getType().getEventType());
        Assertions.assertEquals("Exceeds 30.0%", evt.getType().getCondition());
        Assertions.assertEquals(4, evt.getSeverity());
        Assertions.assertEquals(500, evt.getExpirySeconds());
        Assertions.assertEquals("34.0", evt.getDetails().get("cpu usage"));

        String serialized = evt.toJSON();
        Assertions.assertNotNull(serialized);
        Assertions.assertNotEquals(0, serialized.length());
    }

    @Test
    @DisplayName("updateWorkload scales correctly")
    void testUpdateWorkload() {
        Configuration config = new Configuration();
        config.setEnableCPUHeavyWorkload(true);
        config.setNumCPUWorkloadThreads(2);

        _connector.updateWorkload(config);
        Assertions.assertEquals(2, _connector._threads.size(), "expected scale up");

        config = new Configuration();
        config.setEnableCPUHeavyWorkload(true);
        config.setNumCPUWorkloadThreads(1);
        _connector.updateWorkload(config);
        Assertions.assertEquals(1, _connector._threads.size(), "expected scale down by 1");

        config = new Configuration();
        config.setEnableCPUHeavyWorkload(true);
        config.setNumCPUWorkloadThreads(3);
        _connector.updateWorkload(config);
        Assertions.assertEquals(3, _connector._threads.size(), "expected scale up by 2");

        config = new Configuration();
        config.setEnableCPUHeavyWorkload(false);
        config.setNumCPUWorkloadThreads(3);
        _connector.updateWorkload(config);
        Assertions.assertEquals(0, _connector._threads.size(), "expected scale down to 0");
    }
}
