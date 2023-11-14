#!/usr/bin/python3
from flask import Flask
from flask import request
from flask import render_template

app = Flask(__name__)

@app.route('/')
def index():
  name = request.args.get('name', default = None)
  return render_template('hello.html', name= name)

if __name__ == '__main__': ##직접 실행, not 모듈로 실행
    app.run(host='0.0.0.0', port=19128)
