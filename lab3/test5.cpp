
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

    int numBytes;

    struct sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_port = htons(20128);
    sin.sin_addr.s_addr = inet_addr("127.0.0.1"); //ip주소 문자열을 4바이트 빅 인디안으로 정보로 바꿔줌 호스트 주소는 안됨
    socklen_t sin_size = sizeof(sin);

    char buf2[65536];    
    string buf;

    while (cin >> buf) {
        numBytes = sendto(s, buf.c_str(), buf.length(), 0, (struct sockaddr*) &sin, sin_size); 
        cout << "Sent:" << numBytes << endl;
        numBytes = recvfrom(s, buf2, sizeof(buf2), 0, (struct sockaddr*)&sin, &sin_size); //커널이 누구로 부터 데이터를 받을지를 써줌
        cout << "Recevied: " << numBytes << endl;
        cout << "From " << inet_ntoa(sin.sin_addr) << endl;
        cout << buf2 << endl;
    }

    close(s);
    return 0;
}