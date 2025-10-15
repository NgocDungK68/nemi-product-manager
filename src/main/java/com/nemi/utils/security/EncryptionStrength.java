package com.nemi.utils.security;

/**
 *
 * Three possible options for an AES key length.
 */
public enum EncryptionStrength {
    BIT_128(128),
    BIT_192(192),
    BIT_256(256);

    private final int length;

    EncryptionStrength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public static EncryptionStrength getAESKeyLength(int encryptionStrength) {
        return switch (encryptionStrength) {
            case 128 -> EncryptionStrength.BIT_128;
            case 192 -> EncryptionStrength.BIT_192;
            default -> EncryptionStrength.BIT_256;
        };
    }
}