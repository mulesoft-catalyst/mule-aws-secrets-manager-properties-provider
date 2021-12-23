/*
 * (c) 2003-2018 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.mulesoft.aws.secrets.manager.provider.api;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.List;


import static com.mulesoft.aws.secrets.manager.provider.api.AWSSecretsManagerConfigurationPropertiesConstants.*;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;

/**
 * Builds the provider for a custom-properties-provider:config element.
 *
 * @since 1.0
 */
public class AWSSecretsManagerConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

  private static final ComponentIdentifier CUSTOM_PROPERTIES_PROVIDER =
          builder().namespace(EXTENSION_NAMESPACE).name(CONFIG_ELEMENT).build();

  private final static Logger logger = LoggerFactory.getLogger(AWSSecretsManagerConfigurationPropertiesProvider.class);

  @Override
  public ComponentIdentifier getSupportedComponentIdentifier() {
    return CUSTOM_PROPERTIES_PROVIDER;
  }

  @Override
  public ConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters, ResourceProvider externalResourceProvider) {
    List<ConfigurationParameters> secretManagersList = parameters
            .getComplexConfigurationParameter(builder()
                    .namespace(EXTENSION_NAMESPACE)
                    .name(SECRETS_MANAGER_PARAMETER_GROUP_NAME).build());

    ConfigurationParameters smParams = secretManagersList.get(0);

    List<ConfigurationParameters> basicConnectionsList = parameters
            .getComplexConfigurationParameter(builder()
                    .namespace(EXTENSION_NAMESPACE)
                    .name(AWS_BASIC_CONNECTION_PARAMETER_GROUP_NAME).build());

    ConfigurationParameters basicConnectionParams = basicConnectionsList.get(0);

    List<ConfigurationParameters> roleConnectionsList = parameters
            .getComplexConfigurationParameter(builder()
                    .namespace(EXTENSION_NAMESPACE)
                    .name(AWS_ROLE_CONNECTION_PARAMETER_GROUP_NAME).build());

    ConfigurationParameters roleConnectionParams = roleConnectionsList.get(0);

    List<ConfigurationParameters> advancedConnectionsList = parameters
            .getComplexConfigurationParameter(builder()
                    .namespace(EXTENSION_NAMESPACE)
                    .name(AWS_ADVANCED_CONNECTION_PARAMETER_GROUP_NAME).build());

    ConfigurationParameters advConnectionsParams = advancedConnectionsList.get(0);


    String secretName = getStringParameter (smParams, SECRET_NAME);

    String region = getStringParameter( basicConnectionParams,AWS_REGION).toLowerCase();
    String accessKey = getStringParameter(basicConnectionParams, AWS_ACCESS_KEY);
    String secretKey = getStringParameter(basicConnectionParams, AWS_SECRET_KEY);
    String sessionToken = getStringParameter(basicConnectionParams, AWS_SESSION_TOKEN);

    String roleARN = getStringParameter(roleConnectionParams, AWS_ROLE_ARN);

    String customEndpoint = getStringParameter(advConnectionsParams, AWS_CUSTOM_SERVICE_ENDPOINT);
    boolean useDefaultAWSCredentialsProviderChain = Boolean.parseBoolean(getStringParameter(advConnectionsParams, AWS_USE_DEFAULT_PROVIDER_CHAIN));

    if (!useDefaultAWSCredentialsProviderChain &&
            (StringUtils.isEmpty(accessKey) || StringUtils.isEmpty(secretKey))) {
      String errMsg = "Access Key and Secret Key is Required if Default AWS Credentials Provider Chain is not used";
      logger.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (StringUtils.isNotEmpty(roleARN) &&
            (useDefaultAWSCredentialsProviderChain ||
                    (StringUtils.isNotEmpty(accessKey) && StringUtils.isNotEmpty(secretKey))
            )
    ) {
      String errMsg = "Role based connection requires either Default AWS Credentials Provider Chain or Access/Secret Key";
      logger.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    logger.debug ("AWS SM Provider Creation for Secret: {}, region: {}", secretName,region);
    try {
      return new AWSSecretsManagerConfigurationPropertiesProvider(createAWSSecretsManager(region, accessKey,secretKey, sessionToken, customEndpoint, useDefaultAWSCredentialsProviderChain, roleARN), secretName);
    } catch (Exception ve) {
      logger.error("Error connecting to AWS Secrets Manager", ve);
      return null;
    }
  }

  private String getStringParameter (ConfigurationParameters params, String parameterName) {
    try {
      String value = params.getStringParameter(parameterName);
      return value;
    } catch (Exception e) {
      String errMsg = parameterName + " parameter not present";
      logger.error(errMsg);
      throw new RuntimeException(errMsg);
    }
  }

  private SecretsManagerClient createAWSSecretsManager(String region,  String accessKey, String secretKey,
                                                       String sessionToken, String customEndpoint, boolean useDefaultAWSCredentialsProviderChain, String roleARN) {

    logger.debug ("Region: {}, Custom Endpoint: [{}], Try Default Credentials Provider Chain: {}, RoleARN: {}", region,
            customEndpoint, useDefaultAWSCredentialsProviderChain, roleARN);

    URI endpoint = getEndpoint (region, customEndpoint);

    AwsCredentialsProvider awsCredentialsProvider = getAWSCredentialsProvider(region, accessKey, secretKey,
              sessionToken, useDefaultAWSCredentialsProviderChain, roleARN, endpoint);

    SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                                          .region(Region.of(region))
                                          .endpointOverride(endpoint)
                                          .credentialsProvider(awsCredentialsProvider)
                                          .build();
    return secretsClient;
  }

  private URI getEndpoint (String region, String customEndpoint) {
    URI endpoint = null;

    try {
      if (StringUtils.isNotEmpty(customEndpoint)) {
        logger.debug ("Creating an Endpoint with the Custom Endpoint: {}", customEndpoint);
        endpoint = new URI("https://" + customEndpoint);
      }
      else {
        String regionEndpoint = "https://secretsmanager." + region + ".amazonaws.com";
        logger.debug ("Creating an Endpoint for the RegionEndpoint: {}", regionEndpoint);
        endpoint = new URI(regionEndpoint);
      }
    } catch (Exception e) {
      String errMsg = "Invalid Endpoint";
      logger.error(errMsg);
      throw new RuntimeException(errMsg);
    }
    return endpoint;
  }

  private AwsCredentialsProvider getAWSCredentialsProvider (String region,  String accessKey, String secretKey, String sessionToken,
                                                            boolean useDefaultAWSCredentialsProviderChain, String roleARN, URI endpoint) {

    AwsCredentialsProvider awsCredentialsProvider = null;
    if (useDefaultAWSCredentialsProviderChain) {
      logger.debug("Creating the Default AWS Credentials Provider Chain");
      awsCredentialsProvider = DefaultCredentialsProvider.create();
    }
    else {
      AwsCredentials awsCredentials = null;
      if (StringUtils.isNotEmpty(sessionToken)) {
        logger.debug("Creating Session Credentials");
        awsCredentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
      } else {
        logger.debug("Creating Basic Credentials");
        awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
      }
      logger.debug("Creating Static AWS Credentials Provider");
      awsCredentialsProvider = StaticCredentialsProvider.create(awsCredentials);
    }

    if (StringUtils.isNotEmpty(roleARN)) {
      logger.debug("Role ARN is not Empty - {}", roleARN);
      StsClient stsClient = StsClient.builder()
              .credentialsProvider(awsCredentialsProvider)
              .region(Region.of(region))
              .build();
      try {
        AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                .roleArn(roleARN)
                .roleSessionName("WithRoleARN")
                .build();

        AssumeRoleResponse roleResponse = stsClient.assumeRole(roleRequest);

        Credentials roleCreds = roleResponse.credentials();
        // Display the time when the temp creds expire
        Instant exTime = roleCreds.expiration();
        sessionToken = roleCreds.sessionToken();
        accessKey = roleCreds.accessKeyId();
        secretKey = roleCreds.secretAccessKey();
        logger.debug("Assume Role Response received with access Key - {} with expiration - {}", accessKey, exTime.toString());

        AwsCredentials awsCredentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
        awsCredentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
        logger.debug("Creating Static AWS Credentials Provider for Role: {}", roleARN);
        awsCredentialsProvider = StaticCredentialsProvider.create(awsCredentials);
      } catch (StsException e) {
        String errMsg = e.getMessage();
        logger.error(errMsg);
        throw new RuntimeException(errMsg);
      }
    }
    return awsCredentialsProvider;
  }
}
