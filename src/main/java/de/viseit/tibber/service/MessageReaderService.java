package de.viseit.tibber.service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.viseit.tibber.domain.messaging.ConnectionAck;
import de.viseit.tibber.domain.messaging.NextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageReaderService {
	private final ObjectMapper mapper;
	private static final Map<String, Class<?>> TYPES = new HashMap<>();

	static {
		TYPES.put(ConnectionAck.TYPE, ConnectionAck.class);
		TYPES.put(NextMessage.TYPE, NextMessage.class);
	}

	public Object read(String message) {
		JsonNode jsonNode = null;
		try {
			jsonNode = mapper.readTree(message);
		} catch (JsonProcessingException e) {
			log.error("invalid incomming message", e);
			return null;
		}
		String type = jsonNode.get("type").asText();

		Class<?> clz = TYPES.get(type);
		if (clz == null) {
			log.error("no class configured for type {}", type);
			return null;
		}
		if (!jsonNode.has("payload")) {
			try {
				return clz.getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				log.error("can not create instance of type {}", type, e);
				return null;
			}
		} else {
			try {
				return mapper.treeToValue(jsonNode.get("payload"), clz);
			} catch (JsonProcessingException e) {
				log.error("can not create instance of type {}", type, e);
				return null;
			}
		}
	}
}
