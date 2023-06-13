package eu.olympus.credentialapp.utils;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.regex.Pattern;

public class Utils {

    public static boolean checkURL(String url) {
        return android.util.Patterns.WEB_URL.matcher(url).matches();
    }

    // Encrypted file read
    public static byte[] readEncrypted(String fileToRead, Context context) throws GeneralSecurityException, IOException {
        // Although you can define your own key generation parameter specification, it's
        // recommended that you use the value specified here.
        KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
        String masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);

        File credFile=new File(context.getFilesDir(), fileToRead);
        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                credFile,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        InputStream inputStream = encryptedFile.openFileInput();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int nextByte = inputStream.read();
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte);
            nextByte = inputStream.read();
        }


        return byteArrayOutputStream.toByteArray();
    }

    // Encrypted file write
    public static void writeEncrypted(String fileToWrite, byte[] fileContent, Context context) {
        try {
            // Although you can define your own key generation parameter specification, it's
            // recommended that you use the value specified here.
            KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
            String masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);

            // Creates a file with this name, or replaces an existing file
            // that has the same name. Note that the file name cannot contain
            // path separators.
            File credFile=new File(context.getFilesDir(), fileToWrite);
            if (credFile.exists()) { credFile.delete(); }
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    credFile,
                    context,
                    masterKeyAlias,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            OutputStream outputStream = encryptedFile.openFileOutput();
            outputStream.write(fileContent);
            outputStream.flush();
            outputStream.close();
        } catch (GeneralSecurityException e) {
            Log.d("UTILS","Write file", e);
        } catch (IOException e) {
            Log.d("UTILS","Write file", e);
        }
    }
}
