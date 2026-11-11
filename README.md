# J2-week07 · 챕터 08 회원가입 (REST API)

> 멋쟁이사자처럼 14기 단국대 백엔드 PBL 스터디 7주차
> 참고 코드: [jhs512/demo03-2024](https://github.com/jhs512/demo03-2024) (챕터 08 범위)

이번 주 목표는 **REST API 방식으로 회원가입 API를 구현하는 것**이다.
이 문서에는 수업 내용 전체가 아니라, 수업 중에 헷갈렸거나 새로 알게 된 부분만 정리했다.

---

## 0. 커밋으로 따라가기

수업 스텝 하나가 커밋 하나다. `git log`를 보면서 스텝별 변경을 비교할 수 있다.

| 스텝 | 커밋 메시지 | 핵심 변경 |
|---|---|---|
| 08-02 | 회원가입 API 구현 | `MemberController` (GET `/join`, 컨트롤러에서 직접 유효성 체크) |
| 08-03 | Ut 클래스 | `Ut.str.isBlank`, `Ut.str.hasLength` |
| 08-05 | ExceptionHandler | `GlobalExceptionHandler` (`@ControllerAdvice`) |
| 08-06 | 사람 / 기계 손님 | `ArticleController` `/article/list` vs `/article/listJson` |
| 08-09 | ApiV1MemberController | `@RestController`, `@PostMapping`, `@RequestBody` |
| 08-10 | 상태코드 명시 | `ResponseEntity`로 400 응답 |
| 08-11 | 성공/실패 양식 통일 | `fail-on-empty-beans: false` |
| 08-12 | Validation | `spring-boot-starter-validation`, `@NotBlank`, `@Valid` |
| 08-13 | Validation 예외 처리 | `MethodArgumentNotValidException` 핸들러 |
| 08-14 | 기타 예외 처리 | `Exception` 핸들러 (trace, path), `Rq` |
| 08-15 | 응답본문 클래스 | `MemberJoinRespBody`, `RsData.newDataOf` |
| 08-16 | RsData → 응답코드 | `ResponseAspect` (`@Aspect`, `@Around`) |

> 08-01, 04, 07, 08 스텝은 개념 설명 위주라 코드 변경이 없다.
> 배포용 파일(`.github/workflows`, `Dockerfile`, `fly.toml`, `infraScript`)은 이번 주 내용과 관계가 없어서 뺐다.

---

## 1. 예외 상황은 `return`보다 `throw` (08-04, 08-05)

### 왜 `return`이 불편한가
`return`으로 실패를 알리려면 **리턴 타입이 허용하는 범위 안에서** 실패를 표현해야 한다.

```java
// 리턴 타입이 Member면 실패를 표현할 방법이 null 정도밖에 없다.
// "아이디 중복"인지 "비밀번호 누락"인지 호출자는 알 수 없다.
public Member join(...) {
    if (중복) return null;
}
```

억지로 표현하려면 리턴 타입을 바꾸거나 이상한 분기 코드가 생긴다.
`throw`는 리턴 타입과 상관없이 **원하는 만큼 자세한 실패 정보**를 실어 보낼 수 있다.

```java
throw new GlobalException("400-1", "%s(은)는 이미 존재하는 아이디입니다.".formatted(username));
```

### `return`과 `throw`가 끝내는 범위가 다르다
- `return`: **현재 메서드만** 끝난다.
- `throw`: 호출자가 잡아주지(`try-catch`) 않으면 **호출 스택을 계속 거슬러 올라간다.** 결국 그 HTTP 요청 처리 전체가 중단된다.
- 아무도 잡지 않으면 스프링부트가 기본 방식(Whitelabel Error Page 또는 기본 JSON)으로 오류를 응답한다.

### `@ControllerAdvice` + `@ExceptionHandler`
컨트롤러 어디에서 던진 예외든 **한 곳에서 가로채서** 원하는 응답으로 바꿀 수 있다.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GlobalException.class)      // 우리가 직접 던진 예외
    @ExceptionHandler(MethodArgumentNotValidException.class) // @Valid 실패
    @ExceptionHandler(Exception.class)            // 그 외 모든 예외
}
```

- 핸들러가 여러 개면 스프링이 **가장 구체적인(가장 잘 맞는) 예외 타입**의 핸들러 하나를 골라 실행한다.
  `GlobalException`이 터지면 `Exception` 핸들러가 아니라 `GlobalException` 핸들러가 실행된다.
- 맞는 핸들러가 하나도 없으면 아무것도 실행되지 않고 스프링 기본 처리로 넘어간다.

---

## 2. `Ut` 클래스 안에 `static class`를 하나 더 두는 이유 (08-03)

```java
public class Ut {
    public static class str {
        public static boolean isBlank(String str) { return str == null || str.isBlank(); }
        public static boolean hasLength(String str) { return !isBlank(str); }
    }
}
// 사용: Ut.str.isBlank(username)
```

- 유틸 함수가 늘어나면 `Ut` 한 파일에 문자열·날짜·JSON 함수가 뒤섞인다.
- **중첩 static 클래스로 분류**해 두면 `Ut.str.`, `Ut.date.`처럼 이름 공간이 생겨서 찾기 쉽다.
- 폴더 하나에 파일을 너무 많이 넣지 않고 하위 폴더로 나누는 것과 같은 발상이다.
- 참고: `String.isBlank()`(Java 11+)는 공백만 있는 문자열(`"   "`)도 빈 것으로 본다. `isEmpty()`는 길이 0만 빈 것으로 본다.

---

## 3. 사람 손님과 기계 손님 (08-06, 08-07)

| | 사람 (브라우저) | 기계 (프론트엔드 앱, 다른 서버) |
|---|---|---|
| 필요한 것 | 정보 + 꾸밈 | 정보만 |
| 응답 형식 | HTML, CSS, JS | JSON (또는 XML) |
| 주로 쓰는 HTTP 메서드 | GET (폼 전송 시 POST) | GET, POST, PUT, PATCH, DELETE |
| 부가 정보 전달 | 쿼리스트링 | 쿼리스트링 + **요청 본문(body)** |
| 스프링 쪽 기술 | 타임리프, JSP | `@RestController` |

**"스프링부트로 REST API를 만든다"**고 하면 이렇게 이해하면 된다.
- 스프링부트는 HTML/CSS/JS 없이 **정보 처리만** 한다.
- 화면은 리액트·뷰·스벨트 같은 **별도 프론트엔드 앱**이 그린다.
- 두 앱은 HTTP + JSON으로 통신한다.

### 왜 POSTMAN이 필요한가
브라우저 주소창으로는 **GET 요청만** 보낼 수 있고 요청 본문도 넣을 수 없다.
API 서버를 테스트하려면 POST/PUT/DELETE와 JSON 본문을 보낼 수 있는 도구가 필요하다. 그 도구가 POSTMAN이다.

```
POST http://localhost:8070/api/v1/members
Content-Type: application/json

