package com.simple.book.global.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ResponseMessage {
	
	private String message;
	
	@Builder.Default
	private boolean result = true;
}
