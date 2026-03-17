#ifndef MAINFRAME_H
#define MAINFRAME_H

#include <wx/wx.h>
#include <wx/spinctrl.h>
#include <memory>
#include "analogclock.h"
#include "digitalclock.h"
#include "clockadapter.h"
#include "analogclockpanel.h"

class MainFrame : public wxFrame {
public:
    MainFrame(const wxString& title);

private:
    void OnSetTime(wxCommandEvent& event);
    void UpdateTime();  

    AnalogClock* m_analogClock;
    std::unique_ptr<DigitalClock> m_digitalClock;
    AnalogClockPanel* m_clockPanel;
    wxSpinCtrl* m_hoursSpin;
    wxSpinCtrl* m_minutesSpin;
    wxSpinCtrl* m_secondsSpin;
    wxStaticText* m_digitalDisplay;

    wxDECLARE_EVENT_TABLE();
};

enum {
    ID_SET_TIME = wxID_HIGHEST + 1
};

#endif 