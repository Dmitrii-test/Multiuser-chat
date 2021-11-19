package ru.dmitrii.utils.connections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionTest {

    @Test
    void genKey() throws NoSuchAlgorithmException {
            SecretKey secretKey = Encryption.genKey();
            Assertions.assertNotNull(secretKey);
            Assertions.assertEquals("AES", secretKey.getAlgorithm());
    }

    @Test
    void convertSecretKeyToStringException() {
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            Encryption.convertSecretKeyToString(null);
        });
        assertNotNull(thrown.getMessage());
    }

    @Test
    void convertSecretKey() throws NoSuchAlgorithmException {
        SecretKey secretKey = Encryption.genKey();
        String s = Encryption.convertSecretKeyToString(secretKey);
        Assertions.assertEquals(secretKey, Encryption.convertStringToSecretKey(s));
    }

    @Test
    void convertStringToSecretKey() {
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            Encryption.convertStringToSecretKey(null);
        });
        assertNotNull(thrown.getMessage());
    }

    @Test
    void encryptionString() {
        String abc = Encryption.encryptionString("ABC", 0);
        Assertions.assertEquals("ABC", abc);
        String abc2 = Encryption.encryptionString("ABC", 2);
        Assertions.assertNotEquals("ABC", abc2);
    }

    @Test
    void decryptString() {
        String abc = Encryption.decryptString("ABC", 0);
        Assertions.assertEquals("ABC", abc);
        String abc2 = Encryption.decryptString(Encryption.encryptionString("ABC", 2), 2);
        Assertions.assertEquals("ABC", abc2);
    }
}