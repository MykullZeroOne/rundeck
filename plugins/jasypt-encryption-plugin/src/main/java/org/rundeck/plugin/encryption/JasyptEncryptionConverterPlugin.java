/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.plugin.encryption;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.Password;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import com.dtolabs.utils.Streams;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.data.DataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.Security;

/**
 * JasyptEncryptionConverterPlugin is ...
 *
 * @author Greg Schueler &lt;greg@simplifyops.com&gt;
 * @since 2014-03-26
 */
@Plugin(name = JasyptEncryptionConverterPlugin.PROVIDER_NAME, service = ServiceNameConstants.StorageConverter)
@PluginDescription(title = "Jasypt Encryption", description = "Encrypts data in the Rundeck Storage layer\n\n" +
                                                              "This plugin uses Jasypt to perform encryption. The " +
                                                              "built in java JCE is used unless another provider is " +
                                                              "specified, Bouncycastle can be used by specifying the " +
                                                              "'BC' provider name.\n\n" +
                                                              "Password, algorithm, provider, etc can be specified " +
                                                              "directly, or via environment variables (the `*EnvVarName` " +
                                                              "properties), " +
                                                              "or Java System properties (the `*SysPropName` properties)." )


public class JasyptEncryptionConverterPlugin implements StorageConverterPlugin {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    public static final String PROVIDER_NAME = "jasypt-encryption";
    public static final Logger logger        = LoggerFactory.getLogger(JasyptEncryptionConverterPlugin.class);

    @PluginProperty(title = "Encrypter Type",
                    description =
                            "Jasypt Encrypter to use.\n\n" +
                            "Either 'basic', 'strong', or 'custom'. \n\n" +
                            "* 'basic' uses algorithm PBEWithMD5AndDES\n" +
                            "* 'strong' requires use of the JCE Unlimited Strength policy files. (Algorithm: " +
                            "PBEWithMD5AndTripleDES)\n" +
                            "* 'custom' is required to specify algorithm, provider, etc.\n" +
                            "\n" +
                            "Default: 'basic'.",
                    defaultValue = "basic",
                    required = true)
    @SelectValues(values = {"strong", "basic", "custom"})
    String encryptorType;

    @PluginProperty(title = "Password", description = "Encryption password", required = false)
    @Password
    String password;
    @PluginProperty(title = "Password Environment Variable",
                    description = "Name of Environment variable storing Encryption password",
                    required = false)
    String passwordEnvVarName;
    @PluginProperty(title = "Password System Property",
                    description = "Name of JVM System Property storing Encryption password",
                    required = false)
    String passwordSysPropName;

    @PluginProperty(title = "Algorithm", description = "(optional)" )
    String algorithm;
    @PluginProperty(title = "Algorithm Environment Variable", description = "(optional)" )
    String algorithmEnvVarName;
    @PluginProperty(title = "Algorithm System Property", description = "(optional)" )
    String algorithmSysPropName;

    @PluginProperty(title = "Provider Name",
                    description = "Example: 'BC' (specifies bouncycastle)"
    )
    String provider;
    @PluginProperty(title = "Provider Name Environment Variable",
                    description = "(optional)" )
    String providerEnvVarName;
    @PluginProperty(title = "Provider Name System Property",
                    description = "(optional)" )
    String providerSysPropName;

    @PluginProperty(title = "Provider Class Name",
                    description = "Overrides " +
                                  "Provider Name." )
    String providerClassName;

    @PluginProperty(title = "Provider Class Name Environment Variable",
                    description = "Overrides " +
                                  "Provider Name." )
    String providerClassNameEnvVarName;

    @PluginProperty(title = "Provider Class Name System Property",
                    description = "Overrides " +
                                  "Provider Name." )
    String providerClassNameSysPropName;

    @PluginProperty(title = "Key Obtention Iterations",
                    description = "(optional) Number of hash operations on password when generating key, default: " +
                                  "1000." )
    String keyObtentionIterations;
    @PluginProperty(title = "Key Obtention Iterations Environment Variable",
                    description = "(optional)" )
    String keyObtentionIterationsEnvVarName;
    @PluginProperty(title = "Key Obtention Iterations System Property",
                    description = "(optional)" )
    String keyObtentionIterationsSysPropName;

    private static final int DEFAULT_KEY_OBTENTION_ITERATIONS = 1000;

    private volatile BouncyCastlePBEEncryptor encryptor = null;

    public JasyptEncryptionConverterPlugin() {
    }

