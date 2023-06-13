package inf.um.pilotomimurcia.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import eu.olympus.model.Policy;
import eu.olympus.model.exceptions.TokenGenerationException;
import inf.um.pilotomimurcia.olympus.ClientSingleton;

public abstract class AsyncPresentation {

    public abstract void handleGeneratePresentationResponse(Object response);

    public void doAsyncGeneratePresentation(Policy requestedPolicy) {
        HandlerThread ht = new HandlerThread("PresentationThread");
        ht.start();
        Handler asyncHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                handleGeneratePresentationResponse(msg.obj);
            }
        };
        Runnable runnable = () -> {
            try {
                String res = ClientSingleton.getCredentialManager().generatePresentationToken(requestedPolicy).toJSONString();;
                Message message = new Message();
                message.obj = res;
                asyncHandler.sendMessage(message);
            } catch (TokenGenerationException e) {
                Message message = new Message();
                message.obj = e;
                asyncHandler.sendMessage(message);
            } catch (IllegalStateException e) {
                Message message = new Message();
                message.obj = e;
                asyncHandler.sendMessage(message);
            }
        };
        asyncHandler.post(runnable);
    }
}
