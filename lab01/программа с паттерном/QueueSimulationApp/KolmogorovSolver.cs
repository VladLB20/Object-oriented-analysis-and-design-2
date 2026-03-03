using System;
using System.Collections.Generic;

namespace QueueSimulationApp
{
    public class KolmogorovSolver
    {
        private double lambda;
        private double mu;
        private double theta;
        private int L; 
        private int stateCount;
        
    
        private double[] probabilities;
        private double[] derivatives;
        
        public KolmogorovSolver(double lambda, double mu, double theta, int maxQueueLength)
        {
            this.lambda = lambda;
            this.mu = mu;
            this.theta = theta;
            this.L = maxQueueLength;
            this.stateCount = L + 4;
            probabilities = new double[stateCount];
            derivatives = new double[stateCount];
        }
        

        public void SetInitialConditions()
        {
            for (int i = 0; i < stateCount; i++)
                probabilities[i] = 0;
            probabilities[0] = 1.0; 
        }
        
    
        private void CalculateDerivatives()
        {
            // Уравнение (1): dp00/dt = μ p10 + μ p01 - λ p00
            derivatives[0] = mu * probabilities[1] + mu * probabilities[2] - lambda * probabilities[0];
            
            // Уравнение (2): dp10/dt = (λ/2) p00 + μ p110 - (λ + μ) p10
            derivatives[1] = (lambda / 2) * probabilities[0] + mu * probabilities[3] - (lambda + mu) * probabilities[1];
            
            // Уравнение (3): dp01/dt = (λ/2) p00 + μ p110 - (λ + μ) p01
            derivatives[2] = (lambda / 2) * probabilities[0] + mu * probabilities[3] - (lambda + mu) * probabilities[2];
            
            // Уравнение (4): dp110/dt = λ p10 + λ p01 + (2μ + θ) p111 - (λ + 2μ) p110
            derivatives[3] = lambda * probabilities[1] + lambda * probabilities[2] + 
                           (2 * mu + theta) * probabilities[4] - (lambda + 2 * mu) * probabilities[3];
            
            // Уравнения (5): для q=1..L-1
            for (int q = 1; q <= L - 1; q++)
            {
                int index = 3 + q;
                derivatives[index] = lambda * probabilities[index - 1] + 
                                   (2 * mu + (q + 1) * theta) * probabilities[index + 1] - 
                                   (lambda + 2 * mu + q * theta) * probabilities[index];
            }
            
            // Уравнение (6): dp11L/dt = λ p11,L-1 - (2μ + Lθ) p11L
            int lastIndex = 3 + L;
            derivatives[lastIndex] = lambda * probabilities[lastIndex - 1] - 
                                   (2 * mu + L * theta) * probabilities[lastIndex];
        }
        
        
        public void RungeKuttaStep(double dt)
        {
            double[] k1 = new double[stateCount];
            double[] k2 = new double[stateCount];
            double[] k3 = new double[stateCount];
            double[] k4 = new double[stateCount];
            double[] temp = new double[stateCount];
            
            
            CalculateDerivatives();
            Array.Copy(derivatives, k1, stateCount);
            
            
            for (int i = 0; i < stateCount; i++)
                temp[i] = probabilities[i] + dt * k1[i] / 2;
            SwapArrays(temp, probabilities);
            CalculateDerivatives();
            Array.Copy(derivatives, k2, stateCount);
            SwapArrays(probabilities, temp);
            
            
            for (int i = 0; i < stateCount; i++)
                temp[i] = probabilities[i] + dt * k2[i] / 2;
            SwapArrays(temp, probabilities);
            CalculateDerivatives();
            Array.Copy(derivatives, k3, stateCount);
            SwapArrays(probabilities, temp);
            
            
            for (int i = 0; i < stateCount; i++)
                temp[i] = probabilities[i] + dt * k3[i];
            SwapArrays(temp, probabilities);
            CalculateDerivatives();
            Array.Copy(derivatives, k4, stateCount);
            SwapArrays(probabilities, temp);
            
            
            for (int i = 0; i < stateCount; i++)
            {
                probabilities[i] += dt * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]) / 6;
            }
            
            
            NormalizeProbabilities();
        }
        
        private void SwapArrays(double[] a, double[] b)
        {
            for (int i = 0; i < a.Length; i++)
            {
                double temp = a[i];
                a[i] = b[i];
                b[i] = temp;
            }
        }
        
        private void NormalizeProbabilities()
        {
            double sum = 0;
            for (int i = 0; i < stateCount; i++)
                sum += probabilities[i];
            
            if (Math.Abs(sum - 1.0) > 1e-10)
            {
                for (int i = 0; i < stateCount; i++)
                    probabilities[i] /= sum;
            }
        }
        
        
        public Dictionary<string, double> Solve(double T, double dt)
        {
            SetInitialConditions();
            double t = 0;
            while (t < T)
            {
                RungeKuttaStep(dt);
                t += dt;
            }
            
            return GetResults();
        }
        
       
        public Dictionary<string, double> SolveSteadyState()
        {
            int n = stateCount;
            double[,] A = new double[n, n];
            double[] B = new double[n];
            
            
            // Уравнение (8): μ p10 + μ p01 - λ p00 = 0
            A[0, 0] = -lambda;
            A[0, 1] = mu;
            A[0, 2] = mu;
            B[0] = 0;
            
            // Уравнение (9): (λ/2) p00 + μ p110 - (λ + μ) p10 = 0
            A[1, 0] = lambda / 2;
            A[1, 1] = -(lambda + mu);
            A[1, 3] = mu;
            B[1] = 0;
            
            // Уравнение (10): (λ/2) p00 + μ p110 - (λ + μ) p01 = 0
            A[2, 0] = lambda / 2;
            A[2, 2] = -(lambda + mu);
            A[2, 3] = mu;
            B[2] = 0;
            
            // Уравнение (11): λ p10 + λ p01 + (2μ + θ) p111 - (λ + 2μ) p110 = 0
            A[3, 1] = lambda;
            A[3, 2] = lambda;
            A[3, 3] = -(lambda + 2 * mu);
            A[3, 4] = 2 * mu + theta;
            B[3] = 0;
            
            // Уравнения (12) для q=1..L-1
            for (int q = 1; q <= L - 1; q++)
            {
                int row = 3 + q;
                A[row, 3 + q - 1] = lambda;
                A[row, 3 + q] = -(lambda + 2 * mu + q * theta);
                A[row, 3 + q + 1] = 2 * mu + (q + 1) * theta;
                B[row] = 0;
            }
            
            // Уравнение (14): λ p11,L-1 - (2μ + Lθ) p11L = 0
            int lastRow = 3 + L;
            A[lastRow, lastRow - 1] = lambda;
            A[lastRow, lastRow] = -(2 * mu + L * theta);
            B[lastRow] = 0;
            
            
            for (int i = 0; i < n; i++)
            {
                A[n-1, i] = 1;
            }
            B[n-1] = 1;
            
           
            probabilities = SolveLinearSystem(A, B);
            NormalizeProbabilities();
            
            return GetResults();
        }
        
        private double[] SolveLinearSystem(double[,] A, double[] B)
        {
            int n = B.Length;
            double[] x = new double[n];
            
            // Прямой ход метода Гаусса
            for (int i = 0; i < n; i++)
            {
                
                int maxRow = i;
                for (int k = i + 1; k < n; k++)
                {
                    if (Math.Abs(A[k, i]) > Math.Abs(A[maxRow, i]))
                        maxRow = k;
                }
                
                
                if (maxRow != i)
                {
                    for (int k = i; k < n; k++)
                    {
                        double temp = A[i, k];
                        A[i, k] = A[maxRow, k];
                        A[maxRow, k] = temp;
                    }
                    double tempB = B[i];
                    B[i] = B[maxRow];
                    B[maxRow] = tempB;
                }
                
                
                for (int k = i + 1; k < n; k++)
                {
                    double factor = A[k, i] / A[i, i];
                    for (int j = i; j < n; j++)
                    {
                        A[k, j] -= factor * A[i, j];
                    }
                    B[k] -= factor * B[i];
                }
            }
            
            
            for (int i = n - 1; i >= 0; i--)
            {
                x[i] = B[i];
                for (int j = i + 1; j < n; j++)
                {
                    x[i] -= A[i, j] * x[j];
                }
                x[i] /= A[i, i];
            }
            
            return x;
        }
        
        public Dictionary<string, double> GetResults()
        {
            var results = new Dictionary<string, double>();
            
            
            results["p00"] = probabilities[0];
            results["p10"] = probabilities[1];
            results["p01"] = probabilities[2];
            
            for (int q = 0; q <= L; q++)
            {
                results[$"p11{q}"] = probabilities[3 + q];
            }
            
            
            double avgQueueLength = 0;
            for (int q = 0; q <= L; q++)
            {
                avgQueueLength += q * probabilities[3 + q];
            }
            results["Средняя длина очереди"] = avgQueueLength;
            
            
            results["Вероятность простоя"] = probabilities[0];
            
            
            results["Вероятность 1 канал занят"] = probabilities[1] + probabilities[2];
            
            
            double pBothBusy = 0;
            for (int q = 0; q <= L; q++)
            {
                pBothBusy += probabilities[3 + q];
            }
            results["Вероятность оба канала заняты"] = pBothBusy;
            
            
            results["Вероятность потери (очередь полна)"] = probabilities[3 + L];
            
            
            double impatienceRate = 0;
            for (int q = 1; q <= L; q++)
            {
                impatienceRate += q * theta * probabilities[3 + q];
            }
            results["Интенсивность ухода из очереди"] = impatienceRate;
            
            
            results["Интенсивность входящего потока"] = lambda;
            
            
            double serviceRate = mu * (probabilities[1] + probabilities[2]) + 
                                2 * mu * pBothBusy;
            results["Интенсивность обслуживания"] = serviceRate;
            
            
            results["Коэффициент использования"] = 1 - probabilities[0];
            
            return results;
        }
        
        public double[] GetProbabilityArray()
        {
            return (double[])probabilities.Clone();
        }
    }
}