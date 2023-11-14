#!/usr/bin/python3
from flask import Flask

app = Flask(__name__)

# default method는 get, default 응답은 200 OK
@app.route('/')
def hello_world():
    return 'Hello, World!' 

@app.route('/bad') # 데코레이터: 겉을 감싸 자신의 것을 실행한뒤 속을 실행, 본질적인것 이외에 무언가를 붙힘
def hello_world1():
    return 'Bad World!'

@app.route('/good')
def hello_world2():
    return 'Good World!'

if __name__ == '__main__': ##직접 실행, not 모듈로 실행
    app.run(host='0.0.0.0', port=19128)