    private BouncyCastlePBEEncryptor getEncryptor() {
        if (null == encryptor) {
            synchronized (this) {
                if (null == encryptor) {
                    String resolvedPassword = resolveValue(password, passwordEnvVarName, passwordSysPropName, true, "password");
                    // Clear password fields after reading
                    password = null;
                    passwordEnvVarName = null;
                    passwordSysPropName = null;

                    String resolvedAlgorithm;
                    if ("strong".equals(encryptorType)) {
                        logger.debug("JasyptEncryptionConverterPlugin use STRONG type");
                        resolvedAlgorithm = "PBEWithMD5AndTripleDES";
                    } else if ("basic".equals(encryptorType)) {
                        logger.debug("JasyptEncryptionConverterPlugin use BASIC type");
                        resolvedAlgorithm = "PBEWithMD5AndDES";
                    } else if ("custom".equals(encryptorType)) {
                        logger.debug("JasyptEncryptionConverterPlugin use CUSTOM type");
                        resolvedAlgorithm = resolveValue(algorithm, algorithmEnvVarName, algorithmSysPropName, true, "algorithm");
                    } else {
                        throw new IllegalStateException("encryptorType is required");
                    }

                    int iterations = DEFAULT_KEY_OBTENTION_ITERATIONS;
                    String iterStr = resolveValue(keyObtentionIterations, keyObtentionIterationsEnvVarName, keyObtentionIterationsSysPropName, false, "keyObtentionIterations");
                    if (iterStr != null) {
                        iterations = Integer.parseInt(iterStr);
                    }

                    encryptor = new BouncyCastlePBEEncryptor(resolvedAlgorithm, resolvedPassword, iterations);
                    logger.debug("JasyptEncryptionConverterPlugin configured (using BouncyCastle direct)");
                }
            }
        }
        return encryptor;
    }

    /**
     * Resolves a config value from direct value, environment variable, or system property.
     */
    private String resolveValue(String directValue, String envVarName, String sysPropName, boolean required, String description) {
        if (notBlank(directValue)) {
            return directValue;
        } else if (notBlank(envVarName)) {
            String val = System.getenv(envVarName);
            if (notBlank(val)) return val;
        } else if (notBlank(sysPropName)) {
            String val = System.getProperty(sysPropName);
            if (notBlank(val)) {
                System.clearProperty(sysPropName);
                return val;
            }
        }
        if (required) {
            throw new IllegalStateException(description + ", " + description + "EnvVarName, or " + description + "SysPropName is required");
        }
        return null;
    }

    private boolean notBlank(final String value) {
        return null != value && !"".equals(value);
    }

    @Override
    public HasInputStream readResource(
            Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream
            hasInputStream
    )
    {
        if ("true".equals(resourceMetaBuilder.getResourceMeta().get(PROVIDER_NAME + ":encrypted"))) {
            logger.debug("readResource (encrypted) " + path);
            return decrypt(hasInputStream);
        }
        logger.debug("readResource (unencrypted) " + path);
        return null;
    }

    @Override
    public HasInputStream createResource(
            Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream
    )
    {
        resourceMetaBuilder.getResourceMeta().put(PROVIDER_NAME + ":encrypted", "true");
        logger.debug("createResource " + path);
        return encrypt(hasInputStream);
    }

    @Override
    public HasInputStream updateResource(
            Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream
    )
    {

        resourceMetaBuilder.getResourceMeta().put(PROVIDER_NAME + ":encrypted", "true");
        logger.debug("updateResource " + path);
        return encrypt(hasInputStream);
    }

    private HasInputStream encrypt(final HasInputStream hasInputStream) {
        return new EncryptStream(hasInputStream, getEncryptor());
    }

    private HasInputStream decrypt(final HasInputStream hasInputStream) {
        return new DecryptStream(hasInputStream, getEncryptor());
    }


    private static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Streams.copyStream(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    private static class EncryptStream implements HasInputStream {
        private final HasInputStream hasInputStream;
        private final BouncyCastlePBEEncryptor encryptor;

        private EncryptStream(HasInputStream hasInputStream, BouncyCastlePBEEncryptor encryptor) {
            this.hasInputStream = hasInputStream;
            this.encryptor = encryptor;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(encryptor.encrypt(getBytes(hasInputStream.getInputStream())));
        }

        @Override
        public long writeContent(OutputStream outputStream) throws IOException {
            return DataUtil.copyStream(getInputStream(), outputStream);
        }
    }

    private static class DecryptStream implements HasInputStream {
        private final HasInputStream hasInputStream;
        private final BouncyCastlePBEEncryptor encryptor;

        private DecryptStream(HasInputStream hasInputStream, BouncyCastlePBEEncryptor encryptor) {
            this.hasInputStream = hasInputStream;
            this.encryptor = encryptor;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                return new ByteArrayInputStream(encryptor.decrypt(getBytes(hasInputStream.getInputStream())));
            } catch (RuntimeException e) {
                throw new IOException("Decryption failed.", e);
            }
        }

        @Override
        public long writeContent(OutputStream outputStream) throws IOException {
            return DataUtil.copyStream(getInputStream(), outputStream);
        }
    }
}
