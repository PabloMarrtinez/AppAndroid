package inf.um.pilotomimurcia;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;

import com.androidplot.Plot;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.androidplot.ui.SizeMode;
import com.androidplot.util.PixelUtils;
import com.androidplot.util.SeriesUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

import inf.um.pilotomimurcia.rest.models.AnonVsIdentifiedModel;
import inf.um.pilotomimurcia.rest.models.DateValueModel;
import inf.um.pilotomimurcia.rest.models.StatsResponseModel;
import inf.um.pilotomimurcia.utils.plotdevice.ComputationUtils;
import inf.um.pilotomimurcia.utils.plotdevice.QuickCustomPaint;
import inf.um.pilotomimurcia.utils.plotdevice.XAxisStringLabelFormat;

public class ServiceStatsActivity extends AppCompatActivity {

    private static final String TAG=ServiceStatsActivity.class.getSimpleName();
    private XYPlot plotDaysTotal;
    private XYPlot plotDaysUser;
    private PieChart plotAnonVsId;
    private static final float percentagePieDonut=100f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_stats);
        Bundle extras=getIntent().getExtras();
        String serviceName=(String) extras.get("serviceName");
        StatsResponseModel parsed=new Gson().fromJson((String)extras.get("data"), StatsResponseModel.class);
        Log.d(TAG,(String)extras.get("data"));
        List<DateValueModel> monthServiceTotal=parsed.getParsedMonthServ();
        List<DateValueModel> monthServiceUser=parsed.getParsedMonthServUid();
        AnonVsIdentifiedModel anonVsId=parsed.getAnonVsIdentified();

        setupServiceTotalGraph(serviceName,monthServiceTotal);
        setupServiceUserGraph(serviceName,monthServiceUser);
        setupServiceAnonVsId(serviceName,anonVsId);
    }

    private void genericSetupXYgraph(XYPlot plot, List<DateValueModel> values, LineAndPointFormatter seriesFormat) {
        //Axis configuration: boundaries, labels...
        plot.setDomainBoundaries(0, values.size(), BoundaryMode.FIXED);
        int maxY= SeriesUtils.minMax(values.stream().map(DateValueModel::getValue).collect(Collectors.toList())).getMax().intValue();
        plot.setRangeBoundaries(0,maxY+ 0.1*maxY, BoundaryMode.FIXED);
        Log.d(TAG,"Values: "+ values);
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 1);
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, ComputationUtils.getPrettyStep(maxY,6));
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new DecimalFormat("#"));
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setPaint(new QuickCustomPaint(Color.DKGRAY,15, Paint.Align.LEFT));
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setRotation(0);
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

        //Data series
        XYSeries series=new SimpleXYSeries(values.stream().map(DateValueModel::getValue).collect(Collectors.toList()), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Visits");
        plot.addSeries(series,seriesFormat);
    }

    private void setupServiceAnonVsId(String serviceName, AnonVsIdentifiedModel anonVsId) {
        plotAnonVsId=findViewById(R.id.plotAnonVsIdent);
        plotAnonVsId.setTitle("Total visits to "+serviceName);
        final float padding = PixelUtils.dpToPix(30);
        plotAnonVsId.getPie().setPadding(padding, padding, padding, padding);
        plotAnonVsId.setRenderMode(Plot.RenderMode.USE_MAIN_THREAD);
        //plotAnonVsId.getRenderer(PieRenderer.class).setDonutSize(percentagePieDonut/100f,
        //        PieRenderer.DonutMode.PERCENT);
        Segment sAnon = new Segment("Anonymized: "+anonVsId.getAnonymous(), anonVsId.getAnonymous());
        Segment sId = new Segment("Identified: "+anonVsId.getIdentified(), anonVsId.getIdentified());
        EmbossMaskFilter emf = new EmbossMaskFilter(
                new float[]{1, 1, 1}, 0.4f, 10, 8.2f);

        SegmentFormatter sfAnon = new SegmentFormatter(this, R.xml.pie_segment_formatter_anon);
        sfAnon.getLabelPaint().setShadowLayer(2, 0, 0, Color.BLACK);
        sfAnon.getLabelPaint().setColor(Color.RED);
        sfAnon.getLabelPaint().setFakeBoldText(true);
        //sfAnon.getFillPaint().setMaskFilter(emf);
        SegmentFormatter sfId = new SegmentFormatter(this, R.xml.pie_segment_formatter_id);
        sfId.getLabelPaint().setShadowLayer(2, 0, 0, Color.BLACK);
        sfId.getLabelPaint().setFakeBoldText(true);

        //sfId.getFillPaint().setMaskFilter(emf);

        plotAnonVsId.addSegment(sAnon, sfAnon);
        plotAnonVsId.addSegment(sId, sfId);

        plotAnonVsId.getBorderPaint().setColor(Color.TRANSPARENT);
        plotAnonVsId.getBackgroundPaint().setColor(Color.TRANSPARENT);

        plotAnonVsId.getLegend().setVisible(false);
        //plotAnonVsId.getLegend().setWidth(0.9f, SizeMode.RELATIVE);

        plotAnonVsId.redraw();
    }

    private void setupServiceUserGraph(String serviceName, List<DateValueModel> monthServiceUser) {
        plotDaysUser=findViewById(R.id.plotVisitsDayServiceUser);
        plotDaysUser.setDomainLabel("Day");
        plotDaysUser.setRangeLabel("Visits");
        plotDaysUser.setTitle("Your visits to "+serviceName);

        LineAndPointFormatter seriesFormat =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        genericSetupXYgraph(plotDaysUser,monthServiceUser,seriesFormat);

        //Legend
        plotDaysUser.getLegend().setVisible(false);
        plotDaysUser.redraw();
    }

    private void setupServiceTotalGraph(String serviceName, List<DateValueModel> monthServiceTotal) {
        plotDaysTotal=findViewById(R.id.plotVisitsDayServiceTotal);
        plotDaysTotal.setDomainLabel("Day");
        plotDaysTotal.setRangeLabel("Visits");
        plotDaysTotal.setTitle("Total visits to "+serviceName);

        LineAndPointFormatter seriesFormat =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        seriesFormat.setInterpolationParams(new CatmullRomInterpolator.Params(5, CatmullRomInterpolator.Type.Centripetal));
        genericSetupXYgraph(plotDaysTotal,monthServiceTotal,seriesFormat);

        //Legend
        plotDaysTotal.getLegend().setVisible(false);
        plotDaysTotal.redraw();
    }
}