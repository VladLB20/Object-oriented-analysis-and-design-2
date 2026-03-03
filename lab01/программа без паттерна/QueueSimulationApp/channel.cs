using System;

public class Channel
{
    public bool IsBusy { get; set; }
    public double FreeTime { get; set; }

    public Channel()
    {
        IsBusy = false;
        FreeTime = double.MaxValue;
    }
}