package com.boot.cleanhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <pre>
 *   애플리케이션 진입점 (내장 톰캣 실행용).
 *   외장 톰캣(WAR) 배포 시의 부트스트랩은 ServletInitializer 가 전담한다.
 * </pre>
 */
@SpringBootApplication
public class CleanhubApplication {

	public static void main(String[] args) {
		SpringApplication.run(CleanhubApplication.class, args);
	}

}
