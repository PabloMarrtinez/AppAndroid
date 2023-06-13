package eu.olympus.credentialapp;

public class MySingleton {
    private static MySingleton instance;
    private String user;
    private String pass;

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

    public void setMyVariable(String value1, String value2) {
        user = value1;
        pass = value2;
    }
}
