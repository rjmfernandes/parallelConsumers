# Parallel Consumers

This guide is inspired directly on https://developer.confluent.io/tutorials/confluent-parallel-consumer/kafka.html

Start kafka (single broker):

```bash
docker compose up -d
```

Create the topic:

```bash
kafka-topics --create --topic parallel-consumer-input-topic --bootstrap-server broker:29092 --replication-factor 1 --partitions 1
```

It has a single partition cause for our demonstration purposes thats enough.

Lets populate our topic with data:

```bash
kafka-producer-perf-test  \
    --producer-props bootstrap.servers=localhost:29092 key.serializer=org.apache.kafka.common.serialization.StringSerializer value.serializer=org.apache.kafka.common.serialization.StringSerializer\
        --topic parallel-consumer-input-topic \
    --record-size 1024 \
    --throughput 10000 \
    --num-records 1000000
```

Check [dev.propertes](./configuration/dev.properties):

```
# Consumer properties
bootstrap.servers=localhost:29092
key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
max.poll.interval.ms=300000
enable.auto.commit=false
auto.offset.reset=earliest

# increase max.poll.records from default of 500 to ensure we can download many records
max.poll.records=10000
# large fetch.min.bytes to optimize for throughput
fetch.min.bytes=100000

# parallel consumer properties

parallel.consumer.max.concurrency=256
parallel.consumer.order=UNORDERED
parallel.consumer.commit.mode=PERIODIC_CONSUMER_ASYNCHRONOUS
parallel.consumer.seconds.between.commits=60

# Application-specific properties
input.topic.name=parallel-consumer-input-topic
file.path=topic-output.txt
record.handler.sleep.ms=100
```

We will use this configuration file for both standard and parallel consumers.

We will be using for both cases a basic RecordHandler that will log a sequential number for the record processed and its value: [src/main/java/io/confluent/developer/LogWritingRecordHandler.java](src/main/java/io/confluent/developer/LogWritingRecordHandler.java). But as per our configuration we will also wait 100ms per record simulating a record that takes some time to process.

Run first the [./src/main/java/io/confluent/developer/StandardConsumer.java](src/main/java/io/confluent/developer/StandardConsumer.java). After one minute stop and check which was the last/biggest sequential record number logged. 

After run the [src/main/java/io/confluent/developer/ParallelConsumerApplication.java](src/main/java/io/confluent/developer/ParallelConsumerApplication.java) (which as per our configuration will have max concurrency 256, will consume records unordered and with periodic assynchronous commits each 60s). Again after one minute stop and check now the last sequential number logged. You should find a number considerably bigger than the one before.
