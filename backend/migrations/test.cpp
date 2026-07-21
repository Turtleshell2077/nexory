#include <iostream>
#include <string>
using namespace std;

class Car{
    private:
    string brand;
    int speed;
    public:
    void accelerate(int delta){
        speed += delta;
    }
    void setBrand(string name){
        brand = name;
    }
    void setSpeed(int s){
        speed = s;
    }
    int getSpeed(){
        return speed;
    }
};

int main(){
    Car object1;
    Car object2;
    object1.setBrand("Volvo");
    object1.setSpeed(12);
    object1.accelerate(20);
    cout<<object1.getSpeed();
    return 0;
}