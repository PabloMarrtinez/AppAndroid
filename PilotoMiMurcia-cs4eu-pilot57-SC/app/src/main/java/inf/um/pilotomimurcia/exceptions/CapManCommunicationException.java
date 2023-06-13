package inf.um.pilotomimurcia.exceptions;

public class CapManCommunicationException extends Exception {

    public CapManCommunicationException(String msg){
        super(msg);
    }

    public CapManCommunicationException(String msg,Exception e){
        super(msg,e);
    }
}
