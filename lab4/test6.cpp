#include <arpa/inet.h>
#include <sys/socket.h> //알파벳 순서, c다음 c++ 헤더 파일을 넣는 것이 관행
#include <sys/types.h>
#include <unistd.h>
#include <string.h>
#include <iostream>
#include <string>

#include "person.pb.h"

using namespace std;
using namespace mju;

int main(){
    int s = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if(s<0) return 1;



    struct sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_port = htons(10001);
    sin.sin_addr.s_addr = inet_addr("127.0.0.1"); //ip주소 문자열을 4바이트 빅 인디안으로 정보로 바꿔줌 호스트 주소는 안됨

    Person *p = new Person;
    p->set_name("DK Moon");
    p->set_id(12345678);

    Person::PhoneNumber* phone = p->add_phones();
    phone->set_number("010-111-1234");
    phone->set_type(Person::MOBILE);

    phone = p->add_phones();
    phone->set_number("02-100-1000");
    phone->set_type(Person::HOME);

    const string serializedString = p->SerializeAsString();

    int numBytes = sendto(s, serializedString.c_str(), serializedString.length(), 0, (struct sockaddr*) & sin, sizeof(sin)); 

    cout << "Sent:" << numBytes << endl;

    Person *p2 = new Person;
    
    char buf2[65536];
    memset(&sin, 0, sizeof(sin));
    socklen_t sin_size = sizeof(sin);
    numBytes = recvfrom(s, buf2, sizeof(buf2), 0, (struct sockaddr*)&sin, &sin_size); //커널이 누구로 부터 데이터를 받을지를 써줌
    cout << "Recevied: " << numBytes << endl;
    cout << "From " << inet_ntoa(sin.sin_addr) << endl;
    string rcv(buf2, numBytes);
    p2-> ParseFromString(rcv);

    cout << "Name:" << p2-> name() <<endl;
    cout << "ID:" <<p2->id() << endl;

    for(int i = 0; i< p2->phones_size(); ++i){
        cout << "Type:" << p2->phones(i).type() << endl;
        cout << "Phone:"<< p2->phones(i).number() << endl;
    }
    close(s);
    return 0;
}