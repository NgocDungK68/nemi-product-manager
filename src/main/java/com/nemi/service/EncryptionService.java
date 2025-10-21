package com.nemi.service;

import com.nemi.utils.crypto.AesCipher;
import com.nemi.utils.crypto.model.CipherText;
import com.nemi.utils.crypto.model.Password;
import com.nemi.utils.crypto.model.PlainText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nemi.configuration.CryptoConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EncryptionService {
    private final CryptoConfig cryptoConfig;

    private AesCipher getAesCipher() {
        return new AesCipher(cryptoConfig.getMode(), cryptoConfig.getStrength());
    }

    /**
     * Encrypt access token before storing in database
     */
    public String encrypt(String data) {
        if (data == null || data.trim().isEmpty()) {
            log.warn("Access token is null or empty, cannot encrypt");
            return data;
        }

        try {
            AesCipher cipher = getAesCipher();
            PlainText plainText = new PlainText(data);
            Password password = new Password(cryptoConfig.getPassword());
            
            CipherText encryptedToken = cipher.encrypt(plainText, password);
            log.debug("Successfully encrypted access token");
            return encryptedToken.getValue();
        } catch (Exception e) {
            log.error("Failed to encrypt access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to encrypt access token", e);
        }
    }

    /**
     * Decrypt access token when using for API calls
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            log.warn("Encrypted access token is null or empty, cannot decrypt");
            return encryptedData;
        }

        try {
            AesCipher cipher = getAesCipher();
            CipherText cipherText = new CipherText(encryptedData);
            Password password = new Password(cryptoConfig.getPassword());
            
            PlainText decryptedToken = cipher.decrypt(cipherText, password);
            log.debug("Successfully decrypted access token");
            return decryptedToken.getValue();
        } catch (Exception e) {
            log.error("Failed to decrypt access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt access token", e);
        }
    }
}
