/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// Olympic scoring model considers the average of the last k weeks
// (dropping the b highest and lowest values) as the current prediction.

package com.yahoo.egads.models.tsmm;

import com.yahoo.egads.data.*;
import com.yahoo.egads.data.TimeSeries.Entry;
import org.json.JSONObject;
import org.json.JSONStringer;
import java.util.Properties;
import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.ForecastingModel;
import net.sourceforge.openforecast.DataPoint;
import net.sourceforge.openforecast.Observation;
import java.util.*;

// A moving average forecast model is based on an artificially constructed time series in which the value for a
// given time period is replaced by the mean of that value and the values for some number of preceding and succeeding time periods.
public class MovingAverageModel extends TimeSeriesAbstractModel {
    // methods ////////////////////////////////////////////////

    // The model that will be used for forecasting.
    private ForecastingModel forecaster;
    
    // The default value
    private int period = 2;
    
    // Stores the historical values.
    private TimeSeries.DataSequence data;

    public MovingAverageModel(Properties config) {
        super(config);
        modelName = "MovingAverageModel";
        //
	String moveStep = config.getProperty("MOVE_STEPS");
	if (moveStep != null) {
		try {
			int ms = Integer.parseInt(moveStep);
			if (ms > period) {
				this.period = ms;
			}
		} catch (NumberFormatException e) {
			logger.warn("Wrong move step value set for MovingAverageModel: " + moveStep, e);
		}
	}
    }

    public void reset() {
        // At this point, reset does nothing.
    }
    
    public void train(TimeSeries.DataSequence data) {
        this.data = data;
        int n = data.size();
        DataPoint dp = null;
        DataSet observedData = new DataSet();
        for (int i = 0; i < n; i++) {
            dp = new Observation(data.get(i).value);
            dp.setIndependentValue("x", i);
            observedData.add(dp);
        }
        observedData.setTimeVariable("x"); 
        
	forecaster = new net.sourceforge.openforecast.models.MovingAverageModel(this.period);
        forecaster.init(observedData);
        initForecastErrors(forecaster, data);
        
        logger.debug(getBias() + "\t" + getMAD() + "\t" + getMAPE() + "\t" + getMSE() + "\t" + getSAE() + "\t" + 0 + "\t" + 0);
    }
  
    public void update(TimeSeries.DataSequence data) {

    }

    public String getModelName() {
        return modelName;
    }

    public void predict(TimeSeries.DataSequence sequence) throws Exception {
          int n = data.size();
          DataSet requiredDataPoints = new DataSet();
          DataPoint dp;

          for (int count = 0; count < n; count++) {
              dp = new Observation(0.0);
              dp.setIndependentValue("x", count);
              requiredDataPoints.add(dp);
          }
          forecaster.forecast(requiredDataPoints);

          // Output the results
          Iterator<DataPoint> it = requiredDataPoints.iterator();
          int i = 0;
          while (it.hasNext()) {
              DataPoint pnt = ((DataPoint) it.next());
              logger.info(data.get(i).time + "," + data.get(i).value + "," + pnt.getDependentValue());
              sequence.set(i, (new Entry(data.get(i).time, (float) pnt.getDependentValue())));
              i++;
          }
    }

    public void toJson(JSONStringer json_out) {

    }

    public void fromJson(JSONObject json_obj) {

    }
}
