package de.viseit.tibber.domain.messaging;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NextMessage implements IdMessage {
	public static final String TYPE = "next";

	private Data data;

	@Setter
	@Getter
	public static class Data {
		private LiveMeasurement liveMeasurement;
	}

	@Setter
	@Getter
	public static class LiveMeasurement {
		OffsetDateTime timestamp;
		BigDecimal power;
		BigDecimal powerProduction;
		BigDecimal lastMeterConsumption;
		BigDecimal lastMeterProduction;
		BigDecimal price;
	}
}
