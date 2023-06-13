package inf.um.pilotomimurcia;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import de.tsenger.androsmex.data.CANSpecDO;
import de.tsenger.androsmex.data.CANSpecDOStore;
import es.gob.fnmt.gui.PasswordUI;
import es.gob.fnmt.gui.fragment.NFCCommunicationFragment;
import es.gob.fnmt.gui.fragment.NetworkCommunicationFragment;
import es.gob.fnmt.nfc.NFCCommReaderFragment;
import es.gob.jmulticard.jse.provider.DnieKeyStore;
import inf.um.pilotomimurcia.eidas.MyJavaScriptInterface;
import inf.um.pilotomimurcia.eidas.MyWebViewClient;
import inf.um.pilotomimurcia.eidas.utils.pki.Tool;

public class EIDASActivity extends AppCompatActivity implements NFCCommReaderFragment.NFCCommReaderFragmentListener, NetworkCommunicationFragment.NetCommFragmentListener {

    private CANSpecDOStore _canStore = null;
    private static PrivateKey privateKey;
    private static X509Certificate[] certificates;
    private Button scanEID_B;
    private EditText CAN_ED;
    private NFCCommunicationFragment _readerFragment = null; //Fragment que encapsula las comunicaciones con el dispositivo NFC.
    private NetworkCommunicationFragment _networkFragment = null; //Fragment que encapulsa la GUI durante las comunicaciones a través de la red.
    private WebView myWebView;
    private MyWebViewClient myWebViewClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dnie); // WEBVIEW as content to navigate to keyrock

        myWebView = new WebView(EIDASActivity.this);
        setContentView(myWebView);

        //myWebView.loadUrl(getString(R.string.KeyRock_OAuth));
        myWebView.getSettings().setSupportZoom(true);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setUseWideViewPort(true);

        myWebView.getSettings().setAllowContentAccess(true);
        myWebView.getSettings().setAllowFileAccess(true);
        myWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        myWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        myWebView.addJavascriptInterface(new MyJavaScriptInterface(this), "HTMLOUT");

        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        myWebView.loadUrl("https://ppissuer.inf.um.es/oauth2/authorize?response_type=code&client_id=c682ef00-c13a-444e-a9db-b91ebba1eee0&state=xyz&redirect_uri=https://google.com");
        //myWebView.loadUrl("https://ppissuer.inf.um.es/oauth2/authorize?response_type=code&client_id=c682ef00-c13a-444e-a9db-b91ebba1eee0&state=xyz&redirect_uri=https://google.com");
        myWebView.loadUrl("javascript:(function(){ var html = document.documentElement.innerHTML;" +
                            "if (html.indexOf(\"Por favor, introduzca su DNI\", 0) > 0) {" +
                                "l=document.getElementById('submit_button');" +
                                "e=document.createEvent('HTMLEvents');" +
                                "e.initEvent('click',true,true);" +
                                "l.dispatchEvent(e);" + "" +
                            "}" +
                        "})()");

        myWebViewClient = new MyWebViewClient(this, getApplicationContext());
        myWebView.setWebViewClient(myWebViewClient);


        _canStore = new CANSpecDOStore(this);

        PasswordUI.setPasswordDialog(null);  //dialogo de PIN por defecto.
        PasswordUI.setAppContext(this);

    }


    public void scanEID_BClicked(View view) {
        String text = CAN_ED.getText().toString();
        TextView canEDempty = findViewById(R.id.NoCanTV);
        if (text.length() != 6) // ERROR: CAN number is a 6 length number
        {
            canEDempty.setVisibility(View.VISIBLE);
            return;
        }
        if ( _canStore.getAll().isEmpty() || !text.equals(_canStore.getAll().get(0).getCanNumber())) {
            for (CANSpecDO canSpecDO : _canStore.getAll()) {
                _canStore.delete(canSpecDO);
            }
            _canStore.save(new CANSpecDO(text, "", ""));
        }

        scanEID_B.setVisibility(View.INVISIBLE);
        CAN_ED.setVisibility(View.INVISIBLE);
        canEDempty.setVisibility(View.INVISIBLE);


        //Indicamos el CAN en el fragment de lectura NFC
        CANSpecDO canSpecDO = _canStore.getAll().get(0);
        Bundle arg = new Bundle();
        arg.putParcelable(NFCCommReaderFragment.CAN_ARGUMENT_KEY_STRING, canSpecDO);
        arg.putBoolean(NFCCommReaderFragment.PRELOADKEYSTORE_ARGUMENT_KEY_STRING, true);
        _readerFragment = new NFCCommunicationFragment();
        _readerFragment.setArguments(arg);
        _readerFragment.setTextColor(Color.BLACK);

        //Añadimos el fragment de lectura NFC a la activity.
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, _readerFragment);
        transaction.addToBackStack(null);
        transaction.commit();

    }


    public static PrivateKey getPrivateKey() {
        return privateKey;
    }


    public static X509Certificate[] getCertificates() {
        return certificates;
    }


    @Override
    public void netConnDownload() throws IOException {
        try {
            privateKey = (PrivateKey) NFCCommReaderFragment.getKeyStore().getKey(DnieKeyStore.AUTH_CERT_ALIAS, null);
            certificates = (X509Certificate[]) NFCCommReaderFragment.getKeyStore().getCertificateChain(DnieKeyStore.AUTH_CERT_ALIAS);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void netConnDone(boolean b) {
        setContentView(myWebView);
        myWebViewClient.eIDScanFinished();
    }

    @Override
    public CANSpecDO getCanToStore(KeyStore keyStore, String s) throws KeyStoreException {
        X509Certificate certificate = ((X509Certificate) keyStore.getCertificate(DnieKeyStore.AUTH_CERT_ALIAS));
        certificates = new X509Certificate[]{certificate};
        return new CANSpecDO(s, Tool.getCN(certificate), Tool.getNIF(certificate));
    }

    @Override
    public void doNotify(NFC_callback_notify notify, String s) {
        String message = s;
        switch (notify) {
            case NFC_TASK_INIT:
                _readerFragment.updateInfo(notify, "Comunicando con Dnie", "estableciendo canal seguro...");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //resultInfo.setVisibility(GONE);
                    }
                });
                break;
            case NFC_TASK_UPDATE:
                _readerFragment.updateInfo(notify, "Comunicando con Dnie", "obteniendo información...");
                break;
            case FINAL_TASK_INIT:
                message = "Connectando con sitio web...";
                break;
            case FINAL_TASK_DONE:
                message = "Conexión finalizada.";
                break;
            case ERROR:
                if (s.equalsIgnoreCase(NFCCommReaderFragment.ERROR_PIN_LOCKED)) {
                    Toast.makeText(this.getApplicationContext(), getString(R.string.lib_dni_password_error_pin_locked), Toast.LENGTH_LONG).show();

                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.remove(_readerFragment);
                    transaction.commit();
                    return;
                } else {
                    _readerFragment.updateInfo(notify, "Error en comunicación", message);
                    if (s.contains("CAN incorrecto")) {
                        Toast.makeText(this.getApplicationContext(), s, Toast.LENGTH_LONG).show();

                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.remove(_readerFragment);
                        transaction.commit();
                        return;
                    }
                }
                break;
            default:
                _readerFragment.updateInfo(notify, "Comunicando con Dnie", message);
        }
        if (notify == NFC_callback_notify.NFC_TASK_FINISHED) {
            ProgressDialog myProgressDialog = new ProgressDialog(this);

            myProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            myProgressDialog.setTitle("Descargando datos");
            myProgressDialog.setMessage("");
            myProgressDialog.setProgress(0);
            myProgressDialog.setMax(100);

            _networkFragment = new NetworkCommunicationFragment();
            _networkFragment.setTextColor(Color.BLACK);

            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, _networkFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    public void scanDNIe() {
        setContentView(R.layout.activity_dnie);

        scanEID_B = findViewById(R.id.scanB);
        CAN_ED = findViewById(R.id.CANnumberED);

        if (!_canStore.getAll().isEmpty()) {
            CAN_ED.setText(_canStore.getAll().get(0).getCanNumber());
        }
        CAN_ED.setOnClickListener(v -> {
            CAN_ED.setText("");
        });
    }

    public void finish(String textContent) {
        Intent intent = new Intent();
        intent.putExtra("assertion", textContent);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}