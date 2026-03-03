using System;

namespace QueueSimulationApp
{
    public class Channel : ICloneable
    {
        public bool IsBusy { get; set; }
        public double FreeTime { get; set; }

        public Channel()
        {
            IsBusy = false;
            FreeTime = double.MaxValue;
        }

        
        public Channel(Channel parameters)
        {
            IsBusy = parameters.IsBusy;
            FreeTime = parameters.FreeTime;
        }

        public object Clone()
        {
            return new Channel(this);
        }
    }
}