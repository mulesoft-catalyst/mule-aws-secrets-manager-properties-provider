package com.mulesoft.aws.secrets.manager.provider.api;


import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationProperty;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static com.mulesoft.aws.secrets.manager.provider.api.AWSSecretsManagerConfigurationPropertiesConstants.*;
import static com.mulesoft.aws.secrets.manager.provider.api.AWSSecretsManagerConfigurationPropertiesConstants.AWS_SECRETS_PREFIX;
import static com.mulesoft.aws.secrets.manager.provider.api.AWSSecretsManagerConfigurationPropertiesConstants.REAL_TIME_PREFIX;

public class AWSSecretsManagerConfigurationPropertiesProvider implements ConfigurationPropertiesProvider {

    private final static Logger logger = LoggerFactory.getLogger(AWSSecretsManagerConfigurationPropertiesProvider.class);

    private final static Pattern AWS_SECRETS_PATTERN = Pattern.compile("\\$\\{" + AWS_SECRETS_PREFIX + "[^}]*}");

    private Map <String, String> secretsCache = new ConcurrentHashMap<>();

    private final SecretsManagerClient secretsManagerClient;

    private final String secretName;

    private final MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class);

    public AWSSecretsManagerConfigurationPropertiesProvider(SecretsManagerClient secretsManagerClient, String secretName) {
        this.secretsManagerClient = secretsManagerClient;
        this.secretName = secretName;
    }

    @Override
    public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
        if (logger.isDebugEnabled())
            logger.debug ("ConfigAttributeKey: {}", configurationAttributeKey);
        String modifiedText = configurationAttributeKey;

        if (modifiedText.startsWith(AWS_SECRETS_PREFIX)) {
            final String effectiveKey = modifiedText.substring (AWS_SECRETS_PREFIX.length());
            if (logger.isDebugEnabled())
                logger.debug ("Effective Key: {}" , effectiveKey);
            try {
                final String value = getSecret(effectiveKey);
                if (value != null) {
                    return Optional.of(new ConfigurationProperty() {

                        @Override
                        public Object getSource() {
                            return "AWS Secrets Manager";
                        }

                        @Override
                        public Object getRawValue() {
                            return value;
                        }

                        @Override
                        public String getKey() {
                            return effectiveKey;
                        }
                    });
                }
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public String getDescription() {
        return "AWS Secrets Manager Properties Provider";
    }

    @DisplayName("Get Secret")
    private String getSecret (String secretKey) {
        boolean realTimeFetch = false;

        if (secretKey.startsWith(REAL_TIME_PREFIX))
            realTimeFetch = true;

        if (realTimeFetch) {
            secretKey = secretKey.substring(REAL_TIME_PREFIX.length());
            if (logger.isDebugEnabled())
                logger.debug ("real-time fetch for " + secretKey);
        } else {
            if (this.secretsCache.containsKey (secretKey) ) {
                if (logger.isDebugEnabled())
                    logger.debug ("Cache Hit for key: {}" , secretKey);
                return getValueFromCache(secretKey);
            }
        }

        if (logger.isDebugEnabled())
            logger.debug ("AWS SM Lookup for Key  {} within secret Id: {}" , secretKey, this.secretName);

        GetSecretValueResponse valueResponse = null;
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(this.secretName)
                    .versionStage(AWS_CURRENT_VERSION)
                    .build();
            valueResponse = secretsManagerClient.getSecretValue(valueRequest);
        } catch(SecretsManagerException e) {
            logger.error("Failed to Get Secret from AWS SM -- {} ", e.getMessage());
            throw new RuntimeException ("The requested secret " + secretName + " was not found");
        }

        if(valueResponse == null) {
            logger.error("Failed to Get Secret from AWS SM - valueResponse is empty ");
            return null;
        }

        String secret = null;

        if(valueResponse.secretString() != null) {
            secret = valueResponse.secretString();
        }
        else {
            secret = new String (
                    Base64.getDecoder().decode(
                            valueResponse.secretBinary().asByteBuffer()
                    ).array()
            );
        }

        if (realTimeFetch) {
            ObjectMapper mapper = new ObjectMapper ();
            Map <String, String> secretsCacheTemp = new ConcurrentHashMap<>();
            try {
                Map<String, String> interimMap =  mapper.readValue (secret, mapType);
                secretsCacheTemp = interimMap;
            } catch (IOException e) {
                logger.error("Failed to Refresh the Cache -- {} ", e.toString());
                return null;
            }
            return secretsCacheTemp.get(secretKey);
        } else {
            updateSecretCache (secret);
            return getValueFromCache(secretKey);
        }
    }

    private String getValueFromCache (String key) {
        return this.secretsCache.get(key);
    }

    private void updateSecretCache (String secretData) {
        ObjectMapper mapper = new ObjectMapper ();
        try {
            Map<String, String> interimMap =  mapper.readValue (secretData, mapType);
            secretsCache = interimMap;
        } catch (IOException e) {
            logger.error("Failed to Refresh the Cache -- {} ", e.toString());
            throw new RuntimeException("Failed to Refresh the Cache from AWS SM");
        }
    }
}
