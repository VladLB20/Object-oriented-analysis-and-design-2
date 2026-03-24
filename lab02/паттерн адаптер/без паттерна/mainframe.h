#ifndef MAINFRAME_H
#define MAINFRAME_H

#include <wx/wx.h>
#include <wx/spinctrl.h>
#include <wx/timer.h>
#include "analogclock.h"
#include "analogclockpanel.h"

class MainFrame : public wxFrame {
public:
    MainFrame(const wxString& title);
    ~MainFrame();

private:
    void OnSetTime(wxCommandEvent& event);
    void OnTimer(wxTimerEvent& event);
    void OnSpinFocus(wxFocusEvent& event);
    void OnSpinKillFocus(wxFocusEvent& event);
    void UpdateTime();            
    void UpdateDisplayAndClock(); 
    void AdvanceOneSecond();

    AnalogClock* m_analogClock;
    AnalogClockPanel* m_clockPanel;

    wxSpinCtrl* m_hoursSpin;
    wxSpinCtrl* m_minutesSpin;
    wxSpinCtrl* m_secondsSpin;
    wxStaticText* m_digitalDisplay;
    wxTimer m_timer;

    int m_currentHour;
    int m_currentMinute;
    int m_currentSecond;
    bool m_timerEnabled;

    wxDECLARE_EVENT_TABLE();
};

enum {
    ID_SET_TIME = wxID_HIGHEST + 1
};

#endif 