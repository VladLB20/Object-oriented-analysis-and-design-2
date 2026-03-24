#include "analogclockpanel.h"
#include <cmath>

wxBEGIN_EVENT_TABLE(AnalogClockPanel, wxPanel)
    EVT_PAINT(AnalogClockPanel::OnPaint)
wxEND_EVENT_TABLE()

AnalogClockPanel::AnalogClockPanel(wxWindow* parent, AnalogClock* clock)
    : wxPanel(parent, wxID_ANY, wxDefaultPosition, wxSize(300, 300))
    , m_clock(clock)
{
    SetMinSize(wxSize(200, 200));
}

void AnalogClockPanel::setAnalogClock(AnalogClock* clock) {
    m_clock = clock;
    Refresh();
}

void AnalogClockPanel::refreshClock() {
    Refresh();
}

void AnalogClockPanel::DrawClockNumbers(wxGraphicsContext* gc) {
    double radius = 40;
    for (int i = 1; i <= 12; ++i) {
        double angle = (i * 30.0) * M_PI / 180.0;
        double x = radius * sin(angle);
        double y = -radius * cos(angle);
        wxString num = wxString::Format("%d", i);
        wxFont font(12, wxFONTFAMILY_DEFAULT, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_BOLD);
        gc->SetFont(font, *wxBLACK);
        double textW, textH;
        gc->GetTextExtent(num, &textW, &textH);
        gc->DrawText(num, x - textW/2, y - textH/2);
    }
}

void AnalogClockPanel::OnPaint(wxPaintEvent& WXUNUSED(event)) {
    wxPaintDC dc(this);
    if (!m_clock) return;

    wxGraphicsContext* gc = wxGraphicsContext::Create(dc);
    if (!gc) return;

    int w, h;
    GetSize(&w, &h);
    int side = w < h ? w : h;
    int offsetX = (w - side) / 2;
    int offsetY = (h - side) / 2;

    gc->Translate(offsetX + side/2.0, offsetY + side/2.0);
    double scale = side / 100.0;
    gc->Scale(scale, scale);

    // Циферблат
    gc->SetPen(wxPen(*wxBLACK, 1));
    gc->SetBrush(wxBrush(*wxWHITE));
    gc->DrawEllipse(-48, -48, 96, 96);

    // Подписи цифр (без меток)
    DrawClockNumbers(gc);

    // Часовая стрелка
    gc->PushState();
    gc->Rotate(m_clock->hourAngle() * M_PI / 180.0);
    gc->SetPen(wxPen(*wxBLACK, 2));
    gc->SetBrush(*wxBLACK_BRUSH);
    wxGraphicsPath pathHour = gc->CreatePath();
    pathHour.MoveToPoint(-3, -20);
    pathHour.AddLineToPoint(3, -20);
    pathHour.AddLineToPoint(0, 30);
    pathHour.CloseSubpath();
    gc->FillPath(pathHour);
    gc->StrokePath(pathHour);
    gc->PopState();

    // Минутная стрелка
    gc->PushState();
    gc->Rotate(m_clock->minuteAngle() * M_PI / 180.0);
    gc->SetPen(wxPen(*wxBLUE, 2));
    gc->SetBrush(wxBrush(*wxBLUE));
    wxGraphicsPath pathMin = gc->CreatePath();
    pathMin.MoveToPoint(-2, -30);
    pathMin.AddLineToPoint(2, -30);
    pathMin.AddLineToPoint(0, 40);
    pathMin.CloseSubpath();
    gc->FillPath(pathMin);
    gc->StrokePath(pathMin);
    gc->PopState();

    // Секундная стрелка
    gc->PushState();
    gc->Rotate(m_clock->secondAngle() * M_PI / 180.0);
    gc->SetPen(wxPen(*wxRED, 1));
    gc->SetBrush(wxBrush(*wxRED));
    wxGraphicsPath pathSec = gc->CreatePath();
    pathSec.MoveToPoint(-1, -40);
    pathSec.AddLineToPoint(1, -40);
    pathSec.AddLineToPoint(0, 45);
    pathSec.CloseSubpath();
    gc->FillPath(pathSec);
    gc->StrokePath(pathSec);
    gc->PopState();

    // Центральная точка
    gc->SetBrush(*wxBLACK_BRUSH);
    gc->DrawEllipse(-4, -4, 8, 8);

    delete gc;
}