#ifndef ANALOGCLOCKPANEL_H
#define ANALOGCLOCKPANEL_H

#include <wx/wx.h>
#include <wx/graphics.h>
#include "analogclock.h"

class AnalogClockPanel : public wxPanel {
public:
    AnalogClockPanel(wxWindow* parent, AnalogClock* clock);
    void setAnalogClock(AnalogClock* clock);
    void refreshClock();

private:
    void OnPaint(wxPaintEvent& event);
    void DrawClockNumbers(wxGraphicsContext* gc);
    AnalogClock* m_clock;

    wxDECLARE_EVENT_TABLE();
};

#endif 