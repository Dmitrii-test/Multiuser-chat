package ru.dmitrii.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// Класс шифрования
public class Encryption {

    /**
     * Метод генерации ключа
     *
     * @return String
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    protected static SecretKey genKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(192);
        return keyGenerator.generateKey();
    }

    /**
     * Метод получения строки из ключа
     *
     * @param secretKey SecretKey
     * @return String String
     */

    protected static String convertSecretKeyToString(SecretKey secretKey) {
        byte[] rawData = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(rawData);
    }

    /**
     * Метод получения ключа из строки
     *
     * @param encodedKey String
     * @return SecretKey
     */

    protected static SecretKey convertStringToSecretKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}
