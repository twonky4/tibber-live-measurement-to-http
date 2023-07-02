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
		private OffsetDateTime timestamp;
		private BigDecimal power;
		private BigDecimal powerProduction;
		private BigDecimal lastMeterConsumption;
		private BigDecimal lastMeterProduction;
		private BigDecimal price;
		private DayPrices dayPrices;
	}

	@Setter
	@Getter
	public static class DayPrices {
		private BigDecimal price00;
		private BigDecimal price01;
		private BigDecimal price02;
		private BigDecimal price03;
		private BigDecimal price04;
		private BigDecimal price05;
		private BigDecimal price06;
		private BigDecimal price07;
		private BigDecimal price08;
		private BigDecimal price09;
		private BigDecimal price10;
		private BigDecimal price11;
		private BigDecimal price12;
		private BigDecimal price13;
		private BigDecimal price14;
		private BigDecimal price15;
		private BigDecimal price16;
		private BigDecimal price17;
		private BigDecimal price18;
		private BigDecimal price19;
		private BigDecimal price20;
		private BigDecimal price21;
		private BigDecimal price22;
		private BigDecimal price23;
	}
}
