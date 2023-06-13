package eu.olympus.credentialapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import eu.olympus.credentialapp.utils.asyncoperations.AsyncMfaOperations;
import eu.olympus.server.GoogleAuthenticator;

public class MfaActivity extends AppCompatActivity {

    private static String TAG=MfaActivity.class.getSimpleName();
    private EditText username,password;
    private Button startBtn;
    private AsyncMfaOperations asyncMfaOperations;
    private String user,pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mfa);
        username=(EditText)findViewById(R.id.usernameMfa);
        username.addTextChangedListener(mfaTextWatcher);
        password=(EditText)findViewById(R.id.passwordMfa);
        password.addTextChangedListener(mfaTextWatcher);
        startBtn=findViewById(R.id.btnStart);
        asyncMfaOperations=new AsyncMfaOperations() {
            @Override
            public void handleMFARequestResponse(Object response) {
                if(response instanceof Exception){
                    Log.d(TAG,"Something went wrong when requestingMfaSecret", (Exception) response);
                    runOnUiThread(()->{
                        AlertDialog.Builder builder = new AlertDialog.Builder(MfaActivity.this);
                        builder.setTitle("MFA activation error").setMessage("Could not request MFA secret.").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                    });
                } else{
                    runOnUiThread(()->{
                        AlertDialog.Builder builder = new AlertDialog.Builder(MfaActivity.this);
                        builder.setTitle("MFA activation").setMessage("Input the following secret into your Google Authenticator:\n"+response).setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        confirmMFA();
                                    }
                                }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .setNeutralButton("Copy secret",(dialogInterface,i)->{
                                    //Nothing as we will override OnClick
                                });
                        final AlertDialog dialog = builder.create();
                        dialog.show();
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("MfaSecret", response.toString());
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(MfaActivity.this,"Secret copied to clipboard",Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            }

            @Override
            public void handleMFAConfirmResponse(Object response) {
                if(response instanceof Exception) {
                    Log.d(TAG,"Something went wrong when requestingMfaSecret", (Exception) response);
                    runOnUiThread(()->{
                        AlertDialog.Builder builder = new AlertDialog.Builder(MfaActivity.this);
                        builder.setTitle("MFA activation error").setMessage("Could not confirm MFA secret.").setPositiveButton("Try again", (dialogInterface, i) -> runOnUiThread(MfaActivity.this::confirmMFA))
                        .setNegativeButton("Cancel",(dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                    });
                }else {
                    runOnUiThread(()-> {
                        Toast.makeText(MfaActivity.this,"MFA successfully added", Toast.LENGTH_SHORT).show();
                        SharedPreferences prefs = MfaActivity.this.getSharedPreferences(
                                "com.example.app", Context.MODE_PRIVATE);
                        prefs.edit().putBoolean("mfaChecked",true).apply();
                        finish();
                    });
                }
            }
        };
    }

    private void confirmMFA() {
        AlertDialog.Builder builder2 = new AlertDialog.Builder(MfaActivity.this);
        builder2.setTitle("MFA activation");
        final EditText input = new EditText(MfaActivity.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder2.setView(input);
        builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String token = input.getText().toString();
                asyncMfaOperations.doAsyncMfaConfirm(user,pass,token, GoogleAuthenticator.TYPE);
            }
        });
        builder2.create().show();
    }

    public void onStartClick(View v){
        user=username.getText().toString().trim();
        pass=password.getText().toString().trim();
        asyncMfaOperations.doAsyncMfaRequest(user,pass,GoogleAuthenticator.TYPE);
    }



    private final TextWatcher mfaTextWatcher =new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String us=username.getText().toString().trim();
            String pw=password.getText().toString().trim();
            startBtn.setEnabled(!us.isEmpty() && !pw.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

}