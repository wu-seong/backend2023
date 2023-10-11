#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/errno.h>
#include <string.h>
#include <iostream>
#include <unistd.h>

using namespace std;

int main(){
    int passivSock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

    struct sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_addr.s_addr = INADDR_ANY;
    sin.sin_port = htons(20128);
    if (bind(passivSock, (struct sockaddr*) &sin, sizeof(sin)) < 0){
        cerr << "bind() failed: " << strerror(errno) << endl;
    }

    if(listen(passivSock, 10) < 0 ){
        cerr << "listen() failed: " << strerror(errno) << endl;
        return 1;
    }

    memset(&sin, 0, sizeof(sin));
    unsigned int sin_len = sizeof(sin);
    int clientSock = accept(passivSock, (struct sockaddr *) &sin, &sin_len);
    if (clientSock < 0){
        cerr << "accept() failed: " << strerror(errno) << endl;
        return 1;
    }
    
    char buf[65536];
    int numRecv = recv(clientSock, buf, sizeof(buf), 0);
    if(numRecv == 0){
        cout << "Socket closed: " << clientSock << endl;
    }
    else if(numRecv < 0){
        cerr <<"recv() failed: " << strerror(errno) << endl; 
    }
    else{
        cout << "Received: " << numRecv << "bytes, clientSock" <<clientSock << endl;
    }

    int offset = 0;
    
    while(offset < numRecv){
       
        int numSend = send(clientSock, buf + offset, numRecv - offset, 0);
        if( numSend < 0){
            cerr << "send() failed: " << strerror(errno) << endl;
        }
        else{
            cout << "Sent: " << numSend << endl;
            offset += numSend;
        }
        cout << offset <<" " << numRecv<< endl;
    }
    close(clientSock);
    close(passivSock);
}