package com.simple.book.domain.alarm.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.simple.book.domain.alarm.dto.AlarmUrlDto;
import com.simple.book.domain.alarm.repository.AlarmUrlRepository;
import com.simple.book.domain.user.entity.User;
import com.simple.book.domain.user.repository.UserRepository;

@Service
public class AlarmUrlService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AlarmUrlRepository alarmUrlRepository;
	
	public String getAlarmUrl(String userId) {
		String result = null;
		if (userId != null) {
			Optional<UUID> oldUrl = alarmUrlRepository.findUrlByAuthenticationUserId(userId);
			if (oldUrl.isPresent()) {
				result = "/topic/user/" + oldUrl.get();
			} else {
				throw new RuntimeException("시스템 오류가 발생하였습니다.");
			}
		} else {
			throw new RuntimeException("로그인 후 이용해 주시길 바랍니다.");
		}
		return result;
	}

	public void createAlarmUrl(long id) {
		User user = userRepository.getReferenceById(id);
		AlarmUrlDto dto = AlarmUrlDto.builder().user(user).build();
		try {
			alarmUrlRepository.saveAndFlush(dto.toEntity());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("시스템 오류가 발생하였습니다.");
		}
	}
}