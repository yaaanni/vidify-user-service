package io.github.yaaanni.userservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OtpKafkaConfig {

    @Bean
    public KafkaTemplate<String, Object> otpKafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {

        Map<String, Object> props = new HashMap<>(producerFactory.getConfigurationProperties());

        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 3000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);

        ProducerFactory<String, Object> otpProducerFactory =
                new DefaultKafkaProducerFactory<>(props);

        return new KafkaTemplate<>(otpProducerFactory);
    }
}
