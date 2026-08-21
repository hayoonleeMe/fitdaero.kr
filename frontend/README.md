# FitDaero Frontend

FitDaero의 간편 추천 웹 클라이언트다. React, TypeScript, Vite, Tailwind CSS로 구성한다.

## 실행

Node.js 22와 npm이 필요하다.

```bash
npm install
npm run dev
```

개발 서버는 `/api` 요청을 로컬 Spring Boot 서버로 프록시한다. 백엔드는 기본 포트 `8080`에서 실행한다.

운영 환경도 같은 도메인의 `/api` 경로를 백엔드로 프록시한다.

## 검증

```bash
npm run lint
npm run test
npm run build
```
