package de.viseit.tibber.domain.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Data {
	private final BigDecimal powerConsumption;
	private final BigDecimal powerProduction;
	private final BigDecimal meterConsumption;
	private final BigDecimal meterProduction;
}
