#include "app.h"
#include "mainframe.h"

wxIMPLEMENT_APP(App);

bool App::OnInit() 
{
    MainFrame* frame = new MainFrame("Clock");
    frame->Show(true);
    return true;
}