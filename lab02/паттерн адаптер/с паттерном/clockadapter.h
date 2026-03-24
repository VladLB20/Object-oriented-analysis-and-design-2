#ifndef CLOCKADAPTER_H
#define CLOCKADAPTER_H

#include "digitalclock.h"
#include "analogclock.h"

class ClockAdapter : public DigitalClock 
{
public:
    explicit ClockAdapter(AnalogClock& clock);
    void setTime(int hours, int minutes, int seconds) override;

private:
    AnalogClock& m_clock;
};

#endif 