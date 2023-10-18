#include <iostream>
#include <thread>

using namespace std;

void f1(){
    cout << "f1" << endl;
}

void f2(int arg){
    cout << "f2: " << arg << endl;
}
int main(){
    thread t1;  //대응대는 얘를 만드는데 지정이 안되어 있으니 아무것도 하지 않음
    thread t2(f1);   //f1,과 f2의 실행 순서를 보장하지 않음, 
    thread t3(f2, 10);

    t2.join();
    t3.join();
    
    return 0;
}