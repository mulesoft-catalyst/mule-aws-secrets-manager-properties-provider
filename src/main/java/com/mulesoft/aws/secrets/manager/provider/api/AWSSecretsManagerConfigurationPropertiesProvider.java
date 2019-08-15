package com.mulesoft.aws.secrets.manager.provider.api;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.*;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationProperty;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AWSSecretsManagerConfigurationPropertiesProvider implements ConfigurationPropertiesProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(AWSSecretsManagerConfigurationPropertiesProvider.class);

    private final static String AWS_SECRETS_PREFIX = "aws-secrets::";

    private final static Pattern AWS_SECRETS_PATTERN = Pattern.compile("\\$\\{" + AWS_SECRETS_PREFIX + "[^}]*}");

    private final AWSSecretsManager secretsManager;

    private final String secretName;

    public AWSSecretsManagerConfigurationPropertiesProvider(AWSSecretsManager secretsManager, String secretName) {
        this.secretsManager = secretsManager;
        this.secretName = secretName;
    }

    @Override
    public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
        LOGGER.error("==============================" + configurationAttributeKey);
        String modifiedText = configurationAttributeKey;
        LOGGER.error("==============================" + modifiedText);
        if (modifiedText.startsWith(AWS_SECRETS_PREFIX)) {
            final String effectiveKey = modifiedText.substring(AWS_SECRETS_PREFIX.length());
            LOGGER.error("==============================" + effectiveKey);
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

        /*String[] temp = secretNameKeyPair.split(":");

        if (temp.length != 2) {
            throw new RuntimeException("Number of parameters required is 2");
        }

        String secretName = temp[0];
        String key = temp[1];

        LOGGER.error("Secret Name ==============================" + secretName);*/
        String key = secretKey;
        LOGGER.error("Key ==============================" + key);

        String secret;
        ByteBuffer binarySecretData;
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                .withSecretId(secretName).withVersionStage("AWSCURRENT");
        GetSecretValueResult getSecretValueResult = null;
        try {
            getSecretValueResult = secretsManager.getSecretValue(getSecretValueRequest);

        } catch(ResourceNotFoundException e) {
            LOGGER.error("The secret was not found", e.getMessage());
            throw new ResourceNotFoundException("The requested secret " + secretName + " was not found");
        } catch (InvalidRequestException e) {
            LOGGER.error("The request was invalid due to: ", e.getMessage());
            throw new InvalidRequestException("The request was invalid due to: " + e.getMessage());
        } catch (InvalidParameterException e) {
            LOGGER.error("The secret was not found", e.getMessage());
            throw new InvalidParameterException("The request had invalid params: " + e.getMessage());
        }

        if(getSecretValueResult == null) {
            return null;
        }

        Map<String, String> secretMap = new HashMap();

        if(getSecretValueResult.getSecretString() != null) {
            secret = getSecretValueResult.getSecretString();
            secretMap = returnSecrets(secret);
        }
        else {
            binarySecretData = getSecretValueResult.getSecretBinary();
            secret = binarySecretData.toString();
            secretMap = returnSecrets(secret);
        }

        return secretMap.get(key);
    }

    private static Map<String, String> returnSecrets (String secret) {
        String sample = secret.replaceAll("\"","");
        sample = sample.replace("{", "");
        sample = sample.replace("}", "");
        Map<String, String> secretMap = (HashMap<String, String>)
                Arrays.asList(sample.split(",")).stream().map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0],
                        e -> e[1]));
        return secretMap;
    }
}
