package inf.um.pilotomimurcia.miMurcia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import inf.um.pilotomimurcia.R;
import inf.um.pilotomimurcia.utils.Utils;

public class MiMurciaServicesAdapter extends RecyclerView.Adapter<MiMurciaServicesAdapter.ViewHolder> {
    private List<ServiceInfoModel> serviceModelList;
    private OnServiceListener onServiceListener;

    public MiMurciaServicesAdapter(List<ServiceInfoModel> serviceModelList, OnServiceListener onServiceListener) {
        this.serviceModelList = serviceModelList;
        this.onServiceListener = onServiceListener;
    }

    @NonNull
    @Override
    public MiMurciaServicesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.service_list_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(v, onServiceListener);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MiMurciaServicesAdapter.ViewHolder holder, int position) {
        String name = serviceModelList.get(position).getName().get(Utils.langCode());
        String imgUrl = serviceModelList.get(position).getImage();
        holder.serviceName.setText(name);
        Glide.with(holder.imgService.getContext()).load(imgUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.imgService);
    }

    @Override
    public int getItemCount() {
        return serviceModelList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView serviceName;
        private ImageView imgService;
        OnServiceListener onServiceListener;

        public ViewHolder(@NonNull View itemView, OnServiceListener onServiceListener) {
            super(itemView);
            serviceName = (TextView) itemView.findViewById(R.id.servicesListText);
            imgService = (ImageView) itemView.findViewById(R.id.imgService);
            this.onServiceListener = onServiceListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onServiceListener.onClick(getAdapterPosition());
        }
    }

    public interface OnServiceListener {
        void onClick(int index);
    }
}
