package inf.um.pilotomimurcia.miMurcia.model;

public class SimpleAccessRight {

    private static StringBuilder stringBuilder = new StringBuilder();

    /**
     * Indicates the Action to perform.
     * Examples: POST, PUT, GET, DELETE
     */
    private String ac;

    /**
     * Indicates the Resource to access
     * coap(s)://myIoTdevice/myresource
     *
     * 	In this case the resource would be <myresource>
     */
    private String re;


    public String getPermittedAction() {
        return ac;
    }

    public void setPermittedAction(String ac) {
        this.ac = ac;
    }

    public String getResource() {
        return re;
    }

    public void setResource(String re) {
        this.re = re;
    }

    public String toString() {
        stringBuilder.append("; SimpleAccessRight{ac=");
        stringBuilder.append(ac);
        stringBuilder.append(", re=");
        stringBuilder.append(re);
        stringBuilder.append("}");

        String value = stringBuilder.toString();

        stringBuilder.delete(0, stringBuilder.length());

        return value;
    }


    public void free() {
        ac = null;
        re = null;
    }
}
