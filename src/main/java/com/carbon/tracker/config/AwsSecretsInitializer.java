package com.carbon.tracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.HashMap;
import java.util.Map;

public class AwsSecretsInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        // Only fetch secrets if the "prod" profile is active
        if (!environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"))) {
            return;
        }

        try {
            String secretName = "biometrck/prod/credentials";
            String regionName = environment.getProperty("AWS_REGION", "us-east-1");

            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(Region.of(regionName))
                    .build();

            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
            String secretString = getSecretValueResponse.secretString();

            @SuppressWarnings("unchecked")
            Map<String, Object> secrets = new ObjectMapper().readValue(secretString, HashMap.class);

            Map<String, Object> propertyMap = new HashMap<>();
            // Map the retrieved keys to spring datasource properties
            propertyMap.put("spring.datasource.url", secrets.get("SPRING_DATASOURCE_URL"));
            propertyMap.put("spring.datasource.username", secrets.get("SPRING_DATASOURCE_USERNAME"));
            propertyMap.put("spring.datasource.password", secrets.get("SPRING_DATASOURCE_PASSWORD"));
            if (secrets.containsKey("JWT_SECRET")) {
                propertyMap.put("jwt.secret", secrets.get("JWT_SECRET"));
            }

            environment.getPropertySources().addFirst(new MapPropertySource("awsSecretsProperties", propertyMap));
            System.out.println("Successfully loaded application credentials from AWS Secrets Manager: " + secretName);
        } catch (Exception e) {
            System.err.println("Failed to load credentials from AWS Secrets Manager. falling back to environment variables: " + e.getMessage());
        }
    }
}
