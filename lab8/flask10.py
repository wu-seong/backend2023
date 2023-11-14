#!/usr/bin/python3
from flask import Flask
from flask import request
from flask import render_template
from flask import json
from flask import make_response
app = Flask(__name__)

op_spec = ['+', '-', '*']

def calculator(op, op1, op2):
    op1 = int(op1)
    op2 = int(op2)
    if op is '+':
       return op1 + op2
    elif op is '-':
       return op1 - op2
    else:
       return op1 * op2
    
    
@app.route('/<op1>/<op>/<op2>')
def index(op1, op, op2):
  
  if not (op1.isdigit() and op2.isdigit()):
     return make_response('피연산자는 숫자로 입력해주세요.',400)
  elif op not in(op_spec):
    return make_response('올바르지 않은 연산자입니다.',400)
  
  result = calculator(op, op1, op2)
  return render_template('calculator.html', op1 = op1, op2 = op2, op = op, result = result)

@app.route('/', methods =['POST'])
def index2():
  
  op1 = request.get_json().get('arg1',  None)
  op2 = request.get_json().get('arg2', None)
  op = request.get_json().get('op',  None)
  if op1 is None or op2 is None or op is None:
    return make_response('일부 JSON필드가 누락되었습니다.',400)
  elif op not in(op_spec):
    return make_response('올바르지 않은 연산자입니다.',400)
  
  result = calculator(op, op1, op2)
  return render_template('calculator.html', op1 = op1, op2 = op2, op = op, result = result)

if __name__ == '__main__': ##직접 실행, not 모듈로 실행
    app.run(host='0.0.0.0', port=19128)
