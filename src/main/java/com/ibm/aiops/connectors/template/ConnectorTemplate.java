package com.ibm.aiops.connectors.template;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.aiops.connectors.bridge.ConnectorStatus;
import com.ibm.aiops.connectors.sdk.ConnectorBase;
import com.ibm.aiops.connectors.sdk.ConnectorConfigurationHelper;
import com.ibm.aiops.connectors.sdk.ConnectorException;
import com.ibm.aiops.connectors.sdk.Constant;
import com.ibm.aiops.connectors.sdk.EventLifeCycleEvent;
import com.ibm.aiops.connectors.sdk.SDKSettings;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class ConnectorTemplate extends ConnectorBase {
    static final Logger logger = Logger.getLogger(ConnectorTemplate.class.getName());

    // Topics
    static final String LIFECYCLE_INPUT_EVENTS_TOPIC = "cp4waiops-cartridge.lifecycle.input.events";

    // Self identifier
    static final URI SELF_SOURCE = URI.create("template.connectors.aiops.ibm.com/grpc-event-template");

    static final String THRESHOLD_BREACHED_CE_TYPE = "com.ibm.aiops.connectors.template.threshold-breached";

    protected AtomicReference<Configuration> _configuration;
    protected AtomicBoolean _configurationUpdated;

    protected Counter _samplesGathered;
    protected Counter _primesFound;
    protected Counter _compositesFound;
    protected Counter _errorsSeen;

    protected ExecutorService _executor;
    protected List<Future<?>> _threads;

    /**
     * Instantiates a new ConnectorTemplate
     */
    public ConnectorTemplate() {
        _configuration = new AtomicReference<>();
        _configurationUpdated = new AtomicBoolean(false);
        _executor = Executors.newCachedThreadPool();
        _threads = new ArrayList<>();
    }

    @Override
    public void registerMetrics(MeterRegistry metricRegistry) {
        super.registerMetrics(metricRegistry);
        _samplesGathered = metricRegistry.counter("grpc.template.samples.gathered");
        _primesFound = metricRegistry.counter("grpc.template.primes.found");
        _compositesFound = metricRegistry.counter("grpc.template.composites.found");
        _errorsSeen = metricRegistry.counter("grpc.template.errors");
    }

    @Override
    public SDKSettings onConfigure(CloudEvent event) throws ConnectorException {
        ConnectorConfigurationHelper helper = new ConnectorConfigurationHelper(event);
        Configuration configuration = helper.getDataObject(Configuration.class);
        if (configuration == null) {
            throw new ConnectorException("no configuration provided");
        }
        _configuration.set(configuration);

        // Set initial topics and local state if needed
        SDKSettings settings = new SDKSettings();
        settings.consumeTopicNames = new String[] {};
        settings.produceTopicNames = new String[] { LIFECYCLE_INPUT_EVENTS_TOPIC };
        return settings;
    }

    @Override
    public SDKSettings onReconfigure(CloudEvent event) throws ConnectorException {
        // Update topics and local state if needed
        SDKSettings settings = onConfigure(event);
        _configurationUpdated.set(true);
        return settings;
    }

    @Override
    public void onTerminate(CloudEvent event) {
        // Cleanup external resources if needed
        _executor.shutdownNow();
    }

    @Override
    public void run() {
        // Set connector status as running
        emitStatus(ConnectorStatus.Phase.Running, Duration.ofMinutes(5));

        boolean interrupted = false;
        long lastRan = System.nanoTime();
        while (!interrupted) {
            try {
                // If configuration was successfully updated, resend a status update
                if (_configurationUpdated.get()) {
                    emitStatus(ConnectorStatus.Phase.Running, Duration.ofMinutes(5));
                }

                Configuration config = _configuration.get();
                updateWorkload(config);

                // Some background task that executes periodically
                if (System.nanoTime() - lastRan / 1000000000 > 30) {
                    checkCPUThreshold(config);
                }
                Thread.sleep(1 * 15 * 1000);
            } catch (InterruptedException ignored) {
                // termination of the process has been requested
                interrupted = true;
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void checkCPUThreshold(Configuration config) throws InterruptedException {
        String hostname = getHostName();
        String ipAddress = getIPAddress();
        double currentUsage = collectCPUSample();
        _samplesGathered.increment();

        logger.log(Level.INFO, "cpu usage: " + String.valueOf(currentUsage));

        // Check if threshold has been breached
        if (currentUsage < (double) config.getCpuThreshold()) {
            return;
        }

        // Emit event
        try {
            EventLifeCycleEvent elcEvent = newCPUThresholdLifeCycleEvent(config, hostname, ipAddress, currentUsage);
            CloudEvent ce = CloudEventBuilder.v1().withId(elcEvent.getId()).withSource(SELF_SOURCE)
                    .withType(THRESHOLD_BREACHED_CE_TYPE)
                    .withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                    .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                    .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                    .withData(Constant.JSON_CONTENT_TYPE, elcEvent.toJSON().getBytes(StandardCharsets.UTF_8)).build();
            emitCloudEvent(LIFECYCLE_INPUT_EVENTS_TOPIC, null, ce);
        } catch (JsonProcessingException error) {
            logger.log(Level.SEVERE, "failed to construct cpu threshold breached cloud event", error);
            _errorsSeen.increment();
        }
    }

    protected EventLifeCycleEvent newCPUThresholdLifeCycleEvent(Configuration config, String hostname, String ipAddress,
            double currentUsage) {

        EventLifeCycleEvent event = new EventLifeCycleEvent();
        event.setId(UUID.randomUUID().toString());
        event.setOccurrenceTime(Date.from(Instant.now()));
        event.setSummary("CPU threshold exceeded");
        event.setSeverity(config.getSeverityLevel());

        Map<String, String> details = new HashMap<>();
        details.put("cpu usage", String.valueOf(currentUsage));
        event.setDetails(details);

        event.setExpirySeconds(config.getExpirySeconds());

        EventLifeCycleEvent.Type type = new EventLifeCycleEvent.Type();
        type.setEventType(EventLifeCycleEvent.EVENT_TYPE_PROBLEM);
        type.setClassification("Threshold breach");
        type.setCondition(String.valueOf(currentUsage) + "%");
        event.setType(type);

        Map<String, Object> sender = new HashMap<>();
        sender.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "agent");
        sender.put(EventLifeCycleEvent.RESOURCE_HOSTNAME_FIELD, hostname);
        sender.put(EventLifeCycleEvent.RESOURCE_IP_ADDRESS_FIELD, ipAddress);
        event.setSender(sender);

        Map<String, Object> resource = new HashMap<>();
        resource.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "agent");
        resource.put(EventLifeCycleEvent.RESOURCE_HOSTNAME_FIELD, hostname);
        resource.put(EventLifeCycleEvent.RESOURCE_IP_ADDRESS_FIELD, ipAddress);
        event.setResource(resource);

        return event;
    }

    protected String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception error) {
            return null;
        }
    }

    protected String getIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception error) {
            return null;
        }
    }

    private long lastCollectionTS = 0;
    private Map<Long, Long> lastCollectionCPUTimes = new HashMap<>();

    protected double collectCPUSample() {
        synchronized (this) {
            var threadMX = ManagementFactory.getThreadMXBean();
            long[] threads = threadMX.getAllThreadIds();

            Map<Long, Long> currentCollectionCPUTimes = new HashMap<>();
            double totalCPUTime = 0;

            for (long id : threads) {
                long cpuNs = threadMX.getThreadCpuTime(id);
                if (cpuNs > 0) {
                    currentCollectionCPUTimes.put(id, cpuNs);
                    if (lastCollectionCPUTimes.containsKey(id)) {
                        totalCPUTime += cpuNs - lastCollectionCPUTimes.get(id);
                    } else {
                        totalCPUTime += cpuNs;
                    }
                }
            }

            if (lastCollectionTS == 0) {
                lastCollectionTS = System.nanoTime();
                return -1;
            } else {
                long currentTs = System.nanoTime();
                double percentage = 100 * (totalCPUTime) / (currentTs - lastCollectionTS);
                lastCollectionCPUTimes = currentCollectionCPUTimes;
                lastCollectionTS = currentTs;
                return percentage;
            }
        }
    }

    protected void updateWorkload(Configuration config) {
        synchronized (this._threads) {
            if (config.getEnableCPUHeavyWorkload()) {
                for (int i = this._threads.size(); i < config.getNumCPUWorkloadThreads(); i++) {
                    logger.log(Level.INFO, "spawning new workload thread");
                    Future<?> task = _executor.submit(() -> checkIfRandomNumbersArePrime());
                    _threads.add(task);
                }
                while (this._threads.size() > config.getNumCPUWorkloadThreads() && !this._threads.isEmpty()) {
                    logger.log(Level.INFO, "removing workload thread");
                    var item = _threads.remove(this._threads.size() - 1);
                    item.cancel(true);
                }
            } else if (_executor != null) {
                while (!this._threads.isEmpty()) {
                    logger.log(Level.INFO, "removing workload thread");
                    var item = _threads.remove(this._threads.size() - 1);
                    item.cancel(true);
                }
            }
        }
    }

    protected void checkIfRandomNumbersArePrime() {
        Random r = new Random();
        while (!Thread.interrupted()) {
            try {
                int value = r.nextInt(Integer.MAX_VALUE);
                if (inefficientIsPrime(value)) {
                    logger.log(Level.INFO, "found a prime number: " + String.valueOf(value));
                    _primesFound.increment();
                } else {
                    logger.log(Level.FINE, "found a non-prime number: " + String.valueOf(value));
                    _compositesFound.increment();
                }
            } catch (InterruptedException error) {
                break;
            }
        }
    }

    protected boolean inefficientIsPrime(int value) throws InterruptedException {
        if (value <= 1) {
            return false;
        }
        for (int i = value - 1; i > 1; i--) {
            if (value % i == 0) {
                return false;
            }
            if (Thread.interrupted())
                throw new InterruptedException();
        }
        return true;
    }
}
