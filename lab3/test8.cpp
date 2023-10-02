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

    string buf = "Hello World2";

    struct sockaddr_in sin; //서버 소켓 주소 지정
    memset(&sin, 0, sizeof(sin)); 
    sin.sin_family = AF_INET;
    sin.sin_port = htons(20128);
    sin.sin_addr.s_addr = INADDR_ANY;

    socklen_t sin_size = sizeof(sin);
    char buf2[1501];

    if ( bind(s, (struct sockaddr*) &sin, sizeof(sin) ) == -1 ){ //소켓 바인딩
        cerr << strerror(errno) << endl;
        return 0;
    }
    ssize_t content_len;
    while ( true ){
        content_len = recvfrom(s, buf2, sizeof(buf2), 0, (struct sockaddr *)&sin, &sin_size); //소켓을 통해 데이터를 받음과 동시에 버퍼에 데이터를 저장하고 클라이언트 소켓의 주소 정보를 저장
        cout << buf2 << endl;
        sendto(s, buf2, content_len, 0, (struct sockaddr* )&sin, sin_size);
    }

    close(s);
    return 0;
}