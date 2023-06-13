package eu.olympus.credentialapp;

import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.credentialapp.olympus.EncryptedCredentialStorage;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.jar.JarEntry;

import eu.olympus.model.Attribute;

public class Home extends AppCompatActivity {

    Events f1 = new Events();
    Ticket f2 = new Ticket();

    Admin f3 = new Admin();

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.home);
        loadFragment(f1);
        BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);





    }

    public void loadFragment(Fragment f){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.navbar, f);
        transaction.commit();
    }

    public void salir (){
        Intent i = new Intent(this, Main.class);
        startActivity(i);
    }
    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.Event:
                    loadFragment(f1);
                    return true;
                case R.id.entradas:
                    loadFragment(f2);
                    return true;
                case R.id.control:
                    loadFragment(f3);
                    return true;
                case R.id.salir:
                    salir();
                    return true;



            }
            return false;
        }
    };
}
