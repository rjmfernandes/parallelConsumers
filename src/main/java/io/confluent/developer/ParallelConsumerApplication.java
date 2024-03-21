package io.confluent.developer;


import io.confluent.parallelconsumer.ParallelConsumerOptions;
import io.confluent.parallelconsumer.ParallelStreamProcessor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static io.confluent.parallelconsumer.ParallelConsumerOptions.CommitMode.PERIODIC_CONSUMER_SYNC;
import static io.confluent.parallelconsumer.ParallelConsumerOptions.ProcessingOrder.KEY;
import static io.confluent.parallelconsumer.ParallelStreamProcessor.createEosStreamProcessor;
import static org.apache.commons.lang3.RandomUtils.nextInt;


/**
 * Simple "hello world" Confluent Parallel Consumer application that simply consumes records from Kafka and writes the
 * message values to a file.
 */
public class ParallelConsumerApplication {

    public static final String CONFIGURATION_DEV_PROPERTIES = "./configuration/dev.properties";
    private final ParallelStreamProcessor<String, String> parallelConsumer;
    private final ConsumerRecordHandler<String, String> recordHandler;

    /**
     * Application that runs a given Confluent Parallel Consumer, calling the given handler method per record.
     *
     * @param parallelConsumer the Confluent Parallel Consumer instance
     * @param recordHandler    record handler that implements method to run per record
     */
    public ParallelConsumerApplication(final ParallelStreamProcessor<String, String> parallelConsumer,
                                       final ConsumerRecordHandler<String, String> recordHandler) {
        this.parallelConsumer = parallelConsumer;
        this.recordHandler = recordHandler;
    }

    /**
     * Close the parallel consumer on application shutdown
     */
    public void shutdown() {
        if (parallelConsumer != null) {
            parallelConsumer.close();
        }
    }

    /**
     * Subscribes to the configured input topic and calls (blocking) `poll` method.
     *
     * @param appProperties application and consumer properties
     */
    public void runConsume(final Properties appProperties) {
        String topic = appProperties.getProperty("input.topic.name");

        parallelConsumer.subscribe(Collections.singletonList(topic));

        parallelConsumer.poll(context -> recordHandler.processRecord(context.getSingleConsumerRecord()));
    }

    public static void main(String[] args) throws Exception {
        String configFile = CONFIGURATION_DEV_PROPERTIES;
        if (args.length > 0) {
            configFile = args[0];
        }

        final Properties appProperties = PropertiesUtil.loadProperties(configFile);

        // random consumer group ID for rerun convenience
        String groupId = "parallel-consumer-app-group-" + nextInt();
        appProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // construct parallel consumer
        final Consumer<String, String> consumer = new KafkaConsumer<>(appProperties);
        final ParallelConsumerOptions options = ParallelConsumerOptions.<String, String>builder()
                .ordering(ParallelConsumerOptions.ProcessingOrder.valueOf(
                        appProperties.getProperty("parallel.consumer.order", KEY.name()
                        )))
                .maxConcurrency(
                        Integer.parseInt(
                                appProperties.getProperty("parallel.consumer.max.concurrency", "1")
                        ))
                .commitInterval(Duration.ofSeconds(
                        Long.parseLong(
                                appProperties.getProperty("parallel.consumer.seconds.between.commits", "10")
                        )))
                .consumer(consumer)
                .commitMode(
                        ParallelConsumerOptions.CommitMode.valueOf(
                                appProperties.getProperty("parallel.consumer.commit.mode",
                                        PERIODIC_CONSUMER_SYNC.name())))
                .build();
        ParallelStreamProcessor<String, String> eosStreamProcessor = createEosStreamProcessor(options);

        // create record handler that writes records to configured file
        final String filePath = appProperties.getProperty("file.path");
        //final ConsumerRecordHandler<String, String> recordHandler = new FileWritingRecordHandler(Paths.get(filePath));
        //final ConsumerRecordHandler<String, String> recordHandler = new LogWritingRecordHandler();
        final ConsumerRecordHandler<String, String> recordHandler = new LogWritingRecordHandler(
                Long.parseLong(appProperties.getProperty("record.handler.sleep.ms", "0"))
        );

        // run the consumer!
        final ParallelConsumerApplication consumerApplication = new ParallelConsumerApplication(eosStreamProcessor,
                recordHandler);
        Runtime.getRuntime().addShutdownHook(new Thread(consumerApplication::shutdown));
        consumerApplication.runConsume(appProperties);
    }

}
