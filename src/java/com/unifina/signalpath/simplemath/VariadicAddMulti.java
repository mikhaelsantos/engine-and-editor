package com.unifina.signalpath.simplemath;

import com.unifina.signalpath.*;
import com.unifina.signalpath.variadic.VariadicInput;
import com.unifina.signalpath.variadic.InputInstantiator;

import java.util.Map;

public class VariadicAddMulti extends AbstractSignalPathModule {

	private TimeSeriesInput in1 = new TimeSeriesInput(this, "in1");
	private TimeSeriesInput in2 = new TimeSeriesInput(this, "in2");
	private VariadicInput<Double> variadicInput = new VariadicInput<>("in", this, new InputInstantiator.TimeSeries(), 3);
	private TimeSeriesOutput out = new TimeSeriesOutput(this, "sum");

	@Override
	public void init() {
		addInput(in1);
		addInput(in2);
		addVariadic(variadicInput);
		addOutput(out);
	}

	public void clearState() {}

	public void sendOutput() {
		double sum = 0;
		sum += in1.getValue();
		sum += in2.getValue();
		for (Double val : variadicInput.getValues()) {
			sum += val;
		}
		out.send(sum);
	}
}
