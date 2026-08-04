package com.dbtraining.tradeflow.config;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * KafkaConfig — TICKET-I118 (Day 9)
 * ============================================================================
 * WHAT:    Custom Kafka beans wiring in the DLT recoverer, error handler
 *          with retry backoff, ErrorHandlingDeserializer-wrapped consumer
 *          factory, and a pre-declared trade-events.DLT topic.
 * HOW:     Three beans compose the pipeline:
 *          1. consumerFactory() wraps JsonDeserializer inside
 *             ErrorHandlingDeserializer so bad JSON becomes a recoverable
 *             exception instead of poisoning the partition forever.
 *          2. kafkaListenerContainerFactory() attaches the error handler
 *             to every @KafkaListener that references it (I116/I117/I119).
 *          3. kafkaErrorHandler() = DeadLetterPublishingRecoverer +
 *             FixedBackOff(1s, 3). Deserialization + IllegalArgument are
 *             marked non-retryable — those never succeed on retry.
 * WHY:     Without DLT one malformed message wedges the partition and
 *          consumer lag (visible in I121's Grafana panel) climbs until the
 *          broker restarts. addNotRetryableExceptions short-circuits the
 *          3-second retry loop on errors that will never succeed, so
 *          deserialization failures land on DLT instantly.
 * OBSERVE: Publish `{"bad":true}` via kafka-console-producer -> log line
 *          "Record sent to DLT topic trade-events.DLT"; the message appears
 *          on trade-events.DLT in Kafdrop; recon + audit consumers stay silent.
 * ============================================================================
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final String dltTopic;

    public KafkaConfig(KafkaProperties kafkaProperties,
                       @Value("${tradeflow.kafka.topics.dlt:trade-events.DLT}") String dltTopic) {
        this.kafkaProperties = kafkaProperties;
        this.dltTopic = dltTopic;
    }

    /**
     * ConsumerFactory that wraps JsonDeserializer inside
     * ErrorHandlingDeserializer. Malformed JSON now surfaces as a
     * DeserializationException that the DefaultErrorHandler can catch and
     * route to DLT — without the wrapper, the exception would fire inside
     * the poll loop and stall the partition.
     */
    @Bean
    public ConsumerFactory<String, TradeEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dbtraining.tradeflow.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * ListenerContainerFactory referenced by containerFactory=... on the
     * three @KafkaListener consumers (I116/I117/I119). Attaches the common
     * error handler and turns on Micrometer observations for I120's metrics.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeEvent>
    kafkaListenerContainerFactory(ConsumerFactory<String, TradeEvent> consumerFactory,
                                  DefaultErrorHandler errorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TradeEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

    /**
     * DefaultErrorHandler that retries 3 times with 1-second backoff, then
     * hands failing records to the DLT recoverer. Records land on
     * trade-events.DLT keeping the original partition assignment so ordering
     * within a partition is preserved for offline replay.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(1000L, 3));

        // Unrecoverable errors skip the retry loop and go straight to DLT.
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class);
        return handler;
    }

    /**
     * Pre-declared DLT topic — one of the AI-review findings applied via
     * TICKET-I124. Ensures the topic exists before the first poison-pill
     * lands so DeadLetterPublishingRecoverer never fails to write.
     */
    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(dltTopic).partitions(1).replicas(1).build();
    }
}