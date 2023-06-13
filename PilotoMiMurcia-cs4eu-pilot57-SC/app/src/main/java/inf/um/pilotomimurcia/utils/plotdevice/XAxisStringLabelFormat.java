package inf.um.pilotomimurcia.utils.plotdevice;

import android.util.Log;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.List;

public class XAxisStringLabelFormat extends Format {

    private List<String> xLabels;

    public XAxisStringLabelFormat(List<String> labels){
        this.xLabels=labels;
    }

    @Override
    public StringBuffer format(Object arg0, StringBuffer arg1, FieldPosition arg2) {
        Log.d("PAVO","arg0 "+arg0.toString()+" arg1: "+arg1.toString());
        int parsedInt = Math.round(Float.parseFloat(arg0.toString()));
        if(parsedInt>=0 && parsedInt< xLabels.size()){
            String labelString = xLabels.get(parsedInt);
            arg1.append(labelString);
        }
        return arg1;
    }

    @Override
    public Object parseObject(String arg0, ParsePosition arg1) {
        return xLabels.indexOf(arg0);
    }
}
