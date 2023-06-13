package inf.um.pilotomimurcia.utils.plotdevice;

import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.ColorInt;

import com.androidplot.xy.XYGraphWidget;

public class QuickCustomPaint extends Paint {

    public QuickCustomPaint(@ColorInt int color, int fontSize, Align align){
        super();
        this.setColor(color);
        this.setAntiAlias(true);
        this.setTextAlign(align);
        this.setTextSize(fontSize);
    }
}
