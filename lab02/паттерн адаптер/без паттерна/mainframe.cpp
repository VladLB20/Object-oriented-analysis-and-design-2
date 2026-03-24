#include "mainframe.h"
#include <wx/sizer.h>
#include <wx/stattext.h>
#include <wx/button.h>
#include <wx/panel.h>
#include <cmath>

wxBEGIN_EVENT_TABLE(MainFrame, wxFrame)
    EVT_BUTTON(ID_SET_TIME, MainFrame::OnSetTime)
    EVT_TIMER(wxID_ANY, MainFrame::OnTimer)
wxEND_EVENT_TABLE()

MainFrame::MainFrame(const wxString& title)
    : wxFrame(nullptr, wxID_ANY, title, wxDefaultPosition, wxSize(500, 600))
    , m_analogClock(new AnalogClock())
    , m_currentHour(12), m_currentMinute(0), m_currentSecond(0)
    , m_timerEnabled(true)
{
    wxPanel* mainPanel = new wxPanel(this, wxID_ANY);
    m_clockPanel = new AnalogClockPanel(mainPanel, m_analogClock);

    wxStaticText* hoursLabel = new wxStaticText(mainPanel, wxID_ANY, "Hours:");
    m_hoursSpin = new wxSpinCtrl(mainPanel, wxID_ANY, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxSP_ARROW_KEYS, 0, 23, m_currentHour);
    wxStaticText* minutesLabel = new wxStaticText(mainPanel, wxID_ANY, "Minutes:");
    m_minutesSpin = new wxSpinCtrl(mainPanel, wxID_ANY, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxSP_ARROW_KEYS, 0, 59, m_currentMinute);
    wxStaticText* secondsLabel = new wxStaticText(mainPanel, wxID_ANY, "Seconds:");
    m_secondsSpin = new wxSpinCtrl(mainPanel, wxID_ANY, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxSP_ARROW_KEYS, 0, 59, m_currentSecond);

    m_hoursSpin->Bind(wxEVT_SET_FOCUS, &MainFrame::OnSpinFocus, this);
    m_hoursSpin->Bind(wxEVT_KILL_FOCUS, &MainFrame::OnSpinKillFocus, this);
    m_minutesSpin->Bind(wxEVT_SET_FOCUS, &MainFrame::OnSpinFocus, this);
    m_minutesSpin->Bind(wxEVT_KILL_FOCUS, &MainFrame::OnSpinKillFocus, this);
    m_secondsSpin->Bind(wxEVT_SET_FOCUS, &MainFrame::OnSpinFocus, this);
    m_secondsSpin->Bind(wxEVT_KILL_FOCUS, &MainFrame::OnSpinKillFocus, this);

    wxButton* setButton = new wxButton(mainPanel, ID_SET_TIME, "Set Time");
    m_digitalDisplay = new wxStaticText(mainPanel, wxID_ANY, "12:00:00", wxDefaultPosition, wxDefaultSize, wxALIGN_CENTER_HORIZONTAL);
    wxFont font(24, wxFONTFAMILY_MODERN, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_NORMAL);
    m_digitalDisplay->SetFont(font);

    wxBoxSizer* mainSizer = new wxBoxSizer(wxVERTICAL);
    mainSizer->Add(m_clockPanel, 1, wxEXPAND | wxALL, 10);

    wxBoxSizer* controlSizer = new wxBoxSizer(wxVERTICAL);
    controlSizer->Add(m_digitalDisplay, 0, wxALIGN_CENTER | wxTOP | wxBOTTOM, 10);

    wxFlexGridSizer* gridSizer = new wxFlexGridSizer(3, 2, 5, 5);
    gridSizer->Add(hoursLabel, 0, wxALIGN_RIGHT | wxALIGN_CENTER_VERTICAL);
    gridSizer->Add(m_hoursSpin, 0, wxEXPAND);
    gridSizer->Add(minutesLabel, 0, wxALIGN_RIGHT | wxALIGN_CENTER_VERTICAL);
    gridSizer->Add(m_minutesSpin, 0, wxEXPAND);
    gridSizer->Add(secondsLabel, 0, wxALIGN_RIGHT | wxALIGN_CENTER_VERTICAL);
    gridSizer->Add(m_secondsSpin, 0, wxEXPAND);
    controlSizer->Add(gridSizer, 0, wxALIGN_CENTER | wxALL, 5);
    controlSizer->Add(setButton, 0, wxALIGN_CENTER | wxALL, 10);

    mainSizer->Add(controlSizer, 0, wxEXPAND | wxLEFT | wxRIGHT | wxBOTTOM, 10);
    mainPanel->SetSizer(mainSizer);

    UpdateDisplayAndClock();

    m_timer.SetOwner(this);
    Bind(wxEVT_TIMER, &MainFrame::OnTimer, this);
    m_timer.Start(1000);
}

MainFrame::~MainFrame() {
    delete m_analogClock;
}

void MainFrame::OnSpinFocus(wxFocusEvent& event) {
    m_timer.Stop();
    m_timerEnabled = false;
    event.Skip();
}

void MainFrame::OnSpinKillFocus(wxFocusEvent& event) {
    m_timer.Start(1000);
    m_timerEnabled = true;
    event.Skip();
}

void MainFrame::OnSetTime(wxCommandEvent& WXUNUSED(event)) {
    UpdateTime();
}

void MainFrame::OnTimer(wxTimerEvent& WXUNUSED(event)) {
    if (!m_timerEnabled) return;
    AdvanceOneSecond();
    UpdateDisplayAndClock();
}

void MainFrame::UpdateTime() {
    m_currentHour = m_hoursSpin->GetValue();
    m_currentMinute = m_minutesSpin->GetValue();
    m_currentSecond = m_secondsSpin->GetValue();

    // Прямое вычисление углов (без адаптера)
    double secondAngle = 6.0 * m_currentSecond;
    double minuteAngle = 6.0 * m_currentMinute + 0.1 * m_currentSecond;
    double hourAngle = 30.0 * (m_currentHour % 12) + 0.5 * m_currentMinute + (30.0 / 3600.0) * m_currentSecond;

    auto normalize = [](double a) {
        a = fmod(a, 360.0);
        if (a < 0) a += 360.0;
        return a;
    };

    m_analogClock->setSecondAngle(normalize(secondAngle));
    m_analogClock->setMinuteAngle(normalize(minuteAngle));
    m_analogClock->setHourAngle(normalize(hourAngle));

    UpdateDisplayAndClock();
}

void MainFrame::UpdateDisplayAndClock() {
    if (m_timerEnabled) {
        m_hoursSpin->SetValue(m_currentHour);
        m_minutesSpin->SetValue(m_currentMinute);
        m_secondsSpin->SetValue(m_currentSecond);
    }
    m_clockPanel->refreshClock();

    wxString timeStr = wxString::Format("%02d:%02d:%02d", m_currentHour, m_currentMinute, m_currentSecond);
    m_digitalDisplay->SetLabel(timeStr);
}

void MainFrame::AdvanceOneSecond() {
    m_currentSecond++;
    if (m_currentSecond == 60) {
        m_currentSecond = 0;
        m_currentMinute++;
        if (m_currentMinute == 60) {
            m_currentMinute = 0;
            m_currentHour++;
            if (m_currentHour == 24) m_currentHour = 0;
        }
    }
}