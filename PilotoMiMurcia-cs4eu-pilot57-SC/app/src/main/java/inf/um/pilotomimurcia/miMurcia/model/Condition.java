package inf.um.pilotomimurcia.miMurcia.model;

public class Condition {

    /**
     * The Condition type from :
     * https://tools.ietf.org/html/draft-li-core-conditional-observe-04#section-4
     *
     +-----------------------+------+-----------------+
     | Condition type (5 bit)| Id.  | Condition Value |
     +-----------------------+------+-----------------+
     | Cancellation          |  0   |       no        |
     +-----------------------+------+-----------------+
     | Time series           |  1   |       no        |
     +-----------------------+------+-----------------+
     | Minimum response time |  2   |       yes       |
     +-----------------------+------+-----------------+
     | Maximum response time |  3   |       yes       |
     +-----------------------+------+-----------------+
     | Step                  |  4   |       yes       |
     +-----------------------+------+-----------------+
     | AllValues<            |  5   |       yes       |
     +-----------------------+------+-----------------+
     | AllValues>            |  6   |       yes       |
     +-----------------------+------+-----------------+
     | Value=                |  7   |       yes       |
     +-----------------------+------+-----------------+
     | Value<>               |  8   |       yes       |
     +-----------------------+------+-----------------+
     | Periodic              |  9   |       yes       |
     +-----------------------+------+-----------------+
     *
     */
    private int t;

    /**
     * The value of the condition.
     *
     * Example: If we were to retrieve the temperature of a sensor, we could
     * express a range of values between which gathering this information is permitted.
     */
    private int v;

    /**
     *  The Unit of the of measure of the measure that will be represented by the resource.
     *  Example: In the case of temperature ºC
     */
    private String u;

    /**
     *
     * @param t The type according to draft-li-core-conditional-observe-04#section-4
     * @param v The value that has to be evaluated by the condition
     * @param u The unit of measurement of the value (v)
     */
    public Condition (int t, int v, String u){
        this.t = t;
        this.v = v;
        this.setU(u);
    }

    public int getType() {
        return t;
    }

    public void setType(int t) {
        this.t = t;
    }

    public int getValue() {
        return v;
    }

    public void setValue(int v) {
        this.v = v;
    }

    public String getU() {
        return u;
    }

    public void setU(String u) {
        this.u = u;
    }

}

