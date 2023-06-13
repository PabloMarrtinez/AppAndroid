package inf.um.pilotomimurcia.miMurcia;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import inf.um.pilotomimurcia.utils.Utils;

import inf.um.pilotomimurcia.R;

public class MiMurciaServiceManagementAdapter extends RecyclerView.Adapter<MiMurciaServiceManagementAdapter.ViewHolder> {

    private List<ServiceInfoModel> services;
    private List<Boolean> enabled;
    List<MiMurciaServiceManagementAdapter.ViewHolder> holders;

    public MiMurciaServiceManagementAdapter(List<ServiceInfoModel> services, List<Boolean> enabled) {
        this.services = services;
        this.enabled =enabled;
        holders=new ArrayList<>(services.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.service_info_item,parent,false);
        ViewHolder holder = new MiMurciaServiceManagementAdapter.ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MiMurciaServiceManagementAdapter.ViewHolder holder, int position) {
        String lc=Utils.langCode();
        ServiceInfoModel ser=services.get(position);
        String name = ser.getName().get(lc);
        String imgUrl = ser.getImage();
        String extraInfo=ser.getPrettyExtraInfo();
        holder.serviceName.setText(name);
        holder.serviceDetails.setText(extraInfo);
        holder.check.setChecked(enabled.get(position));
        Glide.with(holder.imgService.getContext()).load(imgUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.imgService);
        this.holders.add(position,holder);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    public List<ServiceInfoModel> enabledServices(){
        List<ServiceInfoModel> enabledServices=new LinkedList<>();
        for(int i=0;i<services.size();i++){
            if(holders.get(i).check.isChecked())
                enabledServices.add(services.get(i));
        }
        return enabledServices;
    }

    public boolean isCheckBoxChecked(int position){
        return holders.get(position).check.isChecked();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView serviceName;
        private TextView serviceDetails;
        private ImageView imgService;
        private CheckBox check;
        private boolean isShowing;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceName = itemView.findViewById(R.id.infoServiceName);
            imgService = itemView.findViewById(R.id.infoServiceIcon);
            serviceDetails=  itemView.findViewById(R.id.serviceExtraInfo);
            serviceDetails.setVisibility(View.GONE);
            check=itemView.findViewById(R.id.checkBoxServiceItem);
            isShowing=false;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(isShowing)
                serviceDetails.setVisibility(View.GONE);
            else
                serviceDetails.setVisibility(View.VISIBLE);
            isShowing=!isShowing;
        }
    }

}