{ "username": "user10", "password": "1234", "nickname": "유저10" }
```

---

## 4. HTTP 메서드와 명사형 URL (08-08)

**행위는 HTTP 메서드가, 대상은 URL이** 표현한다. 그래서 URL에서 동사(`write`, `modify`, `delete`)가 빠지고 명사(복수형)만 남는다.

| 하려는 일 | ❌ 잘못된 방법 | ✅ 올바른 방법 |
|---|---|---|
| 생성 | `GET /article/write?title=..` | `POST /articles` + JSON body |
| 다건 조회 | `GET /article/list` | `GET /articles` |
| 단건 조회 | `GET /article/detail?id=1` | `GET /articles/1` |
| 수정 | `GET /article/modify?id=1&..` | `PUT /articles/1` + JSON body |
| 삭제 | `GET /article/delete?id=1` | `DELETE /articles/1` |

> **PUT vs PATCH**: PUT은 리소스를 **통째로 교체**, PATCH는 **일부 필드만 수정**하는 것이 원래 의미다. 수업에서는 둘 다 "수정"으로 묶었다.

---

## 5. `ApiV1MemberController`에서 바뀐 것들 (08-09)

```java
@RestController                     // ① 
@RequestMapping("/api/v1/members")  // ②
public class ApiV1MemberController {
    @PostMapping("")                 // ③
    public RsData<MemberJoinRespBody> join(
            @RequestBody @Valid MemberJoinReqBody reqBody  // ④
    ) { ... }
}
```

1. **`@RestController` = `@Controller` + 모든 메서드에 `@ResponseBody`**
   리턴값을 뷰 이름으로 보지 않고 그대로 JSON으로 바꿔서 응답한다.
2. **`/api/v1/`**: URL에 API 버전을 붙인다. 나중에 응답 형식이 크게 바뀌면 `/api/v2/`를 새로 만든다. v1을 쓰는 프론트엔드는 깨지지 않고 천천히 옮겨갈 수 있다.
3. **`@PostMapping`**: POST 요청에만 반응한다. 같은 URL로 GET을 보내면 405(Method Not Allowed)가 난다.
4. **`@RequestBody`**: 쿼리스트링이 아니라 **요청 본문의 JSON**을 `MemberJoinReqBody` 객체로 변환(역직렬화)해서 받는다.

> 헷갈렸던 점: `@RequestBody`가 없으면 스프링은 쿼리스트링/폼 데이터에서 값을 찾는다. JSON 본문으로 보냈는데 값이 전부 `null`이면 `@RequestBody`를 빠뜨린 것이다.

---

## 6. HTTP 상태코드 지정하기 (08-10, 08-16)

| 범위 | 의미 |
|---|---|
| 2xx | 성공 |
| 4xx | 실패, **클라이언트 잘못** (입력값 누락, 중복 아이디 등) |
| 5xx | 실패, **서버 잘못** (버그, DB 장애 등) |

예외를 잡아서 JSON을 돌려주면 **상태코드가 기본값 200**으로 나간다. "실패했는데 200"이 되지 않도록 직접 지정해야 한다.

| 방법 | 특징 |
|---|---|
| `@ResponseStatus(HttpStatus.BAD_REQUEST)` | 메서드에 붙인다. 항상 400으로 **고정**된다. |
| `ResponseEntity<T>` | `ResponseEntity.status(코드).body(본문)`처럼 **상황마다 다른 코드**를 줄 수 있다. |

이 프로젝트에서는 `resultCode`의 앞자리(`"400-1"` → 400)로 `statusCode`를 정하고, 그 값을 실제 HTTP 응답코드로 쓴다.

---

## 7. 성공/실패 응답 양식 통일: `RsData` (08-11, 08-15)

프론트엔드가 응답을 **항상 같은 모양으로 파싱**할 수 있도록 성공이든 실패든 같은 형식을 쓴다.

```jsonc
// 성공
{ "resultCode": "200-1", "statusCode": 200, "msg": "회원가입이 완료되었습니다.", "data": { "item": { ... } } }
// 실패
{ "resultCode": "400-1", "statusCode": 400, "msg": "user10(은)는 이미 존재하는 아이디입니다.", "data": {} }
```

- **`resultCode`**: `"상태코드-세부번호"`. 같은 400이라도 원인을 구분할 수 있다. 번호를 정하는 절대적인 규칙은 없고 팀에서 정하면 된다.
- **`@JsonIgnore`**: `isSuccess()`, `isFail()`은 getter처럼 생겨서 JSON에 `"success": true`로 들어가 버린다. 이를 막으려고 붙였다.
- **`newDataOf(data)`**: 서비스가 준 `RsData<Member>`의 코드/메시지는 그대로 두고 `data`만 응답용 객체로 바꾼다.

### 요청 본문 클래스 / 응답 본문 클래스
액션 메서드(엔드포인트) 하나당 보통 클래스 2개를 만든다.
- `MemberJoinReqBody`: 받을 값 (입력이 없으면 필요 없다)
- `MemberJoinRespBody`: 돌려줄 값

엔티티를 그대로 받거나 내보내지 않고 전용 클래스를 두면 **API 스펙이 엔티티 구조와 분리된다.** 나중에 비밀번호 같은 필드를 응답에서 빼기도 쉽다.

### Jackson과 `fail-on-empty-beans`
- **Jackson**은 객체 ↔ JSON ↔ Map 변환을 해주는 라이브러리다. 스프링부트가 `@ResponseBody` 리턴값을 JSON으로 바꿀 때 쓴다.
- Jackson은 기본적으로 **필드도 getter도 없는 빈 객체**(`Empty`)를 JSON으로 바꾸려 하면 예외를 던진다.
- 실패 응답의 `"data": {}`를 만들려면 이걸 꺼야 한다.

```yaml
spring:
  jackson:
    serialization:
      fail-on-empty-beans: false
