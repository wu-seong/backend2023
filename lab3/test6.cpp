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

    struct sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_port = htons(20128);
    sin.sin_addr.s_addr = inet_addr("127.0.0.1"); //ip주소 문자열을 4바이트 빅 인디안으로 정보로 바꿔줌 호스트 주소는 안됨

    int numBytes = sendto(s, buf.c_str(), buf.length(), 0, (struct sockaddr*) & sin, sizeof(sin)); 

    cout << "Sent:" << numBytes << endl;

    char buf2[65536];
    memset(&sin, 0, sizeof(sin));
    socklen_t sin_size = sizeof(sin);
    numBytes = recvfrom(s, buf2, sizeof(buf2), 0, (struct sockaddr*)&sin, &sin_size); //커널이 누구로 부터 데이터를 받을지를 써줌

    cout << "Recevied: " << numBytes << endl;
    cout << "From " << inet_ntoa(sin.sin_addr) << endl;

    memset(&sin, 0, sizeof(sin));
    sin_size = sizeof(sin);
    int result = getsockname(s, (struct sockaddr *) &sin, &sin_size); // 주소를 읽어서 가져옴
    if(result == 0)
        cout << "My addr: " << inet_ntoa(sin.sin_addr) << endl; //0.0.0.0 자기 네트워크 인터페이스 중 아무거나, 나갈 때 결정됨
        cout << "My port: " << ntohs(sin.sin_port) <<endl;

    close(s);
    return 0;
}