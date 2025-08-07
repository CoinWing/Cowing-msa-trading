package cowing.project.cowingmsatrading.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.orderbook.host}")
    private String ORDERBOOK_REDIS_HOST;

    @Value("${spring.data.redis.orderbook.port}")
    private int ORDERBOOK_REDIS_PORT;

    @Value("${spring.data.redis.orderbook.password}")
    private String ORDERBOOK_REDIS_PASSWORD;

    @Value("${spring.data.redis.pending.host}")
    private String PENDING_REDIS_HOST;

    @Value("${spring.data.redis.pending.port}")
    private int PENDING_REDIS_PORT;

    @Value("${spring.data.redis.pending.password}")
    private String PENDING_REDIS_PASSWORD;


    @Primary
    @Bean("orderRedisConnectionFactory")
    public LettuceConnectionFactory orderbookRedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(ORDERBOOK_REDIS_HOST, ORDERBOOK_REDIS_PORT);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(ORDERBOOK_REDIS_PASSWORD));
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean("pendingRedisConnectionFactory")
    public LettuceConnectionFactory pendingRedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(PENDING_REDIS_HOST, PENDING_REDIS_PORT);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(PENDING_REDIS_PASSWORD));
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Primary
    @Bean("orderbookRedisTemplate")
    public RedisTemplate<String, String> orderbookRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(orderbookRedisConnectionFactory());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean("pendingRedisTemplate")
    public RedisTemplate<String, Object> pendingRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(pendingRedisConnectionFactory());

        // JSON 직렬화 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setDefaultSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        return template;
    }
}
