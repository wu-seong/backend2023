import socket
import json
import sys
def main(argv):

    # json으로 직렬화될 딕셔너리 생성
    obj1 = { 
        'name': 'DK Moon',
        'id': 12345678,
        'work':{
            'name': 'Myongji University',
            'address': '116 Myongji-ro'
        },
    }
    s = json.dumps(obj1)
     
    # 소켓 생성및 UDP 서버와 통신
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, 0)
    sock.sendto(bytes(s, encoding='utf-8'), ('127.0.0.1', 10001))
    (data, sender) = sock.recvfrom(65536)

    # 받아온 데이터를 다시 역직렬화
    s = json.loads(data)
    print(s)


if __name__ == '__main__':
    main(sys.argv)