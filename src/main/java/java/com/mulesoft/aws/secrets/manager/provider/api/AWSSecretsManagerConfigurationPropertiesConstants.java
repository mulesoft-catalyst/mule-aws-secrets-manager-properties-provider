package com.mulesoft.aws.secrets.manager.provider.api;


public final class AWSSecretsManagerConfigurationPropertiesConstants {

    public static final String SECRET_NAME = "secretName";
    public static final String AWS_REGION = "region";
    public static final String AWS_ACCESS_KEY = "accessKey";
    public static final String AWS_SECRET_KEY = "secretKey";
    public static final String AWS_SESSION_TOKEN = "sessionToken";
    public static final String AWS_ROLE_ARN = "roleARN";
    public static final String AWS_CUSTOM_SERVICE_ENDPOINT = "customServiceEndPoint";
    public static final String AWS_USE_DEFAULT_PROVIDER_CHAIN = "useDefaultAWSCredentialsProviderChain";

    public final static String AWS_SECRETS_PREFIX = "aws-secrets::";

    public static final String EXTENSION_NAME = "AWS Secrets Manager Properties Override";
    public static final String CONFIG_ELEMENT = "config";
    public static final String EXTENSION_NAMESPACE =
            EXTENSION_NAME.toLowerCase().replace(" ", "-");

    public static final String SECRETS_MANAGER_PARAMETER_GROUP = "Secrets Manager";
    public static final String SECRETS_MANAGER_PARAMETER_GROUP_NAME =
            SECRETS_MANAGER_PARAMETER_GROUP.toLowerCase().replace(" ", "-");

    public static final String AWS_BASIC_CONNECTION_PARAMETER_GROUP = "Basic Connection";
    public static final String AWS_BASIC_CONNECTION_PARAMETER_GROUP_NAME =
            AWS_BASIC_CONNECTION_PARAMETER_GROUP.toLowerCase().replace(" ", "-");

    public static final String AWS_ROLE_CONNECTION_PARAMETER_GROUP = "Role Connection";
    public static final String AWS_ROLE_CONNECTION_PARAMETER_GROUP_NAME =
            AWS_ROLE_CONNECTION_PARAMETER_GROUP.toLowerCase().replace(" ", "-");

    public static final String AWS_ADVANCED_CONNECTION_PARAMETER_GROUP = "Advanced Connection";
    public static final String AWS_ADVANCED_CONNECTION_PARAMETER_GROUP_NAME =
            AWS_ADVANCED_CONNECTION_PARAMETER_GROUP.toLowerCase().replace(" ", "-");

    public static final String AWS_CURRENT_VERSION = "AWSCURRENT";

}
