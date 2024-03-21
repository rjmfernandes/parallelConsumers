package io.confluent.developer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import static org.apache.commons.lang3.RandomUtils.nextInt;

public class StandardConsumer {

    public static final String CONFIGURATION_DEV_PROPERTIES = "./configuration/dev.properties";
    public static final long POLL_DURATION_MS = 300;
    private static final String CONSUMER_GROUP_ID = "standard-consumer-group-";

    /**
     * Reads from topic.
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Properties appProperties = PropertiesUtil.loadProperties(CONFIGURATION_DEV_PROPERTIES);
        appProperties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID+nextInt());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(appProperties);
        String topic = appProperties.getProperty("input.topic.name");

        final String filePath = appProperties.getProperty("file.path");
        //final ConsumerRecordHandler<String, String> recordHandler = new FileWritingRecordHandler(Paths.get(filePath));
        //final ConsumerRecordHandler<String, String> recordHandler = new LogWritingRecordHandler();
        final ConsumerRecordHandler<String, String> recordHandler = new LogWritingRecordHandler(
                Long.parseLong(appProperties.getProperty("record.handler.sleep.ms", "0"))
        );

        consumer.subscribe(Collections.singletonList(topic));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(
                        Duration.ofMillis(POLL_DURATION_MS)
                );
                for (ConsumerRecord<String, String> record : records)
                    recordHandler.processRecord(record);
            }
        } finally {
            consumer.close();
        }

    }
}
