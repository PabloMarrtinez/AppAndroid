package eu.olympus.credentialapp.rest2;


public class APIUtils {
    //public static final String CAPMAN_BASE_URL = "https://10.0.2.2:30303";
    public static final String CAPMAN_BASE_URL = "https://ppissuer.inf.um.es:30303";
    //public static final String PEP_BASE_URL="https://10.0.2.2:1027";
    public static final String PEP_BASE_URL="https://ppissuer.inf.um.es:1027";
    public static final String CHAIN_API_BASE_URL="http://155.54.95.195:3000";

    public static CapManAPIService getCapManAPIService() {
        return RetrofitClient.getClient(CAPMAN_BASE_URL).create(CapManAPIService.class);
    }

    public static PepAPIService getPepAPIService() {
        return RetrofitClient.getClient(PEP_BASE_URL).create(PepAPIService.class);
    }

    public static ChainAPIService getChainAPIService() {
        return RetrofitClient.getClient(CHAIN_API_BASE_URL).create(ChainAPIService.class);
    }
}