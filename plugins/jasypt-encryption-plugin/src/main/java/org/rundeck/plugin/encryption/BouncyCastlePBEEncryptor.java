/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

/**
 * Drop-in replacement for Jasypt's StandardPBEByteEncryptor that uses
 * BouncyCastle directly via standard JCE APIs. Produces output that is
 * byte-compatible with Jasypt 1.9.3's format:
 * <pre>
 *   [SALT (16 bytes)] + [CIPHERTEXT]
 * </pre>
 *
 * This eliminates the abandoned Jasypt dependency and its reflective access
 * to JDK internals that breaks under Java 25 strong encapsulation.
 */
public class BouncyCastlePBEEncryptor {

    private static final int SALT_SIZE = 16; // AES block size
    private static final String PROVIDER = "BC";

    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final String algorithm;
    private final char[] password;
    private final int keyObtentionIterations;
    private final SecureRandom random = new SecureRandom();

    public BouncyCastlePBEEncryptor(String algorithm, String password, int keyObtentionIterations) {
        if (algorithm == null || algorithm.isEmpty()) {
            throw new IllegalArgumentException("algorithm is required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }
        this.algorithm = algorithm;
        this.password = password.toCharArray();
        this.keyObtentionIterations = keyObtentionIterations;
    }

    /**
     * Encrypts the given bytes. Output format is Jasypt-compatible:
     * 16-byte random salt prepended to the ciphertext.
     */
    public byte[] encrypt(byte[] plaintext) {
        try {
            byte[] salt = new byte[SALT_SIZE];
            random.nextBytes(salt);

            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, salt);
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Jasypt format: salt + ciphertext
            byte[] output = new byte[salt.length + ciphertext.length];
            System.arraycopy(salt, 0, output, 0, salt.length);
            System.arraycopy(ciphertext, 0, output, salt.length, ciphertext.length);
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts data produced by this encryptor or by Jasypt's
     * StandardPBEByteEncryptor with the same algorithm/password/iterations.
     */
    public byte[] decrypt(byte[] encryptedInput) {
        try {
            if (encryptedInput.length < SALT_SIZE) {
                throw new IllegalArgumentException("Encrypted data too short");
            }

            byte[] salt = Arrays.copyOfRange(encryptedInput, 0, SALT_SIZE);
            byte[] ciphertext = Arrays.copyOfRange(encryptedInput, SALT_SIZE, encryptedInput.length);

            Cipher cipher = createCipher(Cipher.DECRYPT_MODE, salt);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private Cipher createCipher(int mode, byte[] salt) throws Exception {
        PBEKeySpec keySpec = new PBEKeySpec(password);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorithm, PROVIDER);
        SecretKey key = keyFactory.generateSecret(keySpec);

        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, keyObtentionIterations, new IvParameterSpec(new byte[0]));

        Cipher cipher = Cipher.getInstance(algorithm, PROVIDER);
        cipher.init(mode, key, paramSpec);
        return cipher;
    }
}
