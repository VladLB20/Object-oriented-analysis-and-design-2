#include "clockadapter.h"
#include <cmath>

ClockAdapter::ClockAdapter(AnalogClock& clock) : m_clock(clock) {}

void ClockAdapter::setTime(int hours, int minutes, int seconds) 
{
    
    double secondAngle = 6.0 * seconds;

    
    double minuteAngle = 6.0 * minutes + 0.1 * seconds;

    
    double hourAngle = 30.0 * (hours % 12) + 0.5 * minutes + (30.0 / 3600.0) * seconds;

    
    auto normalize = [](double a) {
        a = fmod(a, 360.0);
        if (a < 0) a += 360.0;
        return a;
    };

    m_clock.setSecondAngle(normalize(secondAngle));
    m_clock.setMinuteAngle(normalize(minuteAngle));
    m_clock.setHourAngle(normalize(hourAngle));
}