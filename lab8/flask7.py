#!/usr/bin/python3
from flask import Flask
from flask import request

app = Flask(__name__)

@app.route('/', methods=['GET', 'POST'])
def index():
   return{
       'method': request.method,
       'name': request.args.get('name', default='이름이 없는자'),
       'client': request.headers['User-Agent'],
   }

if __name__ == '__main__': ##직접 실행, not 모듈로 실행
    app.run(host='0.0.0.0', port=19128)