```

---

## 8. Validation (08-12, 08-13)

컨트롤러마다 `if (Ut.str.isBlank(...)) throw ...`를 쓰던 것을 **어노테이션 규칙**으로 바꾼다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

```java
public static class MemberJoinReqBody {
    @NotBlank private String username;
    ...
}
public RsData<...> join(@RequestBody @Valid MemberJoinReqBody reqBody)
```

- 필드에는 **규칙**(`@NotBlank`)을 붙이고, 파라미터에는 **"검사해라"**(`@Valid`)를 붙인다. `@Valid`를 빠뜨리면 규칙이 있어도 검사하지 않는다.
- 자주 쓰는 규칙

  | 어노테이션 | 통과 조건 |
  |---|---|
  | `@NotNull` | `null`만 아니면 된다 (`""` 통과) |
  | `@NotEmpty` | `null`도 `""`도 아니어야 한다 (`" "` 통과) |
  | `@NotBlank` | 공백 문자만 있어도 안 된다 (문자열 전용, 가장 엄격) |
  | `@Min`, `@Max`, `@Size`, `@Email`, `@Pattern` | 범위, 길이, 형식 |

- 검사에 실패하면 컨트롤러 메서드에 들어가기 전에 **`MethodArgumentNotValidException`**이 발생한다.
- 08-13에서는 이 예외를 잡아 `GlobalException` 형태로 바꿔서 **응답 양식을 통일**했다.

```java
String resultCode = "400-" + ex.getBindingResult().getFieldError().getCode(); // 예: 400-NotBlank
String msg = ex.getBindingResult().getFieldError().getField() + " : "
           + ex.getBindingResult().getFieldError().getDefaultMessage();     // 예: username : 공백일 수 없습니다
