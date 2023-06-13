package inf.um.pilotomimurcia.miMurcia;



import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import inf.um.pilotomimurcia.miMurcia.model.CapabilityToken;
import inf.um.pilotomimurcia.miMurcia.model.SimpleAccessRight;

//TODO Actual storage instead of InMemoryMap
public class CapabilityTokenStorage {

    private static CapabilityTokenStorage storage=null;

    private  Map<String, CapabilityToken> map;

    private CapabilityTokenStorage(){
        map=new HashMap<>();
    }

    public static CapabilityTokenStorage getInstance(){
        if (storage==null)
            storage=new CapabilityTokenStorage();
        return storage;
    }

    public void storeToken(CapabilityToken token){
        for(SimpleAccessRight ar:token.getAr()){
            map.put(ar.getResource(),token);
        }
    }

    public CapabilityToken getToken(String serviceUrl){
        CapabilityToken token=map.get(serviceUrl);
        if(token!=null && !token.IsTimeValid()){
            map.values().removeIf(val -> token.equals(val));
            return null;
        }
        return token;
    }

    public void deleteAll() {
        map.clear();
    }
}
