package inf.um.pilotomimurcia.eidas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.http.SslError;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;
import android.webkit.ClientCertRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import inf.um.pilotomimurcia.EIDASActivity;

public class MyWebViewClient extends WebViewClient {

    private static final String TAG = MyWebViewClient.class.getSimpleName();
    private EIDASActivity fatherActivity;
    private Context appContext;
    private String savedAlias;
    private WebView view;

    private ClientCertRequest request;

    public MyWebViewClient(EIDASActivity fatherActivity, Context appContext) {
        super();
        this.fatherActivity = fatherActivity;
        this.appContext = appContext;
    }

    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Log.d(TAG, "shouldOverrideUrlLoading " + request.getUrl());
        return true;
    }

    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        //Log.d(TAG, "shouldInterceptRequest " + request.getUrl());
        //Log.d(TAG, "shouldInterceptRequest tostring: " + request.toString());
        Log.d(TAG, "shouldInterceptRequest url: " + request.getUrl());
        Log.d(TAG, "shouldInterceptRequest method: " + request.getMethod());
        Log.d(TAG, "shouldInterceptRequest headers: " + request.getRequestHeaders());


        return null;
    }


    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(fatherActivity);
        String message = "SSL Certificate error.";
        switch (error.getPrimaryError()) {
            case SslError.SSL_UNTRUSTED:
                message = "The certificate authority is not trusted.";
                break;
            case SslError.SSL_EXPIRED:
                message = "The certificate has expired.";
                break;
            case SslError.SSL_IDMISMATCH:
                message = "The certificate Hostname mismatch.";
                break;
            case SslError.SSL_NOTYETVALID:
                message = "The certificate is not yet valid.";
                break;
        }
        Log.d("TAG", message);

        handler.proceed();
        message += " Do you want to continue anyway?";

        builder.setTitle("SSL Certificate Error");
        builder.setMessage(message);
        builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.proceed();
            }
        });
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.cancel();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();

    }



    @Override
    public void onReceivedClientCertRequest(WebView view, final ClientCertRequest request) {

        Log.d(TAG, "onReceivedClientCertRequest");

        this.view = view;
        this.request = request;

        Log.d(TAG, "onReceivedClientCertRequest - default: " + request.toString());
        Log.d(TAG, "onReceivedClientCertRequest - host: " + request.getHost());
        Log.d(TAG, "onReceivedClientCertRequest - port: " + request.getPort());
        //Log.d (TAG, "onReceivedClientCertRequest - keyTypes: " +request.getKeyTypes().toString());
        //Log.d (TAG, "onReceivedClientCertRequest - principals: " +request.getPrincipals().toString());
        //request.proceed(eIDASActivity.getPrivateKey(), eIDASActivity.getCertificates());
        createCertSourceSelectorDialog();

    }

    public void showAndroidCertSelector() {

        KeyChain.choosePrivateKeyAlias(fatherActivity,
                new KeyChainAliasCallback() {

                    public void alias(String alias) {
                        // Credential alias selected.  Remember the alias selection for future use.
                        if (alias != null) {
                            saveAlias(alias);
                        } else {
                            Log.d(TAG, "Recovered alias is null. Has user cancelled the cert selection?");
                        }
                    }
                },
                new String[]{"RSA", "DSA"}, // List of acceptable key types. null for any
                null,                // issuer, null for any
                request.getHost(),           // host name of server requesting the cert, null if unavailable
                443,                   // port of server requesting the cert, -1 if unavailable
                savedAlias);                // alias to preselect, null if unavailable


    }

    /* Method for exec the callback of KeyChain.choosePrivateKeyAlias (onReceivedClientCertRequest) */
    private void saveAlias(String alias) {
        if (alias != null) {
            this.savedAlias = alias;
            Log.d(TAG, "alias: " + alias);
            onChosenCertForClientCertRequest(alias);
        } else {
            Log.d(TAG, "Recovered alias is null");
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        view.loadUrl("javascript:window.HTMLOUT.showHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
        //view.loadUrl("javascript:(function(){document.getElementById('SelectedIdP').value = 'EIDAS'; document.idpRedirect.submit();})");
    }

    public void onChosenCertForClientCertRequest(String alias) {


        //Log.d(TAG, "onChosenCertForClientCertRequest");
        PrivateKey privateKey = null;
        try {
            privateKey = KeyChain.getPrivateKey(appContext, savedAlias);
            //Log.d(TAG, "onChosenCertForClientCertRequest - private key");
            //Log.d(TAG, "onChosenCertForClientCertRequest - algor: " + privateKey.getAlgorithm());
            //Log.d(TAG, "onChosenCertForClientCertRequest - format: " + privateKey.getFormat());
            //Log.d(TAG, "onChosenCertForClientCertRequest - obj:\n" + privateKey.toString());
            //Log.d(TAG, "onChosenCertForClientCertRequest - string:\n" + new String (privateKey.getEncoded()));


            X509Certificate[] certifcateChain = KeyChain.getCertificateChain(appContext, savedAlias);
            Log.d(TAG, "onChosenCertForClientCertRequest - public certs");

            if (privateKey != null) {
                request.proceed(privateKey, certifcateChain);
            } else {
                Log.d(TAG, "onChosenCertForClientCertRequest - error retrieving private key");
            }
        } catch (KeyChainException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCertSourceSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(fatherActivity);
        builder
                .setTitle("Certificate requested")
                .setMessage("The web page is asking you to use a certificate, please choose between eID or certificate");

        builder.setPositiveButton("Certificate", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showAndroidCertSelector();
            }
        });
        builder.setNegativeButton("eID", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { fatherActivity.scanDNIe(); }
        });
       /* builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });*/

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void eIDScanFinished() {
        request.proceed(EIDASActivity.getPrivateKey(), EIDASActivity.getCertificates());
    }
}
