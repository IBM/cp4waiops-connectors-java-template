package com.ibm.aiops.connectors.template;

import java.io.IOException;
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

    private AtomicReference<Configuration> _configuration;
    private AtomicBoolean _configurationUpdated;

    private Counter _samplesGathered;
    private Counter _primesFound;
    private Counter _compositesFound;
    private Counter _errorsSeen;

    private ExecutorService _executor;
    private List<Future<?>> _threads;

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
        Map<String, Double> cpuSamples = collectCPUSamples();
        _samplesGathered.increment();

        double currentUsage = 0;
        for (double value : cpuSamples.values()) {
            currentUsage += value;
        }

        // Check if threshold has been breached
        if (currentUsage < (double) config.getCpuThreshold()) {
            return;
        }

        // Emit event
        try {
            EventLifeCycleEvent elcEvent = newCPUThresholdLifeCycleEvent(config, hostname, ipAddress, currentUsage);
            CloudEvent ce = CloudEventBuilder.v1().withId(elcEvent.getId()).withSource(SELF_SOURCE)
                    .withType(THRESHOLD_BREACHED_CE_TYPE)
                    .withExtension("tenantid", "cfd95b7e-3bc7-4006-a4a8-a73a79c71255")
                    .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                    .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                    .withData("application/json", elcEvent.toJSON().getBytes(StandardCharsets.UTF_8)).build();
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
        type.setEventType("problem");
        type.setClassification("Threshold breach");
        type.setCondition(String.valueOf(currentUsage) + "%");
        event.setType(type);

        Map<String, Object> sender = new HashMap<>();
        sender.put("type", "agent");
        sender.put("hostname", hostname);
        sender.put("ipAddress", ipAddress);
        event.setSender(sender);

        Map<String, Object> resource = new HashMap<>();
        resource.put("type", "agent");
        resource.put("hostname", hostname);
        resource.put("ipAddress", ipAddress);
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

    protected Map<String, Double> collectCPUSamples() throws InterruptedException {
        Map<String, Double> samples = new HashMap<>();
        try {
            Runtime runtime = Runtime.getRuntime();
            String[] cmd = new String[] { "ps", "ax", "-o", "pid,%cpu" };
            Process process = runtime.exec(cmd);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorMsg = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                logger.log(Level.WARNING, "non zero exit code when collecting cpu samples: " + errorMsg);
            } else {
                String data = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                logger.log(Level.INFO, data);
                data.lines().forEach(line -> {
                    try {
                        String[] parts = line.split("\\w+", 2);
                        if (parts.length >= 2) {
                            String pid = parts[0];
                            double usage = Double.valueOf(parts[1]);
                            samples.put(pid, usage);
                        }
                    } catch (NumberFormatException error) {
                        // skip line
                    }
                });
            }
        } catch (IOException error) {
            logger.log(Level.WARNING, "failed to collect cpu samples", error);
            _errorsSeen.increment();
        }
        return samples;
    }

    protected void updateWorkload(Configuration config) {
        synchronized (this._threads) {
            if (config.getEnableCPUHeavyWorkload()) {
                for (int i = this._threads.size(); i < config.getNumCPUWorkloadThreads(); i++) {
                    Future<?> task = _executor.submit(() -> checkIfRandomNumbersArePrime());
                    _threads.add(task);
                }
                while (this._threads.size() > config.getNumCPUWorkloadThreads() && !this._threads.isEmpty()) {
                    _threads.get(this._threads.size()-1).cancel(true);
                    _threads.remove(this._threads.size()-1);
                }
            } else if (_executor != null) {
                while (!this._threads.isEmpty()) {
                    _threads.get(this._threads.size()-1).cancel(true);
                    _threads.remove(this._threads.size()-1);
                }
            }
        }
    }

    protected void checkIfRandomNumbersArePrime() {
        Random r = new Random();
        while (!Thread.interrupted()) {
            int value = r.nextInt(Integer.MAX_VALUE);
            if (inefficientIsPrime(value)) {
                logger.log(Level.INFO, "found a prime number: " + String.valueOf(value));
                _primesFound.increment();
            } else {
                logger.log(Level.FINE, "found a non-prime number: " + String.valueOf(value));
                _compositesFound.increment();
            }
        }
    }

    protected boolean inefficientIsPrime(int value) {
        if (value < 0) {
            return false;
        }
        for (int i = value - 1; i > 1; i--) {
            if (value % i == 0) {
                return false;
            }
        }
        return true;
    }
}