```

> `getFieldError()`는 실패한 필드 중 **첫 번째 하나만** 준다. 여러 필드가 동시에 틀려도 메시지는 하나만 나간다.

---

## 9. 기타 예외 처리와 `Rq` (08-14)

`@ExceptionHandler(Exception.class)`는 위에서 처리하지 못한 **나머지 모든 예외**를 잡는다. 이게 없으면 예상하지 못한 오류가 스프링 기본 형식으로 나가서 응답 양식이 깨진다.

- `trace`: `printStackTrace`를 문자열로 받아 담은 것. 어디서 터졌는지 보여준다.
  운영 환경에서는 내부 구조가 노출되므로 보통 빼거나 개발 환경에서만 보여준다.
- `path`: 오류가 난 요청 URL.

### `Rq`
요청(`HttpServletRequest`)과 응답(`HttpServletResponse`)을 감싼 **요청 단위 도우미 객체**다.

```java
@Component
@RequestScope   // HTTP 요청마다 새 Rq 객체가 만들어진다
public class Rq {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    public String getCurrentUrlPath() { return req.getRequestURI(); }
    public void setStatusCode(int statusCode) { resp.setStatus(statusCode); }
}
```

- **`@RequestScope`가 핵심이다.** 일반 빈은 싱글톤(앱 전체에 하나)인데, `Rq`는 요청마다 따로 생긴다. 그래서 동시에 들어온 요청끼리 섞이지 않는다.
- 컨트롤러/서비스/핸들러가 서블릿 API를 직접 만지지 않고 `rq.xxx()`만 호출하면 된다.

---

## 10. AOP로 `RsData` 상태코드를 실제 응답코드에 반영 (08-16)

### 문제
컨트롤러가 `RsData`를 그냥 리턴하면 JSON 안의 `statusCode`가 201이든 400이든 **실제 HTTP 응답코드는 항상 200**이다.
메서드마다 `ResponseEntity`로 감싸면 코드가 지저분해진다.

### AOP(관점 지향 프로그래밍)
여러 메서드에 공통으로 들어가는 앞뒤 작업(로깅, 트랜잭션, 응답 가공 등)을 **한 곳에 모아 두고 규칙으로 끼워 넣는** 방식이다.

| 용어 | 뜻 |
|---|---|
| Aspect | 끼워 넣을 공통 기능을 모은 클래스 (`@Aspect`) |
| Pointcut | **어디에** 끼울지 정하는 규칙 (`@Around("...")` 안의 식) |
| Advice | **언제** 실행할지 (`@Before`, `@After`, `@Around` 등) |
| JoinPoint | 실제로 가로챈 메서드 실행 지점 |

```java
@Around("""
    ( within(@RestController *) && (@annotation(GetMapping) || @annotation(PostMapping) || ...) )
    || @annotation(ResponseBody)
""")
public Object handleResponse(ProceedingJoinPoint joinPoint) throws Throwable {
    Object proceed = joinPoint.proceed();   // ← 원래 컨트롤러 메서드 실행
    if (proceed instanceof RsData rsData) {
        rq.setStatusCode(rsData.getStatusCode()); // ← 실행 "후"에 응답코드 설정
    }
    return proceed;
}
```

- 포인트컷 해석: "`@RestController` 클래스 안의 `@GetMapping`/`@PostMapping`/... 메서드" **또는** "`@ResponseBody`가 붙은 메서드".
- `@Around`는 원래 메서드 실행 전후를 모두 감싼다. **`joinPoint.proceed()`를 호출해야** 원래 메서드가 실행된다. 빠뜨리면 컨트롤러가 아예 실행되지 않는다.
- 결과: 컨트롤러는 `RsData`만 리턴하면 되고, 응답코드는 Aspect가 알아서 맞춘다.

> 헷갈렸던 점: `build.gradle`에 `spring-boot-starter-aop`가 없는데 `@Aspect`가 동작한다.
> Spring Boot 3.2에서는 `spring-boot-starter-data-jpa`가 AOP 스타터를 함께 끌어오기 때문이다.
> JPA 없이 AOP만 쓰는 프로젝트라면 `implementation 'org.springframework.boot:spring-boot-starter-aop'`를 직접 추가해야 한다.

> 수업 08-01: AOP, Aspect, Validation은 **규칙 기반** 기능이다. 원리는 깊어도 적용 자체는 "규칙을 선언하면 프레임워크가 알아서 끼워 넣는" 식이라 비교적 쉽다.

---

## 실행 방법

```bash
./gradlew bootRun     # http://localhost:8070
```

- `application.yml`이 `secret` 프로필을 include하지만, `application-secret.yml`이 없어도 실행된다 (`.gitignore` 처리됨).
- 개발 DB는 H2 파일 DB(`./db_dev`)를 쓴다.
- Redis 자동설정은 `application.yml`에서 제외되어 있어 Redis 없이 실행된다.
