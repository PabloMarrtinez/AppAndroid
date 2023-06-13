package eu.olympus.credentialapp.utils.asyncoperations;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import eu.olympus.credentialapp.olympus.ClientSingleton;
import eu.olympus.model.Policy;
import eu.olympus.model.exceptions.AuthenticationFailedException;
import eu.olympus.model.exceptions.OperationFailedException;

public abstract class AsyncMfaOperations {

    public static String CONFIRM_OK="OK";

    public abstract void handleMFARequestResponse(Object response);
    public abstract void handleMFAConfirmResponse(Object response);

    public void doAsyncMfaRequest(String user, String password, String type) {
        HandlerThread ht = new HandlerThread("MfaThread");
        ht.start();
        Handler asyncHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                handleMFARequestResponse(msg.obj);
            }
        };
        Runnable runnable = () -> {
            try {
                ClientSingleton.getInstance().clearSession();
                String res = ClientSingleton.getInstance().requestMFAChallenge(user, password,type);
                Message message = new Message();
                message.obj = res;
                asyncHandler.sendMessage(message);
            } catch (AuthenticationFailedException | OperationFailedException e) {
                Message message = new Message();
                message.obj = e;
                asyncHandler.sendMessage(message);
            }
        };
        asyncHandler.post(runnable);
    }

    public void doAsyncMfaConfirm(String user, String password, String token, String type) {
        HandlerThread ht = new HandlerThread("MfaThread");
        ht.start();
        Handler asyncHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                handleMFAConfirmResponse(msg.obj);
            }
        };
        Runnable runnable = () -> {
            try {
                ClientSingleton.getInstance().clearSession();
                ClientSingleton.getInstance().confirmMFA(user, password,token,type); //Empty presentation
                Message message = new Message();
                message.obj = CONFIRM_OK;
                asyncHandler.sendMessage(message);
            } catch (AuthenticationFailedException | OperationFailedException e) {
                Message message = new Message();
                message.obj = e;
                asyncHandler.sendMessage(message);
            }
        };
        asyncHandler.post(runnable);
    }
}
