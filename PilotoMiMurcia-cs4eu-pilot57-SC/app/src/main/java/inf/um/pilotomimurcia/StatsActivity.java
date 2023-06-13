package inf.um.pilotomimurcia;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;

import com.androidplot.util.PixelUtils;
import com.androidplot.util.SeriesUtils;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import inf.um.pilotomimurcia.miMurcia.CapabilityTokenStorage;
import inf.um.pilotomimurcia.miMurcia.MiMurciaServicesAdapter;
import inf.um.pilotomimurcia.miMurcia.ServiceInfoModel;
import inf.um.pilotomimurcia.miMurcia.model.CapabilityToken;
import inf.um.pilotomimurcia.rest.APIUtils;
import inf.um.pilotomimurcia.utils.Utils;
import inf.um.pilotomimurcia.utils.plotdevice.ComputationUtils;
import inf.um.pilotomimurcia.utils.plotdevice.QuickCustomPaint;
import inf.um.pilotomimurcia.utils.plotdevice.XAxisStringLabelFormat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsActivity extends AppCompatActivity implements MiMurciaServicesAdapter.OnServiceListener {

    private static final String TAG=StatsActivity.class.getSimpleName();
    private XYPlot plot;

    private BarFormatter bf;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private MiMurciaServicesAdapter adapter;
    private List<ServiceInfoModel> lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        lista = Utils.retrieveServices(this);
        recyclerView = findViewById(R.id.serviceStatsListView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MiMurciaServicesAdapter(lista, this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        plot=findViewById(R.id.plotTotalUser);
        plot.setDomainLabel("Service");
        plot.setRangeLabel("Visits");
        plot.setTitle("Your total service accesses");

        if(!getIntent().hasExtra("data")) {
            Log.e(TAG,"Error, no stat data");
        }
        JsonObject stats = new Gson().fromJson(getIntent().getStringExtra("data"),JsonObject.class);
        List<String> labels=new LinkedList<>(); //TODO maybe need to translate depending on service definitions/management...
        List<Number> values=new LinkedList<>();
        for(String service:stats.keySet()){
            labels.add(service);
            values.add(stats.get(service).getAsInt());
        }

        setupGraph(labels, values);
    }

    private void setupGraph(List<String> labels, List<Number> values) {
        //Axis configuration: boundaries, labels...
        plot.setDomainBoundaries(-1, values.size(), BoundaryMode.FIXED);
        int maxY=SeriesUtils.minMax(values).getMax().intValue();
        plot.setRangeBoundaries(0,maxY+ 0.1*maxY, BoundaryMode.FIXED);
        Log.d(TAG,"Values: "+ values);
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 1);
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, ComputationUtils.getPrettyStep(maxY,6));
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new XAxisStringLabelFormat(labels));
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setPaint(new QuickCustomPaint(Color.DKGRAY,34, Paint.Align.LEFT));
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setRotation(20);
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#"));
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setPaint(new QuickCustomPaint(Color.DKGRAY,34, Paint.Align.CENTER));

        //Graph config (grid, background...)
        //plot.getGraph().setRangeGridLinePaint(null);
        plot.getGraph().setDomainGridLinePaint(null);
        plot.getBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getGraph().setBackgroundPaint(new QuickCustomPaint(Color.WHITE,20, Paint.Align.CENTER));
        plot.getGraph().getGridBackgroundPaint().setColor(Color.WHITE);

        //Margins
        plot.getGraph().setMarginBottom(PixelUtils.dpToPix(20));
        plot.getGraph().setMarginRight(PixelUtils.dpToPix(20));

        //Bars configuration
        bf=new BarFormatter(Color.rgb(100, 100, 150), Color.WHITE);
        bf.setMarginLeft(PixelUtils.dpToPix(1));
        bf.setMarginRight(PixelUtils.dpToPix(1));

        //Data series
        XYSeries series=new SimpleXYSeries(values, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "User visits");
        plot.addSeries(series,bf);

        //Bar renderer, for extra configurations like bar width(/separation)
        BarRenderer renderer = plot.getRenderer(BarRenderer.class);
        renderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_WIDTH,PixelUtils.dpToPix(30));

        //Legend
        plot.getLegend().setVisible(false);

        plot.redraw();
    }

    @Override
    public void onClick(int index) {
        ServiceInfoModel service = this.lista.get(index);
        String url="/"+service.getUrlPath();
        CapabilityToken token=CapabilityTokenStorage.getInstance().getToken("/stats");
        APIUtils.getPepAPIService().getServiceStats(token.getSub(),url,token.toJsonString()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if(response.code()==200){
                    Intent intent=new Intent(StatsActivity.this,ServiceStatsActivity.class);
                    Log.d(TAG,response.body().toString());
                    intent.putExtra("serviceName",service.getName().get(Utils.langCode()));
                    intent.putExtra("data",response.body().toString());
                    startActivity(intent);
                }else if(response.code()==401){
                    AlertDialog.Builder builder = new AlertDialog.Builder(StatsActivity.this);
                    builder.setTitle("Unauthorized").setMessage("Could not retrieve stats data: PEP denied access").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                } else{ //Other codes returned are not as relevant (internal server error/wrong request...)
                    AlertDialog.Builder builder = new AlertDialog.Builder(StatsActivity.this);
                    builder.setTitle("PEP error").setMessage("Could not retrieve stats data: PEP response with code "+response.code()).setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(StatsActivity.this);
                builder.setTitle("PEP error").setMessage("Could not retrieve stats data: Communication with PEP failed").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.d(TAG,"PEP OnFailure ",t);
            }
        });
    }
}