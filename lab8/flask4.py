#!/usr/bin/python3
from flask import Flask
from flask import make_response

app = Flask(__name__)

@app.route('/<greeting>/<name>')
def greet(greeting, name):
    resp = make_response(f'{greeting}, {name}!', 404)
    return resp

if __name__ == '__main__': ##직접 실행, not 모듈로 실행
    app.run(host='0.0.0.0', port=19128)
