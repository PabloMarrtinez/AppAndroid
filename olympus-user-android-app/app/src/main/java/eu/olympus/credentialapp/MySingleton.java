package eu.olympus.credentialapp;

public class MySingleton {
    private static MySingleton instance;
    private String user;
    private String pass;

    private String publicKey;

    private MySingleton() {
        // Constructor privado para evitar la creación de múltiples instancias
    }

    public static synchronized MySingleton getInstance() {
        if (instance == null) {
            instance = new MySingleton();
        }
        return instance;
    }

    public String getUser(){
        return user;
    }

    public String getPass(){
        return pass;
    }

    public String getPublicKey(){
        return publicKey;
    }

    public void setMyVariable2(String value1) {
        publicKey = value1;
    }
    public void setMyVariable(String value1, String value2) {
        user = value1;
        pass = value2;
    }
}
