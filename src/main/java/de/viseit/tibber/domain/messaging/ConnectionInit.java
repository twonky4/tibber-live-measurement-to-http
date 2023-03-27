package de.viseit.tibber.domain.messaging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ConnectionInit {
	public static final String TYPE = "connection_init";

	private final String token;
}
