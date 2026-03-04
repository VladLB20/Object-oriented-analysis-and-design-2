using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace QueueSimulationApp
{
    public class QueueSimulator
    {
        
        private double lambda;
        private double mu;
        private double theta;
        private double simulationTime;
        private int maxQueueLength;
        
        
        private SystemState state;
        private Statistics stats;
        private Random random;
        private double currentTime;
        private Queue<double> requestQueue;
        private int channelCount;
        private Channel[] channels;
        private CancellationTokenSource cancellationTokenSource;
        private bool isRunning;
        
        
        public event Action<SystemState, double> OnStateChanged;
        public event Action<string> OnEvent;
        public event Action<Statistics> OnSimulationCompleted;
        
        public QueueSimulator(double lambda, double mu, double theta, int channelCount, int maxQueueLength, double simulationTime)
        {
            this.lambda = lambda;
            this.mu = mu;
            this.theta = theta;
            this.channelCount = channelCount;
            this.maxQueueLength = maxQueueLength;
            this.simulationTime = simulationTime;

            state = new SystemState(channelCount, maxQueueLength);
            stats = new Statistics();
            random = new Random();
            requestQueue = new Queue<double>();
            cancellationTokenSource = new CancellationTokenSource();

            
            channels = new Channel[channelCount];
            for (int i = 0; i < channelCount; i++)
            {
                channels[i] = new Channel();  
            }
        }
        
        private double GetExponentialRandom(double rate)
        {
            return -Math.Log(1.0 - random.NextDouble()) / rate;
        }
        
        public async Task RunAsync()
        {
            isRunning = true;
            currentTime = 0;
            
            double nextArrivalTime = GetExponentialRandom(lambda);
            
            OnEvent?.Invoke("Моделирование начато");
            OnStateChanged?.Invoke(state, currentTime);
            
            while (currentTime < simulationTime && !cancellationTokenSource.Token.IsCancellationRequested)
            {
                double nextEventTime = nextArrivalTime;
                int nextEventChannel = -1; 

                for (int i = 0; i < channelCount; i++)
                {
                    if (channels[i].FreeTime < nextEventTime)
                    {
                        nextEventTime = channels[i].FreeTime;
                        nextEventChannel = i;
                    }
                }

                if (nextEventTime > simulationTime) break;

                currentTime = nextEventTime;

                if (nextEventChannel == -1)
                {
                    ProcessArrival();
                    nextArrivalTime = currentTime + GetExponentialRandom(lambda);
                }
                else
                {
                    ProcessChannelCompletion(nextEventChannel);
                }

                ProcessImpatience();
                UpdateSystemStateFromChannels(); 
                OnStateChanged?.Invoke(state, currentTime);
                await Task.Delay(10);
            }
            
            isRunning = false;
            OnEvent?.Invoke("Моделирование завершено");
            OnSimulationCompleted?.Invoke(stats);
        }
        
        private void ProcessArrival()
        {
            stats.TotalRequests++;
            OnEvent?.Invoke($"Поступила новая заявка (всего: {stats.TotalRequests})");

            var freeIndices = new List<int>();
            for (int i = 0; i < channelCount; i++)
                if (!channels[i].IsBusy) freeIndices.Add(i);

            if (freeIndices.Count > 0)
            {
                int idx = freeIndices[random.Next(freeIndices.Count)];
                StartServiceInChannel(idx);
            }
            else
            {
                if (state.QueueLength < state.MaxQueueLength)
                {
                    requestQueue.Enqueue(currentTime);
                    state.QueueLength++;
                    OnEvent?.Invoke($"Заявка поставлена в очередь (длина: {state.QueueLength})");
                }
                else
                {
                    stats.LostRequestsQueueFull++;
                    OnEvent?.Invoke("Очередь полна! Заявка потеряна");
                }
            }
        }
                
        private void StartServiceInChannel(int idx)
        {
            channels[idx].IsBusy = true;
            double serviceTime = GetExponentialRandom(mu);
            channels[idx].FreeTime = currentTime + serviceTime;
            stats.TotalServiceTime += serviceTime;
            OnEvent?.Invoke($"Заявка начала обслуживаться в Канале {idx + 1}");
        }

        private void ProcessChannelCompletion(int idx)
        {
            stats.ServedRequests++;
            channels[idx].IsBusy = false;
            channels[idx].FreeTime = double.MaxValue;
            OnEvent?.Invoke($"Канал {idx + 1} освободился (обслужено: {stats.ServedRequests})");

            if (state.QueueLength > 0)
            {
                double arrivalTime = requestQueue.Dequeue();
                double waitTime = currentTime - arrivalTime;
                stats.TotalQueueTime += waitTime;
                state.QueueLength--;

                StartServiceInChannel(idx);
            }
        }

        private void UpdateSystemStateFromChannels()
        {
            for (int i = 0; i < channelCount; i++)
            {
                state.Channels[i] = channels[i].IsBusy;
            }
        }
        
        private void ProcessImpatience()
        {
            
            if (state.QueueLength > 0 && random.NextDouble() < theta * 0.01)
            {
                if (requestQueue.Count > 0)
                {
                    requestQueue.Dequeue();
                    state.QueueLength--;
                    stats.LostRequestsImpatience++;
                    OnEvent?.Invoke("Заявка ушла из очереди (нетерпение)");
                }
            }
        }
        
        public void Stop()
        {
            if (isRunning)
            {
                cancellationTokenSource.Cancel();
                OnEvent?.Invoke("Моделирование остановлено пользователем");
            }
        }

    }

}

