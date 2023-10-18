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
    cout << "f2: " << arg << endl;
}
int main(){
    auto start = chrono::system_clock::now();

    thread t1;  //대응대는 얘를 만드는데 지정이 안되어 있으니 아무것도 하지 않음
    thread t2(f1);   //f1,과 f2의 실행 순서를 보장하지 않음, 
    thread t3(f2, 10);

    cout <<"C++id: " << t3.get_id() << endl;
    cout << "Native id: " << t3.native_handle() << endl;

    t2.join();
    t3.join();
    auto end = chrono::system_clock::now();
    auto duration = end - start;
    auto seconds = chrono::duration_cast<chrono::seconds>(duration).count(); //사실은 정확히 10초가 아님,  이를 정확하게 하는 realtimeOS라는 다른 영역이 있음
    cout << "Elapsed: " << seconds << endl;
    

    return 0;
}