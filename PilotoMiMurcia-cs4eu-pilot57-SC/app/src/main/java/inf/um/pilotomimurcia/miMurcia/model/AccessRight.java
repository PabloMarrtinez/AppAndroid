package inf.um.pilotomimurcia.miMurcia.model;

import java.util.List;

public class AccessRight  extends SimpleAccessRight
{



    /**
     * Indicates with a flag if the all conditions have to be evaluated positively
     * to perform the solicited action ( AND with the integer 0) or if the case of any
     * of conditions are met the action can be performed (OR with the integer 1)
     */
    private int f;

    /**
     * A list of Condition to be met.
     */
    private List<Condition> co;




    public int getFlag() {
        return f;
    }

    public void setFlag(int flag) {
        this.f = flag;
    }


    public List<Condition> getConditions() {
        return co;
    }

    public void setConditions(List<Condition> con) {
        this.co = con;
    }


}