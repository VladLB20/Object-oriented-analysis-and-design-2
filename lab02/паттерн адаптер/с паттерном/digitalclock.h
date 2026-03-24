#ifndef DIGITALCLOCK_H
#define DIGITALCLOCK_H

class DigitalClock 
{
public:
    virtual ~DigitalClock() = default;
    virtual void setTime(int hours, int minutes, int seconds) = 0;
};

#endif 