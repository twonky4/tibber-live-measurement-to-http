package de.viseit.tibber.domain.messaging;

import java.util.Collections;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Subscription implements IdMessage {
	public static final String TYPE = "subscribe";

	private final Map<String, String> variables = Collections.emptyMap();
	private final Map<String, String> extensions = Collections.emptyMap();
	private final String query;
}
