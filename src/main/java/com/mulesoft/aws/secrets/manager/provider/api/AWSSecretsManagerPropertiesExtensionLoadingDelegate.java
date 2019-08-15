/*
 * (c) 2003-2018 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.mulesoft.aws.secrets.manager.provider.api;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.SELECT;

/**
 * Declares extension for Secure Properties Configuration module
 *
 * @since 1.0
 */
public class AWSSecretsManagerPropertiesExtensionLoadingDelegate implements ExtensionLoadingDelegate {

  public static final String EXTENSION_NAME = "Mule AWS Secrets Manager";
  public static final String CONFIG_ELEMENT = "config";
  public static final String SECRETS_MANAGER_PARAMETER_GROUP = "Secrets Manager";

  @Override
  public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {
    ConfigurationDeclarer configurationDeclarer = extensionDeclarer.named(EXTENSION_NAME)
            .describedAs(String.format("Crafted %s Extension", EXTENSION_NAME))
            .withCategory(SELECT)
            .onVersion("1.0.0")
            .fromVendor("AWS")
            .withConfig(CONFIG_ELEMENT);

    addSecretsManagerParameters(configurationDeclarer);
  }


  /**
   * Add the Basic Connection parameters to the parameter list
   *
   * @param configurationDeclarer Extension {@link ConfigurationDeclarer}
   */
  private void addSecretsManagerParameters(ConfigurationDeclarer configurationDeclarer) {

    ParameterGroupDeclarer addSecretsManagerParametersGroup = configurationDeclarer
            .onParameterGroup(SECRETS_MANAGER_PARAMETER_GROUP)
            .withDslInlineRepresentation(true);

    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    addSecretsManagerParametersGroup
            .withRequiredParameter("region")
            .withDisplayModel(DisplayModel.builder().displayName("AWS Secrets Manager Region").build())
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .describedAs("AWS Secrets Manager region as us-east-2");

    addSecretsManagerParametersGroup
            .withRequiredParameter("accessKey")
            .withDisplayModel(DisplayModel.builder().displayName("AWS Access Key").build())
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .describedAs("AWS Access Key");

    addSecretsManagerParametersGroup
            .withRequiredParameter("secretKey")
            .withDisplayModel(DisplayModel.builder().displayName("AWS Secret Key").build())
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .describedAs("AWS Secret Key");

    addSecretsManagerParametersGroup
            .withRequiredParameter("secretName")
            .withDisplayModel(DisplayModel.builder().displayName("Secret Name").build())
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .describedAs("AWS Secret Name");
  }
}
