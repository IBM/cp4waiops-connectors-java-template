package com.ibm.aiops.connectors.template;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aiops.connectors.bridge.ConnectorStatus;
import com.ibm.cp4waiops.connectors.sdk.ConnectorBase;
import com.ibm.cp4waiops.connectors.sdk.ConnectorConfigurationHelper;
import com.ibm.cp4waiops.connectors.sdk.ConnectorException;
import com.ibm.cp4waiops.connectors.sdk.Constant;
import com.ibm.cp4waiops.connectors.sdk.EventLifeCycleEvent;
import com.ibm.cp4waiops.connectors.sdk.SDKSettings;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.json.JSONObject;

public class ConnectorTemplate extends ConnectorBase {
    static final Logger logger = Logger.getLogger(ConnectorTemplate.class.getName());

    // Topics
    static final String LIFECYCLE_INPUT_EVENTS_TOPIC = "cp4waiops-cartridge.lifecycle.input.events";
    static final String METRICS_MANAGER_INPUT_TOPIC = "cp4waiops-cartridge.analyticsorchestrator.metrics.itsm.raw";

    // Self identifier
    static final URI SELF_SOURCE = URI.create("template.connectors.aiops.ibm.com/grpc-event-template");

    static final String THRESHOLD_BREACHED_CE_TYPE = "com.ibm.aiops.connectors.template.threshold-breached";
    static final String METRIC_GATHERED_CE_TYPE = "com.ibm.aiops.connectors.template.metric-gathered";

    static final String METRIC_RESOURCE_ID = "database01.bigblue.com";

    protected AtomicReference<Configuration> _configuration;

    protected AtomicLong _cpuMetricLastGathered;

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
        _cpuMetricLastGathered = new AtomicLong(0);
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
        // Generate then dump sample historical metric data
        if (shouldGenerateSampleData(_configuration.get(), configuration)) {

            try {
                // Convert dates to epoch ms format
                SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                Date startdate = df.parse(configuration.getHistoricStartDate());
                Date enddate = df.parse(configuration.getHistoricEndDate());
                String start = String.valueOf(startdate.getTime());
                String end = String.valueOf(enddate.getTime());
                String today = df.format(new Date());
                if (configuration.getHistoricEndDate().equals(today)) {
                    end = String.valueOf(Instant.now().toEpochMilli());
                }
                logger.log(Level.INFO,
                        "Generating sample historical data with metric name: " + configuration.getMetricName());
                generateData(start, end, configuration.getMetricName());
            } catch (ParseException e) {
                logger.log(Level.INFO, "Error with data generation: " + e.getMessage());
            }

        }
        _configuration.set(configuration);

        // Set initial topics and local state if needed
        SDKSettings settings = new SDKSettings();
        settings.consumeTopicNames = new String[] {};
        settings.produceTopicNames = new String[] { LIFECYCLE_INPUT_EVENTS_TOPIC, METRICS_MANAGER_INPUT_TOPIC };

