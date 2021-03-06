package ru.dmitrii.utils.connections;

import org.jetbrains.annotations.NotNull;

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
    protected static String convertSecretKeyToString(@NotNull SecretKey secretKey) {
        byte[] rawData = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(rawData);
    }

    /**
     * Метод получения ключа из строки
     *
     * @param encodedKey String
     * @return SecretKey
     */
    @NotNull
    protected static SecretKey convertStringToSecretKey(@NotNull String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }


    /**
     * Метод шифрования строки шифром Цезаря
     *
     * @param str String
     * @param k int Количество сдвигов
     * @return String
     */
    @NotNull
    protected static String encryptionString(@NotNull String str, int k) {
        return passString(str, k);
    }


    /**
     * Метод расшифровки строки шифром Цезаря
     *
     * @param str String
     * @param n int Количество сдвигов
     * @return SecretKey
     */
    @NotNull
    protected static String decryptString(@NotNull String str, int n) {
        int k=Integer.parseInt("-"+n);
        return passString(str, k);
    }

    /**
     * Обход строки шифром цезаря
     * @param str String
     * @param k int Количество сдвигов
     * @return String
     */
    @NotNull
    private static String passString(@NotNull String str, int k) {
        StringBuilder string= new StringBuilder();
        for(int i=0;i<str.length();i++) {
            char c=str.charAt(i);
            if(c>='a'&&c<='z')// Если символ в строке строчный
            {
                c+=k%26;// ключ% 26 бит
                if(c<'a')
                    c+=26;// слева налево
                if(c>'z')
                    c-=26;// направо
            }else if(c>='A'&&c<='Z')// Если символ в строке в верхнем регистре
            {
                c+=k%26;// ключ% 26 бит
                if(c<'A')
                    c+=26;// слева налево
                if(c>'Z')
                    c-=26;// направо
            }
            string.append(c);
        }
        return string.toString();
    }
}
