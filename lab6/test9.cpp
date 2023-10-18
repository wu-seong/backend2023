#include <chrono>
#include <iostream>
#include <thread>
#include <mutex>

using namespace std;

int sum = 0;
mutex m1,m2; //mutex는 보통 전역변수로 

void f(){
    for(int i = 0; i< 10 * 1000 * 1000; ++i){ //1000만
        m1.lock();   //공유 변수 sum을 atomic하게 변경하기 위해 m1을 점유
        m2.lock();   //m2를 점유 하려 하는데 main threadd에서 먼저 점유하여 대기
        ++sum;      //읽기 -> 연산 -> 쓰기 작업이 일어남
        m1.unlock();
        m2.unlock();
    }
}


int main(){
    thread t(f);
    for(int i = 0; i< 10 * 1000 * 1000; ++i){ //1000만
        m2.lock(); //m2를 먼저 점유
        m1.lock(); //m1을 점유 하려 하는데 thread t 에서 m1을 먼저 점유하여 대기
        
        ++sum;      //읽기 -> 연산 -> 쓰기 작업이 일어남
        m2.unlock();
        m1.unlock();
    }
    t.join();
    cout << "Sum: " << sum << endl;
    
}