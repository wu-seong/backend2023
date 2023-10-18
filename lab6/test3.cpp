#include <iostream>
#include <thread>
#include <chrono>

using namespace std;

void f1(){
    cout << "f1: " << this_thread::get_id() <<endl;
    this_thread::sleep_for(chrono::milliseconds(10 * 1000));
    cout << "f1: woke up" << endl;
}

void f2(int arg){
    //this_thread::sleep_for(chrono::milliseconds(10 * 1000)); //이코드를 넣지 않으면 스레드가 금방 작업을 끝냄
    cout << "f2: " << arg << endl;
}
int main(){
    thread t1;  //대응대는 얘를 만드는데 지정이 안되어 있으니 아무것도 하지 않음
    thread t2(f1);   //f1,과 f2의 실행 순서를 보장하지 않음, 
    thread t3(f2, 10);

    cout <<"C++id: " << t3.get_id() << endl;
    cout << "Native id: " << t3.native_handle() << endl;

    t2.join();
    t3.join();
    
    return 0;
}