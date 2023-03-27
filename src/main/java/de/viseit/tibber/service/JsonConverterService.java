package de.viseit.tibber.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.lang.reflect.Field;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.viseit.tibber.domain.messaging.IdMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JsonConverterService {
	private final ObjectMapper mapper;

	public String convertMessage(Object payload) {
		Field field = null;
		try {
			field = payload.getClass().getField("TYPE");
		} catch (NoSuchFieldException | SecurityException e) {
			log.error("usage of wrong message type", e);
			return "";
		}
		String type = null;
		try {
			type = (String) field.get(null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.error("usage of wrong message type", e);
			return "";
		}

		Message message = Message.builder()
				.id(payload instanceof IdMessage ? UUID.randomUUID().toString() : null)
				.payload(payload)
				.type(type)
				.build();

		try {
			return mapper.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			log.error("can not write message", e);
			return "";
		}
	}

	@Builder
	@Getter
	private static class Message {
		@JsonInclude(NON_NULL)
		private final String id;
		private final String type;
		private final Object payload;
	}
}
