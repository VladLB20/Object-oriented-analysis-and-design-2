#ifndef ANALOGCLOCK_H
#define ANALOGCLOCK_H

class AnalogClock 
{
public:
    AnalogClock();

    void setHourAngle(double angle);
    void setMinuteAngle(double angle);
    void setSecondAngle(double angle);

    double hourAngle() const;
    double minuteAngle() const;
    double secondAngle() const;

private:
    double m_hourAngle;
    double m_minuteAngle;
    double m_secondAngle;
};

#endif 