package inf.um.pilotomimurcia.miMurcia;

import android.content.Context;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

import eu.olympus.model.AttributeType;
import eu.olympus.model.Policy;
import eu.olympus.model.Predicate;
import inf.um.pilotomimurcia.R;

public class ServiceInfoModel {

    @SerializedName("policy")
    @Expose
    private Policy policy;

    @SerializedName("commodel")
    @Expose
    private String commodel;

    @SerializedName("parameters")
    @Expose
    private Map<String,String> parameters;

    @SerializedName("image")
    @Expose
    private String image;

    @SerializedName("urlPath")
    @Expose
    private String urlPath;

    @SerializedName("name")
    @Expose
    private Map<String,String> name;

    public ServiceInfoModel() {
    }

    public ServiceInfoModel(Policy policy, String commodel, Map<String, String> parameters, String image, Map<String, String> name, String urlPath) {
        this.policy = policy;
        this.commodel = commodel;
        this.parameters = parameters;
        this.image = image;
        this.name = name;
        this.urlPath=urlPath;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public String getCommodel() {
        return commodel;
    }

    public void setCommodel(String commodel) {
        this.commodel = commodel;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }

    public String getPrettyExtraInfo(){
        StringBuilder builder=new StringBuilder();
        builder.append("Model: ").append(commodel).append("\n");
        if(parameters.size()!=0)
            builder.append("Parameters: ");
        for(String param:parameters.keySet()){
            builder.append(param).append(": ").append(parameters.get(param)).append("\n");
        }
        builder.append("Policy: ");
        builder.append(generatePolicyText(policy));
        return builder.toString();
    }

    private String generatePolicyText(Policy requestedPolicy) {
        StringBuilder message= new StringBuilder();
        if(requestedPolicy.getPredicates().size()==0){
            message = new StringBuilder("This service does not request any attributes");
        }else {
            for(Predicate p:requestedPolicy.getPredicates()){
                switch (p.getOperation()){
                    case EQ:
                        message.append("- ").append(p.getAttributeName()).append(" is equal to ").append(p.getValue().getAttr()).append("\n");
                        break;
                    case REVEAL:
                        message.append("- ").append(p.getAttributeName()).append(" is revealed\n");
                        break;
                    case GREATERTHANOREQUAL:
                        if(p.getValue().getType()== AttributeType.DATE)
                            message.append("- ").append(p.getAttributeName()).append(" is after ").append(p.getValue().getAttr()).append("\n");
                        else
                            message.append("- ").append(p.getAttributeName()).append(" is greater than ").append(p.getValue().getAttr()).append("\n");
                        break;
                    case LESSTHANOREQUAL:
                        if(p.getValue().getType()== AttributeType.DATE)
                            message.append("- ").append(p.getAttributeName()).append(" is before ").append(p.getValue().getAttr()).append("\n");
                        else
                            message.append("- ").append(p.getAttributeName()).append(" is less than ").append(p.getValue().getAttr()).append("\n");
                        break;
                    case INRANGE:
                        if(p.getValue().getType()== AttributeType.DATE)
                            message.append("- ").append(p.getAttributeName()).append(" is after ").append(p.getValue().getAttr()).append(" and before ").append(p.getExtraValue().getAttr()).append("\n");
                        else
                            message.append("- ").append(p.getAttributeName()).append(" is greater than ").append(p.getValue().getAttr()).append(" and less than ").append(p.getExtraValue().getAttr()).append("\n");
                }
            }
        }
        return message.toString();
    }
}
