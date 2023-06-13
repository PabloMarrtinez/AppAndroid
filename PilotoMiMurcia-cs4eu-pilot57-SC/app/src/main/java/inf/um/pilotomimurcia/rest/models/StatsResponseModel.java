package inf.um.pilotomimurcia.rest.models;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class StatsResponseModel {

    @SerializedName("monthServ")
    @Expose
    private List<List<Object>> monthServ = null;
    @SerializedName("monthServUid")
    @Expose
    private List<List<Object>> monthServUid = null;
    @SerializedName("anonVsIdentified")
    @Expose
    private AnonVsIdentifiedModel anonVsIdentified;

    private List<DateValueModel> parsedMonthServ=null;
    private List<DateValueModel> parsedMonthServUid=null;

    public List<List<Object>> getMonthServ() {
        return monthServ;
    }

    public void setMonthServ(List<List<Object>> monthServ) {
        this.monthServ = monthServ;
    }

    public List<List<Object>> getMonthServUid() {
        return monthServUid;
    }

    public void setMonthServUid(List<List<Object>> monthServUid) {
        this.monthServUid = monthServUid;
    }

    public AnonVsIdentifiedModel getAnonVsIdentified() {
        return anonVsIdentified;
    }

    public void setAnonVsIdentified(AnonVsIdentifiedModel anonVsIdentified) {
        this.anonVsIdentified = anonVsIdentified;
    }

    public List<DateValueModel> getParsedMonthServ(){
        if(parsedMonthServ==null){
            parsedMonthServ=new LinkedList<>();
            for(List<Object> dataPoint:monthServ){
                parsedMonthServ.add(new DateValueModel((String) dataPoint.get(0),(Number) dataPoint.get(1)));
            }
        }
        return parsedMonthServ;
    }

    public List<DateValueModel> getParsedMonthServUid(){
        if(parsedMonthServUid==null){
            parsedMonthServUid=new LinkedList<>();
            for(List<Object> dataPoint:monthServUid){
                parsedMonthServUid.add(new DateValueModel((String) dataPoint.get(0),(Number) dataPoint.get(1)));
            }
        }
        return parsedMonthServUid;
    }
}
