package inf.um.pilotomimurcia.utils.plotdevice;

public class ComputationUtils {

    public static float getPrettyStep(float max, int nSeps){
        float val= max/nSeps;
        if(val<2.5)
            return 1;
        else
            return 5*(Math.round(val/5));
    }
}
