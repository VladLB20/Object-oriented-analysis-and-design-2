namespace QueueSimulationApp
{
    public class Statistics
    {
        public int TotalRequests { get; set; }          
        public int ServedRequests { get; set; }         
        public int LostRequestsQueueFull { get; set; }  
        public int LostRequestsImpatience { get; set; } 
        public double TotalQueueTime { get; set; }      
        public double TotalServiceTime { get; set; }    
        
        public Statistics()
        {
            TotalRequests = 0;
            ServedRequests = 0;
            LostRequestsQueueFull = 0;
            LostRequestsImpatience = 0;
            TotalQueueTime = 0;
            TotalServiceTime = 0;
        }
    }
}