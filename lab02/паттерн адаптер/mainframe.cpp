#include "mainframe.h"
#include <wx/sizer.h>
#include <wx/stattext.h>
#include <wx/button.h>
#include <wx/panel.h>

wxBEGIN_EVENT_TABLE(MainFrame, wxFrame)
    EVT_BUTTON(ID_SET_TIME, MainFrame::OnSetTime)
wxEND_EVENT_TABLE()

MainFrame::MainFrame(const wxString& title)
    : wxFrame(nullptr, wxID_ANY, title, wxDefaultPosition, wxSize(500, 600))
    , m_analogClock(new AnalogClock())
    , m_digitalClock(new ClockAdapter(*m_analogClock))  
{
    wxPanel* mainPanel = new wxPanel(this, wxID_ANY);

    m_clockPanel = new AnalogClockPanel(mainPanel, m_analogClock);

    wxStaticText* hoursLabel = new wxStaticText(mainPanel, wxID_ANY, "Часы:");
    m_hoursSpin = new wxSpinCtrl(mainPanel, wxID_ANY, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxSP_ARROW_KEYS, 0, 23, 12);
    wxStaticText* minutesLabel = new wxStaticText(mainPanel, wxID_ANY, "Минуты:");
    m_minutesSpin = new wxSpinCtrl(mainPanel, wxID_ANY, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxSP_ARROW_KEYS, 0, 59, 0);
    wxStaticText* secondsLabel = new wxStaticText(mainPanel, wxID_ANY, "Секунды:");
    m_secondsSpin = new wxSpinCtrl(mainPanel, wxID_ANY, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxSP_ARROW_KEYS, 0, 59, 0);

    wxButton* setButton = new wxButton(mainPanel, ID_SET_TIME, "Установить время");

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

    
    UpdateTime();
}

void MainFrame::OnSetTime(wxCommandEvent& WXUNUSED(event)) {
    UpdateTime();
}

void MainFrame::UpdateTime() {
    int h = m_hoursSpin->GetValue();
    int m = m_minutesSpin->GetValue();
    int s = m_secondsSpin->GetValue();

    m_digitalClock->setTime(h, m, s);
    m_clockPanel->refreshClock();

    wxString timeStr = wxString::Format("%02d:%02d:%02d", h, m, s);
    m_digitalDisplay->SetLabel(timeStr);
}