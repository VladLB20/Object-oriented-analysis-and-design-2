namespace QueueSimulationApp
{
    public class SystemState
    {
        public bool[] Channels { get; set; }
        public int QueueLength { get; set; }
        public int MaxQueueLength { get; set; }
        public int ChannelCount { get; }

        public SystemState(int channelCount, int maxQueueLength)
        {
            ChannelCount = channelCount;
            Channels = new bool[channelCount];
            QueueLength = 0;
            MaxQueueLength = maxQueueLength;
        }

        public SystemState Clone()
        {
            var clone = new SystemState(this.ChannelCount, this.MaxQueueLength)
            {
                Channels = (bool[])this.Channels.Clone(),
                QueueLength = this.QueueLength
            };
            return clone;
        }
    }
}