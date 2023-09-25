#include <arpa/inet.h>
#include <sys/socket.h> //알파벳 순서, c다음 c++ 헤더 파일을 넣는 것이 관행
#include <sys/types.h>
#include <unistd.h>
#include <string.h>

#include <iostream>
#include <string>

using namespace std;

int main(){
    int s = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if(s<0) return 1;

    string buf = "Hello World";

    struct sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_port = htons(10000);
    sin.sin_addr.s_addr = inet_addr("127.0.0.1"); //ip주소 문자열을 4바이트 빅 인디안으로 정보로 바꿔줌 호스트 주소는 안됨

    int numBytes = sendto(s, buf.c_str(), buf.length(), 0, (struct sockaddr*) & sin, sizeof(sin)); //보낸 것 까지만 알려줌 받았는지는 모름 확인 해보니 실제로 10000번 포트로 통신하는 프로그램은 없었음

    cout << "Sent:" << numBytes << endl;

    close(s);
    return 0;
}