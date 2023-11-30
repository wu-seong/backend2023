# 실행환경
memo.py 는 Python3 Flask 로 되어있습니다. 본 실행환경은 과제 제출용이기 때문에 localhost로 redirect를 받아 간단하게 아래 실행 예시를 보고 동작할 수 있습니다.
배포환경에서는 로드밸런서, nginx, uwsgi를 거쳐 Flask가 요청을 수행하고, redirecturl 또한 로드밸런서의 DNS주소로 두었습니다.

# 필요 패키지 설치

requests~=2.31.0
redis~=5.0.1
urllib3~=2.1.0
flask~=3.0.0

# 실행 예시

일반적인 flask 실행 방식대로 실행하면 됩니다.

```
$ flask --app memo run --port 포트 번호
```
또는
```
$ python3 memo.py
```

후자의 경우 memo.py 안에서 port 번호 8000번을 기본값으로 사용하고 있으니 필요시 수정하세요.

# 동작 설명

## index.html 읽어 오기

memo.py 를 실행하고 브라우저에서 `http://localhost:포트번호` 처럼 접근할 경우 `index.html` 을 읽어오게 됩니다.

이는 `Flask` 의 `template` 기능을 사용하고 있으며, 사용되고 있는 `index.html` 의 template file 은 `templates/index.html` 에 위치하고 있습니다.

이 template 은 현재 `name` 이라는 변수만을 외부 변수 값으로 입력 받습니다. 해당 변수는 유저가 현재 로그인 중인지를 알려주는 용도로 사용되며 `index.html` 은 그 값의 유무에 따라 다른 내용을 보여줍니다.

## index.html 이 호출하는 REST API 들

`index.html` 은 `memo.py` 에 다음 API 들을 호출합니다.

* `GET /login` : authorization code 를 얻어오는 URL 로 redirect 시켜줄 것을 요청합니다. (아래 설명)

* `GET /memo` : 현재 로그인한 유저가 작성한 메모 목록을 JSON 으로 얻어옵니다. 결과 JSON 은 다음과 같은 형태가 되어야 합니다.
  ```
  {"memos": ["메모내용1", "메모내용2", ...]}
  ```

* `POST /memo` : 새 메모를 추가합니다. HTTP 요청은 다음과 같은 JSON 을 전송해야 됩니다.
  ```
  {"text": "메모내용"}
  ```
  새 메모가 생성된 경우 memo.py 는 `200 OK` 를 반환합니다.


## 네이버 로그인 API 호출

수업 시간에 설명한대로 authorization code 를 얻어오는 동작은 클라이언트에서부터 시작하게 됩니다.

그런데 코드를 보면 `index.html` 에서 해당 API 동작을 바로 시작하는 것이 아니라 `GET /login` 을 통해서 서버에게 해당 REST API 로 redirect 시켜달라고 하는 방식으로 브라우저가 API 를 호출합니다. 이는 Chrome 계열의 브라우저의 `CORS` 문제 때문에 그렇습니다.

비록 서버가 redirect 해주는 방식을 사용하고는 있지만, 클라이언트인 브라우저가 그 API 를 직접 호출한다는 점은 동일합니다.

## 로그인 혹은 가입 처리

네이버 OAuth 과정을 마무리 한 뒤에 네이버의 profile API 를 통해 얻은 유저의 고유 식별 번호를 갖는 유저가 DB 에 없는 경우 새 유저로 취급하고 DB 에 해당 유저의 이름을 포함하는 레코드를 생성합니다.

만일 같은 네이버 고유 식별 번호의 유저가 있다면 그냥 로그인 된 것으로 간주합니다.

어떤 경우든 DB 에서 해당 유저의 정보를 얻어낼 수 있도록 `userId` 라는 `HTTP cookie` 를 설정합니다.

## `def home()`

`userId` 쿠키가 설정되어 있는 경우 DB 에서 해당 유저의 이름을 읽어와서 `index.html` template 에 이름을 반영합니다.

## Redirect URI 에 대응되는 함수 (예: `def onOAuthAuthorizationCodeRedirected()`)

본인이 네이버 앱 등록시 설정한 Redirect URI 에 대응되는 주소의 handler 를 `memo.py` 에 구현해야됩니다. 현재 `memo.py` 가 8000 번 포트에 뜰 것이라고 가정하고 `http://localhost:8000/auth` 와 같은 형태로 네이버 앱 등록에 Redirect URI 가 지정된 것으로 가정해서 `def onOAuthAuthorizationCodeRedirected()` 에 `@app.route('/auth')` 라는 태깅이 되어있습니다.
    1. redirect uri 를 호출한 request 로부터 authorization code 와 state 정보를 얻어낸다.
    2. authorization code 로부터 access token 을 얻어내는 네이버 API 를 호출한다.
    3. 얻어낸 access token 을 이용해서 프로필 정보를 반환하는 API 를 호출하고,
    4. 얻어낸 user id 와 name 을 DB 에 저장한다.(redis서버에)
    5. 첫 페이지로 redirect 하는데 로그인 쿠키를 설정하고 보내준다.

## `def getMemos()`
docker의 redis container가 동작하는 원격 서버를 이용하여 쿠키로 받은 userId를 key로 하여 현재 로그인한 유저의 메모들을 읽어옵니다. 

## `def post_new_memo()`

redis가 동작하는 원격서버에 userId를 key로 하여 memoList에 새 메모를 추가합니다. 
