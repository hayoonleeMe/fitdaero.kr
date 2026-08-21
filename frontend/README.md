# FitDaero Frontend

FitDaero의 간편 추천 웹 클라이언트다. React, TypeScript, Vite, Tailwind CSS로 구성한다.

## 실행

Node.js 22와 npm이 필요하다.

```bash
npm install
npm run dev
```

개발 서버는 `/api` 요청을 로컬 Spring Boot 서버로 프록시한다. 백엔드는 기본 포트 `8080`에서 실행한다.

## 환경 변수

개발 중에는 별도 환경 변수가 필요 없다. 배포 환경에서 API 주소를 바꿔야 할 때만 `VITE_API_BASE_URL`을 사용한다.

```bash
VITE_API_BASE_URL=https://api.example.com
```

로컬 값은 `.env.local`에 두며 Git에 커밋하지 않는다.

## 검증

```bash
npm run lint
npm run build
```
