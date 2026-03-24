#include "analogclock.h"

AnalogClock::AnalogClock()
    : m_hourAngle(0.0), m_minuteAngle(0.0), m_secondAngle(0.0)
{}

void AnalogClock::setHourAngle(double angle) { m_hourAngle = angle; }
void AnalogClock::setMinuteAngle(double angle) { m_minuteAngle = angle; }
void AnalogClock::setSecondAngle(double angle) { m_secondAngle = angle; }
double AnalogClock::hourAngle() const { return m_hourAngle; }
double AnalogClock::minuteAngle() const { return m_minuteAngle; }
double AnalogClock::secondAngle() const { return m_secondAngle; }