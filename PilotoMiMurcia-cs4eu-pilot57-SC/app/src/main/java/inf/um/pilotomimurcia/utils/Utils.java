package inf.um.pilotomimurcia.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import eu.olympus.model.AttributeType;
import eu.olympus.model.Policy;
import eu.olympus.model.Predicate;
import inf.um.pilotomimurcia.MenuPrincipal;
import inf.um.pilotomimurcia.R;
import inf.um.pilotomimurcia.exceptions.CapManCommunicationException;
import inf.um.pilotomimurcia.miMurcia.ServiceInfoModel;
import inf.um.pilotomimurcia.rest.CapManAPIService;
import retrofit2.Call;
import retrofit2.Response;

public class Utils {
    public static final String formatRFC3339UTC = "yyyy-MM-dd'T'HH:mm:ss";

    // Encrypted sharedPreferences
    public static SharedPreferences secureSharedPreferences(Context context) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                "accountData",
                "Vgkq3lirFC", // Random
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        return sharedPreferences;
    }

    // Encrypted file read
    public static byte[] readEncrypted(String fileToRead, Context context) throws GeneralSecurityException, IOException {
        // Although you can define your own key generation parameter specification, it's
        // recommended that you use the value specified here.
        KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
        String masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);

        File credFile = new File(context.getFilesDir(), fileToRead);
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
            File credFile = new File(context.getFilesDir(), fileToWrite);
            if (credFile.exists()) {
                credFile.delete();
            }
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
            Log.d("UTILS", "Write file", e);
        } catch (IOException e) {
            Log.d("UTILS", "Write file", e);
        }
    }

    public static void longLog(String TAG, String message) {
        int maxLogSize = 2000;
        for(int i = 0; i <= message.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i+1) * maxLogSize;
            end = Math.min(end, message.length());
            Log.d(TAG, message.substring(start, end));
        }
    }

    public static  List<ServiceInfoModel> retrieveServices(Context context) {
        Gson gson = new Gson();
        try {
            String json = secureSharedPreferences(context).getString("enabledServices", "");
            return gson.fromJson(json,  new TypeToken<List<ServiceInfoModel>>(){}.getType());
        } catch (GeneralSecurityException | IOException e) {
            Log.e("Utils","Could not read secure preferences",e);
        }
        return null;
    }

    public static String generatePermissionText(Context context,Policy requestedPolicy) {
        StringBuilder message= new StringBuilder();
        if(requestedPolicy.getPredicates().size()==0){
            message = new StringBuilder("This service does not request any attributes");
        }else {
            for(Predicate p:requestedPolicy.getPredicates()){
                switch (p.getOperation()){
                    case EQ:
                        message.append("- ").append(p.getAttributeName()).append(" is equal to ").append(p.getValue().getAttr()).append("\n");
                        break;
                    case REVEAL:
                        message.append("- ").append(p.getAttributeName()).append(" is revealed\n");
                        break;
                    case GREATERTHANOREQUAL:
                        if(p.getValue().getType()== AttributeType.DATE)
                            message.append("- ").append(p.getAttributeName()).append(" is after ").append(p.getValue().getAttr()).append("\n");
                        else
                            message.append("- ").append(p.getAttributeName()).append(" is greater than ").append(p.getValue().getAttr()).append("\n");
                        break;
                    case LESSTHANOREQUAL:
                        if(p.getValue().getType()== AttributeType.DATE)
                            message.append("- ").append(p.getAttributeName()).append(" is before ").append(p.getValue().getAttr()).append("\n");
                        else
                            message.append("- ").append(p.getAttributeName()).append(" is less than ").append(p.getValue().getAttr()).append("\n");
                        break;
                    case INRANGE:
                        if(p.getValue().getType()== AttributeType.DATE)
                            message.append("- ").append(p.getAttributeName()).append(" is after ").append(p.getValue().getAttr()).append(" and before ").append(p.getExtraValue().getAttr()).append("\n");
                        else
                            message.append("- ").append(p.getAttributeName()).append(" is greater than ").append(p.getValue().getAttr()).append(" and less than ").append(p.getExtraValue().getAttr()).append("\n");
                }
            }
            message.append(context.getString(R.string.askPermissionsText));
        }
        return message.toString();
    }

    public static Policy getPolicy(String service, CapManAPIService apiService, String TAG) throws CapManCommunicationException {
        Log.d(TAG,"Requesting policy "+service);
        try {
            ExecutorService executor= Executors.newSingleThreadExecutor();
            CompletionService<Response> completionService = new ExecutorCompletionService<>(executor);
            Policy policy=new Policy();
            RetrofitCallablePolicy callable=new RetrofitCallablePolicy(apiService.getPolicy(service));
            completionService.submit(callable);
            Future<Response> responseFuture=completionService.take();
            Response resp=responseFuture.get();
            Log.d(TAG,"PolicyRequest: Response "+ resp.toString());
            JsonObject responseBody=(JsonObject)resp.body();
            Log.d(TAG,"PolicyRequest: Response body "+responseBody.toString()); //TODO Change method for Error handling
            if(responseBody.has("Error")){
                throw new CapManCommunicationException("Something wrong with service path"+responseBody.get("Error").getAsString());
            }
            ObjectMapper mapper=new ObjectMapper();
            policy=mapper.readValue(resp.body().toString(),Policy.class);
            Log.d(TAG,"Succesfully retrieved policy for service");
            return policy;
        } catch (Exception e) {
            Log.e(TAG,"Something wrong while communicating with Capability Manager",e);
            throw new CapManCommunicationException("Something wrong while communicating with Capability Manager",e);
        }
    }

    public static String langCode(){
        String lanCode;
        switch ( Locale.getDefault().getLanguage()){
            case "es":
                lanCode="es";
                break;
            default:
                lanCode="en";
        }
        return lanCode;
    }

    private static class RetrofitCallablePolicy implements Callable<Response> {

        private Call<JsonElement> call;

        public RetrofitCallablePolicy(Call<JsonElement> call){
            this.call=call;
        }

        @Override
        public Response call() throws Exception {
            return call.execute();
        }
    }

    public static String readContentFromUri(Uri uri, Context context) {
        try {
            InputStream in = context.getContentResolver().openInputStream(uri); // openInputStream(uri);


            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }

            return total.toString();
        }catch (Exception e) {
            Log.e("UTILS", e.getMessage());
            return null;

        }
    }



}
