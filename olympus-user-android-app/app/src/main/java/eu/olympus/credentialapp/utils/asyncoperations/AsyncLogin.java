package eu.olympus.credentialapp.utils.asyncoperations;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;

import java.util.ArrayList;

import eu.olympus.credentialapp.olympus.ClientSingleton;
import eu.olympus.model.Policy;
import eu.olympus.model.exceptions.AuthenticationFailedException;
import eu.olympus.model.exceptions.TokenGenerationException;

public abstract class AsyncLogin {

    public abstract void handleLoginResponse(Object response);

    public void doAsyncLogin(String user, String password, String token, String type) {
        HandlerThread ht = new HandlerThread("LoginThread");
        ht.start();
        Handler asyncHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                handleLoginResponse(msg.obj);
            }
        };
        Runnable runnable = () -> {
            try {
                ClientSingleton.getInstance().clearSession();
                String res = ClientSingleton.getInstance().authenticate(user, password, new Policy(new ArrayList<>(),"getCredential"),token,type); //Empty presentation
                Message message = new Message();
                message.obj = res;
                asyncHandler.sendMessage(message);
            } catch (AuthenticationFailedException e) {
                Message message = new Message();
                message.obj = e;
                asyncHandler.sendMessage(message);
            }
        };
        asyncHandler.post(runnable);
    }
}
