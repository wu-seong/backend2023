#include <chrono>
#include <iostream>
#include <thread>
#include <mutex>

using namespace std;

int sum = 0;
mutex m; //mutex는 보통 전역변수로 

void f(){
    for(int i = 0; i< 10 * 1000 * 1000; ++i){ //1000만
        m.lock();   //동기: 시간을 맞추는 것
        ++sum;      //읽기 -> 연산 -> 쓰기 작업이 일어남
        m.unlock();
    }
}

int main(){
    thread t(f);
    for(int i = 0; i< 10 * 1000 * 1000; ++i){ //1000만
       unique_lock<mutex> ul(m); //변수를 꼭 써줘야함 안쓰면 생성자만 호출하고 바로 변수 없어짐
       ++sum;
    }
    t.join();
    cout << "Sum: " << sum << endl;
    
}