        return settings;
    }

    @Override
    public SDKSettings onReconfigure(CloudEvent event) throws ConnectorException {
        // Update topics and local state if needed
        SDKSettings settings = onConfigure(event);
        return settings;
    }

    @Override
    public void onTerminate(CloudEvent event) {
        // Cleanup external resources if needed
        _executor.shutdownNow();
    }

    @Override
    public void run() {
        final long NANOSECONDS_PER_SECOND = 1000000000;
        final long TASK_PERIOD_S = 60;
        final long STATUS_UPDATE_PERIOD_S = 150;
        final long LOOP_PERIOD_MS = 1000;

        boolean interrupted = false;
        long taskLastRan = 0;
        long statusLastUpdated = 0;
        while (!interrupted) {
            try {
                Configuration config = _configuration.get();
                updateWorkload(config);

                // Some background task that executes periodically
                if ((System.nanoTime() - taskLastRan) / NANOSECONDS_PER_SECOND > TASK_PERIOD_S) {
                    taskLastRan = System.nanoTime();

                    checkCPUThreshold(config);
                }

                // Periodic status update
                if ((System.nanoTime() - statusLastUpdated) / NANOSECONDS_PER_SECOND > STATUS_UPDATE_PERIOD_S) {
                    statusLastUpdated = System.nanoTime();
                    updateStatus();
                }

                // Wait
                Thread.sleep(LOOP_PERIOD_MS);
            } catch (InterruptedException ignored) {
                // termination of the process has been requested
                interrupted = true;
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void updateStatus() {
        final Duration StatusTTL = Duration.ofMinutes(5);
        final Duration LastGatherPeriod = Duration.ofMinutes(3);

        long lastGather = _cpuMetricLastGathered.get();
        if (System.nanoTime() - lastGather > LastGatherPeriod.toNanos()) {
            Map<String, String> details = new HashMap<>();
            details.put("reason", "failed to gather cpu metrics recently");
            emitStatus(ConnectorStatus.Phase.Retrying, StatusTTL, details);
        } else {
            emitStatus(ConnectorStatus.Phase.Running, StatusTTL);
        }
    }

    protected void generateData(String start, String end, String metricName) {

        Random random = new Random();
        int interval = 300000;
        Long startdate = Long.parseLong(start);
        Long enddate = Long.parseLong(end);

        String result = "{\"groups\": [\n";
        int counter = 0;

        while (startdate + interval < enddate) {

            // Counter ensures cloud event payload is below Kafka max payload size of 1mb
            if (counter < 4999) {

                result += "{\"timestamp\":\"" + String.valueOf(startdate) + "\",\"resourceID\":\"" + METRIC_RESOURCE_ID
                        + "\",\"metrics\":{\"" + metricName + "\":0." + String.valueOf(random.nextInt(999999999))
                        + "},\"attributes\":{\"group\":\"CPU\",\"node\":\"" + METRIC_RESOURCE_ID + "\"}},\n";
                startdate += 300000;
                counter++;
            } else {
                result += "{\"timestamp\":\"" + String.valueOf(startdate) + "\",\"resourceID\":\"" + METRIC_RESOURCE_ID
                        + "\",\"metrics\":{\"" + metricName + "\":0." + String.valueOf(random.nextInt(999999999))
                        + "},\"attributes\":{\"group\":\"CPU\",\"node\":\"" + METRIC_RESOURCE_ID + "\"}}\n";

                result += "]}";

                CloudEvent ce = CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withSource(SELF_SOURCE)
                        .withType(METRIC_GATHERED_CE_TYPE)
                        .withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                        .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                        .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                        .withData(Constant.JSON_CONTENT_TYPE, result.getBytes(StandardCharsets.UTF_8)).build();
                emitCloudEvent(METRICS_MANAGER_INPUT_TOPIC, null, ce);
                counter = 0;
                result = "{\"groups\": [\n";

            }

        }
        result += "{\"timestamp\":\"" + String.valueOf(startdate) + "\",\"resourceID\":\"" + METRIC_RESOURCE_ID
                + "\",\"metrics\":{\"" + metricName + "\":0." + String.valueOf(random.nextInt(999999999))
                + "},\"attributes\":{\"group\":\"CPU\",\"node\":\"" + METRIC_RESOURCE_ID + "\"}}\n";

        result += "]}";

        CloudEvent ce = CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withSource(SELF_SOURCE)
                .withType(METRIC_GATHERED_CE_TYPE)
                .withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withData(Constant.JSON_CONTENT_TYPE, result.getBytes(StandardCharsets.UTF_8)).build();
        emitCloudEvent(METRICS_MANAGER_INPUT_TOPIC, null, ce);
        logger.log(Level.INFO, "Done sending sample data");

    }

    protected void checkCPUThreshold(Configuration config) throws InterruptedException {
        String hostname = getHostName();
        String ipAddress = getIPAddress();
        double currentUsage = collectCPUSample();
        _samplesGathered.increment();

        logger.log(Level.INFO, "cpu usage: " + String.valueOf(currentUsage));

        // Metric
        if (config.getEnableGatherMetrics() && config.getIsLiveData()) {
            // Emit event
            try {
                MetricManagerMetric mmmEvent = newMetricGatheredEvent(config, currentUsage);
                Map<String, ArrayList> group = new HashMap<>();
                ArrayList<MetricManagerMetric> groupArray = new ArrayList<MetricManagerMetric>();
                groupArray.add(mmmEvent);
                group.put("groups", groupArray);
                String jsonGroup = new ObjectMapper().writeValueAsString(group);
                CloudEvent ce = CloudEventBuilder.v1().withId(mmmEvent.getId()).withSource(SELF_SOURCE)
                        .withType(METRIC_GATHERED_CE_TYPE)
                        .withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                        .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                        .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                        .withData(Constant.JSON_CONTENT_TYPE, jsonGroup.getBytes(StandardCharsets.UTF_8)).build();
                emitCloudEvent(METRICS_MANAGER_INPUT_TOPIC, null, ce);
            } catch (JsonProcessingException error) {
                logger.log(Level.SEVERE, "failed to construct metric gathered cloud event", error);
                _errorsSeen.increment();
            }
        }

        // Check if threshold has been breached
        if (currentUsage < (double) config.getCpuThreshold()) {
            return;
        }

        // Emit event
        try {
            EventLifeCycleEvent elcEvent = newCPUThresholdLifeCycleEvent(config, hostname, ipAddress,
                    config.getCpuThreshold(), currentUsage);
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
            double threshold, double currentUsage) {

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
        type.setCondition("Exceeds " + String.valueOf(threshold) + "%");
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

    protected MetricManagerMetric newMetricGatheredEvent(Configuration config, double value) {

        MetricManagerMetric metric = new MetricManagerMetric();
        metric.setId(UUID.randomUUID().toString());
        metric.setResourceID(METRIC_RESOURCE_ID);
        metric.setTimestamp(Instant.now().toEpochMilli());

        Map<String, Double> metrics = new HashMap<>();
        metrics.put(config.getMetricName(), value);
        metric.setMetrics(metrics);

        Map<String, String> attributes = new HashMap<>();
        attributes.put(MetricManagerMetric.RESOURCE_GROUP_FIELD, "CPU");
        attributes.put(MetricManagerMetric.RESOURCE_NODE_FIELD, METRIC_RESOURCE_ID);
        metric.setAttributes(attributes);

        return metric;
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
                _cpuMetricLastGathered.set(System.nanoTime());
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

    protected boolean stringsEqual(String a, String b) {
        if (a == null || b == null)
            return a == b;
        return a.equals(b);
    }

    protected boolean shouldGenerateSampleData(Configuration oldConfig, Configuration newConfig) {
        // If gathering metrics is disabled, skip the checks below
        if (!newConfig.getEnableGatherMetrics())
            return false;

        if (newConfig.getIsLiveData() || oldConfig != null && oldConfig.getIsLiveData() == newConfig.getIsLiveData()
                && stringsEqual(oldConfig.getMetricName(), newConfig.getMetricName())
                && stringsEqual(oldConfig.getHistoricEndDate(), newConfig.getHistoricEndDate())
                && stringsEqual(oldConfig.getHistoricStartDate(), newConfig.getHistoricStartDate())) {
            return false;
        }
        return true;
    }
}
