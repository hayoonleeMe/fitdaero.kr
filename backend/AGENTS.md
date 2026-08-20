# Backend 작업 지침

## 코드 형식

- 멤버가 있는 클래스·인터페이스·enum 본문에서는 여는 중괄호 다음과 첫 멤버 사이를 한 줄 띄운다.
- 반복적인 보일러플레이트를 줄이고 가독성을 높일 수 있으면 Lombok 사용을 권장한다. 다만 생성자 로직·명시적 `equals`/`hashCode`처럼 동작을 직접 정의해야 하면 명시적으로 작성하고, JPA 엔티티에는 `@Data`를 사용하지 않는다.

## 데이터베이스

- 스키마 변경은 Flyway 마이그레이션으로만 적용하고, `spring.jpa.hibernate.ddl-auto=validate`를 유지한다.
- JPA·Flyway·MySQL 동작 변경은 Testcontainers MySQL 통합 테스트로 검증한다.
