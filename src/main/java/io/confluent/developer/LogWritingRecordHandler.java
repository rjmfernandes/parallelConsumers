package io.confluent.developer;


import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Record handler that writes the Kafka message value to a file.
 */
public class LogWritingRecordHandler extends ConsumerRecordHandler<String, String> {

    private static final Logger log = LoggerFactory.getLogger(LogWritingRecordHandler.class);
    private long waitTime = 0;

    public LogWritingRecordHandler() {

    }

    public LogWritingRecordHandler(long waitTime) {
        this.waitTime = waitTime;
    }

    @Override
    protected void processRecordImpl(final ConsumerRecord<String, String> consumerRecord) {
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                log.error("Error while waiting", e);
            }
        }
        log.info(getNumRecordsProcessed() + " - " + consumerRecord.value());
    }

}
