package inf.um.pilotomimurcia.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import eu.olympus.client.interfaces.UserClient;
import eu.olympus.keyrock.model.TestIdentityProof;
import eu.olympus.model.Policy;
import eu.olympus.model.exceptions.AuthenticationFailedException;
import eu.olympus.model.exceptions.OperationFailedException;
import eu.olympus.model.exceptions.UserCreationFailedException;
import eu.olympus.model.server.rest.IdentityProof;
import inf.um.pilotomimurcia.olympus.ClientSingleton;

public abstract class AsyncOperations {
    private static final String TAG = AsyncOperations.class.getSimpleName();
    public static final String SIGNUP_OK="Sign up done";
    public static final String IDPROOF_OK="IdProof done";
    public static final int TEST_PROOF_CODE=0;
    public static final int UID_PROOF_CODE=1;
    public static final int EIDAS_PROOF_CODE=2;

    public void doAsyncLogin(String user, String password, Policy policy) {
        HandlerThread ht = AsyncHandler.getInstance().getHandler();
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
                UserClient client=ClientSingleton.getInstance();
                client.clearSession();
                String res = client.authenticate(user, password, policy,null,"NONE");
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

    public void doAsyncRegister(String user, String password, IdentityProof proof) {
        HandlerThread ht = AsyncHandler.getInstance().getHandler();
        ht.start();
        Handler asyncHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                handleRegisterResponse(msg.obj);
            }
        };
        Runnable runnable = () -> {
            try {
                UserClient client=ClientSingleton.getInstance();
                client.clearSession();
                if (proof != null) {
                    client.createUserAndAddAttributes(user, password, proof);
                } else {
                    client.createUser(user, password);
                }
                Message message = new Message();
                message.obj = SIGNUP_OK;
                asyncHandler.sendMessage(message);
            } catch (UserCreationFailedException e) {
                Message message = new Message();
                message.obj = e;
                asyncHandler.sendMessage(message);
            }
        };
        asyncHandler.post(runnable);
    }

    public void doAsyncIdentityProof(String user, String password, @NonNull IdentityProof proof) {
        HandlerThread ht = AsyncHandler.getInstance().getHandler();
        ht.start();
        Handler asyncHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                handleProofIdentityResponse(msg.obj,msg.what);
            }
        };
        Runnable runnable = () -> {
            try {
                UserClient client=ClientSingleton.getInstance();
                client.clearSession();
                client.addAttributes(user, password, proof,null,"NONE");
                Message message = new Message();
                message.obj = IDPROOF_OK;
                switch (proof.getClass().getSimpleName()){
                    case "TestIdentityProof":
                        message.what= TEST_PROOF_CODE;
                        break;
                    case "MiMurciaUserIdIdentityProof":
                        message.what= UID_PROOF_CODE;
                        break;
                    case "EidasIdentityProof":
                        message.what= EIDAS_PROOF_CODE;
                        break;
                }
                asyncHandler.sendMessage(message);
            } catch (OperationFailedException e) {
                Message message = new Message();
                switch (proof.getClass().getSimpleName()){
                    case "TestIdentityProof":
                        message.what= TEST_PROOF_CODE;
                        break;
                    case "MiMurciaUserIdIdentityProof":
                        message.what= UID_PROOF_CODE;
                        break;
                    case "EidasIdentityProof":
                        message.what= EIDAS_PROOF_CODE;
                        break;
                }
                message.obj = e;
                asyncHandler.sendMessage(message);
            }
        };
        asyncHandler.post(runnable);
    }

    public abstract void handleProofIdentityResponse(Object response, int what);
    public abstract void handleRegisterResponse(Object response);
    public abstract void handleLoginResponse(Object response);
}
