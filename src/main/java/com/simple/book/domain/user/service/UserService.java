package com.simple.book.domain.user.service;


import com.simple.book.domain.jwt.dto.AuthTokenDto;
import com.simple.book.domain.jwt.dto.CustomUserDetails;
import com.simple.book.domain.oauth2.CustomOAuth2User;
import com.simple.book.domain.user.dto.request.SignupRequestDto;
import com.simple.book.domain.user.entity.Authentication;
import com.simple.book.domain.user.entity.User;
import com.simple.book.domain.user.repository.AuthenticationRepository;
import com.simple.book.domain.user.util.Address;
import com.simple.book.domain.user.util.InfoSet;
import com.simple.book.domain.user.util.Role;
import com.simple.book.global.advice.ErrorCode;
import com.simple.book.global.exception.AuthenticationFailureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import com.simple.book.domain.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
	private final UserRepository userRepository;
	private final AuthenticationRepository authenticationRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	@Transactional(rollbackFor = {Exception.class})
	public String remove(String userId) {
		Authentication authentication = authenticationRepository.findByUserId(userId);
		authenticationRepository.delete(authentication);
		return "회원탈퇴 완료";
	}
	//회원가입
	@Transactional(rollbackFor = {Exception.class})
	public User signup(SignupRequestDto signupRequestDto) {
//		log.info("signup password : " + signupRequestDto.getPassword());
		Authentication authentication = Authentication.builder()
				.userId(signupRequestDto.getUserId())
				.email(signupRequestDto.getEmail())
				.password(bCryptPasswordEncoder.encode(signupRequestDto.getPassword()))
				.infoSet(InfoSet.DEFAULT)
				.build();
		authenticationRepository.save(authentication);

		User user = User.builder()
				.username(signupRequestDto.getUsername())
				.role(Role.USER)
				.nickname(signupRequestDto.getNickname())
				.build();
		user.setAuthentication(authentication);
		authentication.setUser(user);
		userRepository.save(user);
		return user;
	}

	//로그인
	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		//DB에서 조회
		Authentication authentication = authenticationRepository.findByUserId(userId);
		if (authentication != null) {
			AuthTokenDto authTokenDto = AuthTokenDto.builder()
					.infoSet(authentication.getInfoSet().toString())
					.name(authentication.getUser().getUsername())
					.username(authentication.getUserId())
					.password(authentication.getPassword())
					.role(authentication.getUser().getRole().toString())
					.build();

			//UserDetails에 담아서 return하면 AutneticationManager가 검증 함
			return new CustomUserDetails(authTokenDto);
		}
		throw new AuthenticationFailureException("아이디가 잘못되었습니다.", ErrorCode.USER_FAILED_AUTHENTICATION);
	}
	public String getCurrentUserId() {
		org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			if (authentication instanceof OAuth2AuthenticationToken) {
				CustomOAuth2User oauthToken = (CustomOAuth2User) authentication.getPrincipal();
				return oauthToken.getUsername(); // OAuth2로 인증된 경우 사용자 ID 추출
			} else if (authentication instanceof UsernamePasswordAuthenticationToken) {
				CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
				return customUserDetails.getUsername();
			}
		}
		return null; // 사용자가 인증되지 않았거나 인증 정보가 없는 경우
	}

}
