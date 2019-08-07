/*
 * (c) 2003-2018 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.mulesoft.aws.secrets.manager.provider.api;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.mulesoft.aws.secrets.manager.provider.api.AWSSecretsManagerPropertiesExtensionLoadingDelegate.*;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;

/**
 * Builds the provider for a custom-properties-provider:config element.
 *
 * @since 1.0
 */
public class AWSSecretsManagerConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

  public static final String EXTENSION_NAMESPACE =
          EXTENSION_NAME.toLowerCase().replace(" ", "-");
  public static final String SECRETS_MANAGER_PARAMETER_GROUP_NAME =
          SECRETS_MANAGER_PARAMETER_GROUP.toLowerCase().replace(" ", "-");
  private static final ComponentIdentifier CUSTOM_PROPERTIES_PROVIDER =
          builder().namespace(EXTENSION_NAMESPACE).name(CONFIG_ELEMENT).build();

  private final static Logger LOGGER = LoggerFactory.getLogger(AWSSecretsManagerConfigurationPropertiesProvider.class);

  @Override
  public ComponentIdentifier getSupportedComponentIdentifier() {
    return CUSTOM_PROPERTIES_PROVIDER;
  }

  @Override
  public ConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters, ResourceProvider externalResourceProvider) {
    try {
      return new AWSSecretsManagerConfigurationPropertiesProvider(getAWSSecretsManager(parameters));
    } catch (Exception ve) {
      LOGGER.error("Error connecting to AWS Secrets Manager", ve);
      return null;
    }
  }

  private AWSSecretsManager getAWSSecretsManager(ConfigurationParameters parameters) {

    String region = null;
    String endPoint = null;
    String accessKey = null;
    String secretKey = null;
    AWSSecretsManager client = null;

    List<ConfigurationParameters> secretManagersList = parameters
            .getComplexConfigurationParameter(builder()
                    .namespace(EXTENSION_NAMESPACE)
                    .name(SECRETS_MANAGER_PARAMETER_GROUP_NAME).build());

    ConfigurationParameters temp = secretManagersList.get(0);

    try {
      accessKey = temp.getStringParameter("region");
    } catch (Exception e) {
      LOGGER.error("region parameter not present");
      throw new RuntimeException("region parameter not present");
    }
    try {
      accessKey = temp.getStringParameter("accessKey");
    } catch (Exception e) {
      LOGGER.error("accessKey parameter not present");
      throw new RuntimeException("accessKey parameter not present");
    }
    try {
      secretKey = temp.getStringParameter("secretKey");
    } catch (Exception e) {
      LOGGER.error("secretKey parameter not present");
      throw new RuntimeException("secretKey parameter not present");
    }

    endPoint = "secretsmanager." + region + ".amazonaws.com";

    BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

    AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration(endPoint, region);
    AWSSecretsManagerClientBuilder clientBuilder = AWSSecretsManagerClientBuilder
                                                  .standard()
                                                  .withCredentials(new AWSStaticCredentialsProvider(awsCreds));
    clientBuilder.setEndpointConfiguration(config);
    client = clientBuilder.build();

    return client;
  }

}
