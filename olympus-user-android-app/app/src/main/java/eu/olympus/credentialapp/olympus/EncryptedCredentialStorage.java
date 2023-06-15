package eu.olympus.credentialapp.olympus;

import android.content.Context;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.model.PSCredential;
import eu.olympus.credentialapp.utils.Utils;
import java.io.File;

public class EncryptedCredentialStorage implements CredentialStorage {

    private String filename;
    private Context context;

    public EncryptedCredentialStorage(String filename,Context context){
        this.filename=filename;
        this.context=context;
    }

    @Override
    public void storeCredential(PSCredential psCredential) {
        byte[] content=psCredential.getEncoded().getBytes();
        //Log.d("EncryptedStorage","Store "+psCredential.toString());
        Utils.writeEncrypted(filename,content,context);
    }


    @Override
    public PSCredential getCredential() {
        try {
            byte[] content=Utils.readEncrypted(filename,context);
            //Log.d("EncryptedStorage","Get "+new String(content));
            return new PSCredential(new String(content));
        } catch (Exception e) {
            Log.d("EncryptedStorage","Cannot retrieve credential");
        }
        return null;
    }



    @Override
    public boolean checkCredential() {
        try {
            byte[] content=Utils.readEncrypted(filename,context);
            PSCredential currentCredential=new PSCredential(new String(content));
            if(currentCredential.getEpoch() < System.currentTimeMillis()){
                deleteCredential();
                Log.d("EncryptedStorage","Expired credential");
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.d("EncryptedStorage","No credential ");
        }
        return false;
    }

    @Override
    public void deleteCredential() {
        Utils.writeEncrypted(filename,new byte[0],context);
    }

    @Override
    public void deleteRevocationCredential() {

        String revocationFilename = filename + "_revocation";
        File fileToDelete = new File(context.getFilesDir(), revocationFilename);
        fileToDelete.delete();

    }


    @Override
    public void storeRevocationCredential(PSCredential psCredential) {

        String revocationFilename = filename + "_revocation";
        byte[] content = psCredential.getEncoded().getBytes();
        Utils.writeEncrypted(revocationFilename, content, context);


    }


    @Override
    public PSCredential getRevocationCredential() {
        return null;
    }


}