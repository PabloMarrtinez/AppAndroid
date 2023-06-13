package eu.olympus.credentialapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ImageView resultImage = findViewById(R.id.resultImage);
        TextView resultText = findViewById(R.id.resultText);

        Intent intent = getIntent();
        int res = intent.getIntExtra("result", 0);
        switch (res) {
            case 0:
                resultImage.setImageResource(R.drawable.ic_approved);
                resultText.setText(R.string.verificationOK);
                break;
            case 1:
                resultImage.setImageResource(R.drawable.ic_rejected);
                resultText.setText(R.string.verificationFail);
                break;
            default:
                resultImage.setImageResource(R.drawable.ic_blue_screen);
                resultText.setText(R.string.presentationFail);
                break;
        }
    }
}