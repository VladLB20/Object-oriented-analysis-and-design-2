using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace QueueSimulationApp
{
    public partial class MainForm : Form
    {
        private QueueSimulator simulator;
        private KolmogorovSolver kolmogorovSolver;
        private ListBox lstQueueHistory;
        private ListBox lstEvents;
        private TextBox txtLambda, txtMu, txtTheta, txtMaxQueue, txtTime, txtChannelCount;
        private Button btnStart, btnStop, btnReset;
        private Label lblStatus, lblCurrentState;
        private DataGridView dgvStats;
        private Panel pnlStateVisual;
        private CheckBox chkAutoScroll;
        private TabControl tabControl;
        private TabPage tabSimulation, tabKolmogorov;
        private DataGridView dgvKolmogorov;
        private Button btnSolveKolmogorov, btnCompareResults;
        private TextBox txtKolmTime, txtKolmDt;
        private Label lblKolmResults;
        private Dictionary<string, double> stateTimes;
        private string lastStateKey;
        private double lastStateChangeTime;

        public MainForm()
        {
            InitializeComponent();
            Text = "Симулятор многоканальной СМО с ожиданием + уравнения Колмогорова";
            Size = new Size(1200, 800);
        }

        private void InitializeComponent()
        {
            tabControl = new TabControl
            {
                Location = new Point(10, 10),
                Size = new Size(1160, 730),
                Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right
            };

            tabSimulation = new TabPage(" Имитационное моделирование");
            tabKolmogorov = new TabPage(" Решить уравнения");

            tabControl.Controls.Add(tabSimulation);
            tabControl.Controls.Add(tabKolmogorov);

            var pnlParams = new Panel
            {
                Location = new Point(10, 10),
                Size = new Size(350, 230),
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.LightGray
            };

            int y = 15;
            AddLabel(pnlParams, "Интенсивность поступления (λ):", 10, y);
            txtLambda = AddTextBox(pnlParams, "1.0", 200, y); y += 30;

            AddLabel(pnlParams, "Интенсивность обслуживания (μ):", 10, y);
            txtMu = AddTextBox(pnlParams, "0.8", 200, y); y += 30;

            AddLabel(pnlParams, "Интенсивность ухода (θ):", 10, y);
            txtTheta = AddTextBox(pnlParams, "0.1", 200, y); y += 30;

            AddLabel(pnlParams, "Количество каналов:", 10, y);
            txtChannelCount = AddTextBox(pnlParams, "2", 200, y); y += 30;

            AddLabel(pnlParams, "Макс. длина очереди:", 10, y);
            txtMaxQueue = AddTextBox(pnlParams, "5", 200, y); y += 30;

            AddLabel(pnlParams, "Время моделирования:", 10, y);
            txtTime = AddTextBox(pnlParams, "100", 200, y);

            var pnlControls = new Panel
            {
                Location = new Point(10, 250),
                Size = new Size(350, 100),
                BorderStyle = BorderStyle.FixedSingle
            };

            btnStart = new Button { Text = "▶ Старт", Location = new Point(10, 10), Size = new Size(80, 30) };
            btnStop = new Button { Text = "⏹ Стоп", Location = new Point(100, 10), Size = new Size(80, 30), Enabled = false };
            btnReset = new Button { Text = "🔄 Сброс", Location = new Point(190, 10), Size = new Size(80, 30) };

            lblStatus = new Label { Text = "Готово к запуску", Location = new Point(10, 50), Size = new Size(320, 20), Font = new Font("Arial", 10, FontStyle.Bold) };

            pnlControls.Controls.AddRange(new Control[] { btnStart, btnStop, btnReset, lblStatus });

            pnlStateVisual = new Panel
            {
                Location = new Point(10, 360),
                Size = new Size(350, 150),
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.White
            };

            lblCurrentState = new Label
            {
                Text = "Состояние: Ожидание запуска",
                Location = new Point(10, 10),
                Size = new Size(330, 130),
                Font = new Font("Arial", 11),
                TextAlign = ContentAlignment.MiddleCenter
            };
            pnlStateVisual.Controls.Add(lblCurrentState);

            var lblQueueHistory = new Label { Text = "📊 История очереди:", Location = new Point(370, 10), Size = new Size(300, 20), Font = new Font("Arial", 10, FontStyle.Bold) };
            lstQueueHistory = new ListBox
            {
                Location = new Point(370, 35),
                Size = new Size(400, 200),
                Font = new Font("Consolas", 9),
                BackColor = Color.LightCyan
            };

            var lblEvents = new Label { Text = "📝 События системы:", Location = new Point(370, 245), Size = new Size(300, 20), Font = new Font("Arial", 10, FontStyle.Bold) };
            lstEvents = new ListBox
            {
                Location = new Point(370, 270),
                Size = new Size(400, 200),
                Font = new Font("Consolas", 9),
                BackColor = Color.LightYellow
            };

            chkAutoScroll = new CheckBox { Text = "Автопрокрутка", Location = new Point(370, 475), Size = new Size(120, 20), Checked = true };

            var lblStats = new Label { Text = "📈 Статистика:", Location = new Point(370, 500), Size = new Size(300, 20), Font = new Font("Arial", 10, FontStyle.Bold) };
            dgvStats = new DataGridView
            {
                Location = new Point(370, 525),
                Size = new Size(400, 180),
                ColumnCount = 2,
                RowHeadersVisible = false,
                AllowUserToAddRows = false,
                BackgroundColor = Color.White,
                BorderStyle = BorderStyle.FixedSingle,
                Font = new Font("Arial", 9)
            };
            dgvStats.Columns[0].Width = 250;
            dgvStats.Columns[1].Width = 150;
            dgvStats.Columns[0].HeaderText = "Параметр";
            dgvStats.Columns[1].HeaderText = "Значение";

            tabSimulation.Controls.AddRange(new Control[]
            {
                pnlParams, pnlControls, pnlStateVisual,
                lblQueueHistory, lstQueueHistory,
                lblEvents, lstEvents, chkAutoScroll,
                lblStats, dgvStats
            });

            var lblKolmogorovTitle = new Label
            {
                Text = " ЧИСЛЕННОЕ РЕШЕНИЕ",
                Location = new Point(10, 10),
                Size = new Size(600, 30),
                Font = new Font("Arial", 12, FontStyle.Bold),
                TextAlign = ContentAlignment.MiddleCenter
            };

            var pnlKolmParams = new Panel
            {
                Location = new Point(10, 50),
                Size = new Size(400, 100),
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.LightGray
            };

            int y2 = 15;
            AddLabel(pnlKolmParams, "Время решения (T):", 10, y2);
            txtKolmTime = AddTextBox(pnlKolmParams, "100", 150, y2); y2 += 30;

            AddLabel(pnlKolmParams, "Шаг интегрирования (dt):", 10, y2);
            txtKolmDt = AddTextBox(pnlKolmParams, "0.01", 150, y2);

            btnSolveKolmogorov = new Button
            {
                Text = "Решить уравнения Колмогорова",
                Location = new Point(420, 50),
                Size = new Size(180, 30)
            };

            btnCompareResults = new Button
            {
                Text = "Сравнить с имитацией",
                Location = new Point(610, 50),
                Size = new Size(150, 30),
                Enabled = false
            };

            lblKolmResults = new Label
            {
                Text = "Результаты будут отображены здесь",
                Location = new Point(10, 160),
                Size = new Size(800, 30),
                Font = new Font("Arial", 10, FontStyle.Italic),
                TextAlign = ContentAlignment.MiddleLeft
            };

            dgvKolmogorov = new DataGridView
            {
                Location = new Point(10, 200),
                Size = new Size(1120, 480),
                ColumnCount = 2,
                RowHeadersVisible = false,
                AllowUserToAddRows = false,
                BackgroundColor = Color.White,
                BorderStyle = BorderStyle.FixedSingle,
                Font = new Font("Arial", 9)
            };
            dgvKolmogorov.Columns[0].Width = 400;
            dgvKolmogorov.Columns[1].Width = 200;
            dgvKolmogorov.Columns[0].HeaderText = "Параметр / Состояние";
            dgvKolmogorov.Columns[1].HeaderText = "Значение";

            tabKolmogorov.Controls.AddRange(new Control[]
            {
                lblKolmogorovTitle, pnlKolmParams, btnSolveKolmogorov,
                btnCompareResults, lblKolmResults, dgvKolmogorov
            });

            Controls.Add(tabControl);

            btnStart.Click += BtnStart_Click;
            btnStop.Click += BtnStop_Click;
            btnReset.Click += BtnReset_Click;
            btnSolveKolmogorov.Click += BtnSolveKolmogorov_Click;
            btnCompareResults.Click += BtnCompareResults_Click;
        }

        private void AddLabel(Panel panel, string text, int x, int y)
        {
            var label = new Label
            {
                Text = text,
                Location = new Point(x, y),
                Size = new Size(180, 20),
                Font = new Font("Arial", 9)
            };
            panel.Controls.Add(label);
        }

        private TextBox AddTextBox(Panel panel, string defaultValue, int x, int y)
        {
            var txt = new TextBox
            {
                Text = defaultValue,
                Location = new Point(x, y),
                Size = new Size(120, 23),
                Font = new Font("Arial", 9)
            };
            panel.Controls.Add(txt);
            return txt;
        }

        private async void BtnStart_Click(object sender, EventArgs e)
        {
            if (!ValidateInputs())
                return;

            double lambda = double.Parse(txtLambda.Text);
            double mu = double.Parse(txtMu.Text);
            double theta = double.Parse(txtTheta.Text);
            int channelCount = int.Parse(txtChannelCount.Text);
            int maxQueue = int.Parse(txtMaxQueue.Text);
            double simTime = double.Parse(txtTime.Text);

            InitializeStateStatistics(channelCount, maxQueue);

            lstQueueHistory.Items.Clear();
            lstEvents.Items.Clear();
            dgvStats.Rows.Clear();

            simulator = new QueueSimulator(lambda, mu, theta, channelCount, maxQueue, simTime);

            simulator.OnStateChanged += (state, time) =>
            {
                this.Invoke(new Action(() =>
                {
                    UpdateStateStatistics(state, time);
                    UpdateStateDisplay(state, time);

                    string stateStr = $"[{time:F2}] ";
                        for (int i = 0; i < state.Channels.Length; i++)
                        {
                            stateStr += $"Канал{i+1}:{(state.Channels[i] ? "Занят" : "Свободен")} ";
                        }
                        stateStr += $", Очередь: {state.QueueLength}/{state.MaxQueueLength}";

                    lstQueueHistory.Items.Add(stateStr);
                    if (lstQueueHistory.Items.Count > 100)
                        lstQueueHistory.Items.RemoveAt(0);

                    if (chkAutoScroll.Checked)
                        lstQueueHistory.TopIndex = lstQueueHistory.Items.Count - 1;

                    lblStatus.Text = $"Моделирование... Время: {time:F2}";
                }));
            };

            simulator.OnEvent += (message) =>
            {
                this.Invoke(new Action(() =>
                {
                    lstEvents.Items.Add($"[{DateTime.Now:HH:mm:ss}] {message}");
                    if (lstEvents.Items.Count > 50)
                        lstEvents.Items.RemoveAt(0);

                    if (chkAutoScroll.Checked)
                        lstEvents.TopIndex = lstEvents.Items.Count - 1;
                }));
            };

            simulator.OnSimulationCompleted += (stats) =>
            {
                this.Invoke(new Action(() =>
                {
                    FinalizeStateStatistics(simTime);
                    UpdateStatistics(stats);
                    btnStart.Enabled = true;
                    btnStop.Enabled = false;
                    btnReset.Enabled = true;
                    btnCompareResults.Enabled = true;
                    lblStatus.Text = "Завершено";

                    ShowCompletionMessage(stats);
                }));
            };

            btnStart.Enabled = false;
            btnStop.Enabled = true;
            btnReset.Enabled = false;
            btnCompareResults.Enabled = false;

            await simulator.RunAsync();
        }

        private void InitializeStateStatistics(int channelCount, int maxQueue)
        {
            stateTimes = new Dictionary<string, double>();
            if (channelCount == 2)
            {
                stateTimes["p00"] = 0;
                stateTimes["p10"] = 0;
                stateTimes["p01"] = 0;
                for (int q = 0; q <= maxQueue; q++)
                    stateTimes[$"p11{q}"] = 0;
            }
            lastStateKey = null;
            lastStateChangeTime = 0;
        }

        private void UpdateStateStatistics(SystemState state, double time)
        {
            if (state.ChannelCount != 2) return;
            if (lastStateKey != null)
            {
                double timeInState = time - lastStateChangeTime;
                stateTimes[lastStateKey] += timeInState;
            }

            lastStateKey = GetStateKey(state);
            lastStateChangeTime = time;
        }

        private void FinalizeStateStatistics(double totalTime)
        {
            if (stateTimes == null || lastStateKey == null) return;
            double timeInState = totalTime - lastStateChangeTime;
            stateTimes[lastStateKey] += timeInState;

            foreach (var key in stateTimes.Keys.ToList())
                stateTimes[key] /= totalTime;
        }

        private string GetStateKey(SystemState state)
        {
            if (state.ChannelCount != 2) return null;
            if (!state.Channels[0] && !state.Channels[1])
                return "p00";
            else if (state.Channels[0] && !state.Channels[1])
                return "p10";
            else if (!state.Channels[0] && state.Channels[1])
                return "p01";
            else
                return $"p11{state.QueueLength}";
        }

        private void UpdateStateDisplay(SystemState state, double time)
        {
            string stateText = $"ВРЕМЯ: {time:F2}\n\n";
            stateText += $"📊 ТЕКУЩЕЕ СОСТОЯНИЕ:\n";
            stateText += $"────────────────────\n";
            for (int i = 0; i < state.ChannelCount; i++)
            {
                stateText += $"Канал {i + 1}: {(state.Channels[i] ? "🔴 ЗАНЯТ" : "✅ СВОБОДЕН")}\n";
            }
            stateText += $"────────────────────\n";
            stateText += $"Очередь: {state.QueueLength} / {state.MaxQueueLength}\n";
            stateText += $"Свободных каналов: {state.Channels.Count(c => !c)}";

            lblCurrentState.Text = stateText;

            if (state.QueueLength == state.MaxQueueLength)
                pnlStateVisual.BackColor = Color.LightPink;
            else if (state.QueueLength > state.MaxQueueLength / 2)
                pnlStateVisual.BackColor = Color.LightYellow;
            else
                pnlStateVisual.BackColor = Color.LightGreen;
        }

        private void UpdateStatistics(Statistics stats)
        {
            dgvStats.Rows.Clear();

            dgvStats.Rows.Add("Всего заявок поступило", stats.TotalRequests);
            dgvStats.Rows.Add("Успешно обслужено", stats.ServedRequests);
            dgvStats.Rows.Add("Потеряно (очередь полна)", stats.LostRequestsQueueFull);
            dgvStats.Rows.Add("Ушли из очереди (нетерпение)", stats.LostRequestsImpatience);
            dgvStats.Rows.Add("Всего потеряно", stats.LostRequestsQueueFull + stats.LostRequestsImpatience);

            if (stats.TotalRequests > 0)
            {
                double serviceProb = (double)stats.ServedRequests / stats.TotalRequests;
                double lossProb = (double)(stats.LostRequestsQueueFull + stats.LostRequestsImpatience) / stats.TotalRequests;

                dgvStats.Rows.Add("Вероятность обслуживания", $"{serviceProb:P2}");
                dgvStats.Rows.Add("Вероятность потери", $"{lossProb:P2}");
            }

            if (stats.ServedRequests > 0)
            {
                dgvStats.Rows.Add("Среднее время в очереди", $"{stats.TotalQueueTime / stats.ServedRequests:F2}");
                dgvStats.Rows.Add("Среднее время обслуживания", $"{stats.TotalServiceTime / stats.ServedRequests:F2}");
            }

            dgvStats.Rows.Add("Эффективность системы", stats.TotalRequests > 0 ? $"{((double)stats.ServedRequests / stats.TotalRequests * 100):F1}%" : "0%");
        }

        private void ShowCompletionMessage(Statistics stats)
        {
            string message = $"✅ Моделирование завершено!\n\n" +
                           $"Всего заявок: {stats.TotalRequests}\n" +
                           $"Обслужено: {stats.ServedRequests}\n" +
                           $"Потери: {stats.LostRequestsQueueFull + stats.LostRequestsImpatience}\n" +
                           $"Эффективность: {((double)stats.ServedRequests / stats.TotalRequests * 100):F1}%";

            MessageBox.Show(message, "Результаты моделирования",
                          MessageBoxButtons.OK, MessageBoxIcon.Information);
        }

        private void BtnSolveKolmogorov_Click(object sender, EventArgs e)
        {
            if (!ValidateKolmogorovInputs())
                return;

            try
            {
                double lambda = double.Parse(txtLambda.Text);
                double mu = double.Parse(txtMu.Text);
                double theta = double.Parse(txtTheta.Text);
                int maxQueue = int.Parse(txtMaxQueue.Text);
                double T = double.Parse(txtKolmTime.Text);
                double dt = double.Parse(txtKolmDt.Text);

                dgvKolmogorov.Rows.Clear();

                kolmogorovSolver = new KolmogorovSolver(lambda, mu, theta, maxQueue);
                var results = kolmogorovSolver.Solve(T, dt);
                var steadyStateResults = kolmogorovSolver.SolveSteadyState();

                dgvKolmogorov.Rows.Add("=== РЕЗУЛЬТАТЫ НЕСТАЦИОНАРНОГО РЕШЕНИЯ (t=" + T + ") ===", "");
                dgvKolmogorov.Rows.Add("p00 (оба канала свободны)", results["p00"].ToString("F6"));
                dgvKolmogorov.Rows.Add("p10 (канал 1 занят)", results["p10"].ToString("F6"));
                dgvKolmogorov.Rows.Add("p01 (канал 2 занят)", results["p01"].ToString("F6"));

                for (int q = 0; q <= maxQueue; q++)
                {
                    dgvKolmogorov.Rows.Add($"p11{q} (оба канала заняты, очередь={q})",
                        results[$"p11{q}"].ToString("F6"));
                }

                dgvKolmogorov.Rows.Add("", "");
                dgvKolmogorov.Rows.Add("=== ХАРАКТЕРИСТИКИ СИСТЕМЫ ===", "");
                dgvKolmogorov.Rows.Add("Средняя длина очереди", results["Средняя длина очереди"].ToString("F4"));
                dgvKolmogorov.Rows.Add("Вероятность простоя", results["Вероятность простоя"].ToString("P4"));
                dgvKolmogorov.Rows.Add("Вероятность занятия 1 канала", results["Вероятность 1 канал занят"].ToString("P4"));
                dgvKolmogorov.Rows.Add("Вероятность занятия обоих каналов", results["Вероятность оба канала заняты"].ToString("P4"));
                dgvKolmogorov.Rows.Add("Вероятность потери (очередь полна)", results["Вероятность потери (очередь полна)"].ToString("P4"));
                dgvKolmogorov.Rows.Add("Коэффициент использования", results["Коэффициент использования"].ToString("P4"));

                dgvKolmogorov.Rows.Add("", "");
                dgvKolmogorov.Rows.Add("=== РЕЗУЛЬТАТЫ СТАЦИОНАРНОГО РЕШЕНИЯ ===", "");
                dgvKolmogorov.Rows.Add("p00 (стационарный)", steadyStateResults["p00"].ToString("F6"));
                dgvKolmogorov.Rows.Add("Средняя длина очереди (стационарный)",
                    steadyStateResults["Средняя длина очереди"].ToString("F4"));
                dgvKolmogorov.Rows.Add("Вероятность потери (стационарный)",
                    steadyStateResults["Вероятность потери (очередь полна)"].ToString("P4"));

                for (int i = 0; i < dgvKolmogorov.Rows.Count; i++)
                {
                    string param = dgvKolmogorov.Rows[i].Cells[0].Value?.ToString() ?? "";
                    if (param.StartsWith("p"))
                    {
                        dgvKolmogorov.Rows[i].DefaultCellStyle.BackColor = Color.LightBlue;
                    }
                    else if (param.Contains("ХАРАКТЕРИСТИКИ") || param.Contains("РЕЗУЛЬТАТЫ"))
                    {
                        dgvKolmogorov.Rows[i].DefaultCellStyle.BackColor = Color.LightGray;
                        dgvKolmogorov.Rows[i].DefaultCellStyle.Font = new Font("Arial", 9, FontStyle.Bold);
                    }
                }

                lblKolmResults.Text = $"✅ Уравнения Колмогорова решены успешно (T={T}, dt={dt})";
                lblKolmResults.ForeColor = Color.Green;
            }
            catch (Exception ex)
            {
                lblKolmResults.Text = $"❌ Ошибка при решении уравнений: {ex.Message}";
                lblKolmResults.ForeColor = Color.Red;
                MessageBox.Show($"Ошибка при решении уравнений Колмогорова:\n{ex.Message}",
                              "Ошибка",
                              MessageBoxButtons.OK,
                              MessageBoxIcon.Error);
            }
        }

        private void BtnCompareResults_Click(object sender, EventArgs e)
        {
            if (int.Parse(txtChannelCount.Text) != 2)
            {
                MessageBox.Show("Сравнение доступно только для двухканальной системы.",
                                "Предупреждение",
                                MessageBoxButtons.OK,
                                MessageBoxIcon.Warning);
                return;
            }

            if (stateTimes == null || kolmogorovSolver == null)
            {
                MessageBox.Show("Сначала выполните имитационное моделирование и решение уравнений Колмогорова",
                              "Недостаточно данных",
                              MessageBoxButtons.OK,
                              MessageBoxIcon.Warning);
                return;
            }

            DataGridView dgvComparison = new DataGridView
            {
                Location = new Point(10, 10),
                Size = new Size(900, 500),
                ColumnCount = 4,
                RowHeadersVisible = false,
                AllowUserToAddRows = false,
                BackgroundColor = Color.White,
                BorderStyle = BorderStyle.FixedSingle,
                Font = new Font("Arial", 9)
            };

            dgvComparison.Columns[0].Width = 250;
            dgvComparison.Columns[1].Width = 150;
            dgvComparison.Columns[2].Width = 150;
            dgvComparison.Columns[3].Width = 150;

            dgvComparison.Columns[0].HeaderText = "Параметр";
            dgvComparison.Columns[1].HeaderText = "Имитация";
            dgvComparison.Columns[2].HeaderText = "Численное решение";
            dgvComparison.Columns[3].HeaderText = "Разница";

            var kolmogorovResults = kolmogorovSolver.GetResults();

            dgvComparison.Rows.Add("p00",
                stateTimes.ContainsKey("p00") ? stateTimes["p00"].ToString("F6") : "0",
                kolmogorovResults["p00"].ToString("F6"),
                GetDifferenceString(stateTimes.GetValueOrDefault("p00", 0), kolmogorovResults["p00"]));

            dgvComparison.Rows.Add("p10",
                stateTimes.ContainsKey("p10") ? stateTimes["p10"].ToString("F6") : "0",
                kolmogorovResults["p10"].ToString("F6"),
                GetDifferenceString(stateTimes.GetValueOrDefault("p10", 0), kolmogorovResults["p10"]));

            dgvComparison.Rows.Add("p01",
                stateTimes.ContainsKey("p01") ? stateTimes["p01"].ToString("F6") : "0",
                kolmogorovResults["p01"].ToString("F6"),
                GetDifferenceString(stateTimes.GetValueOrDefault("p01", 0), kolmogorovResults["p01"]));

            int maxQueue = int.Parse(txtMaxQueue.Text);
            for (int q = 0; q <= maxQueue; q++)
            {
                string key = $"p11{q}";
                dgvComparison.Rows.Add(key,
                    stateTimes.ContainsKey(key) ? stateTimes[key].ToString("F6") : "0",
                    kolmogorovResults.ContainsKey(key) ? kolmogorovResults[key].ToString("F6") : "0",
                    GetDifferenceString(stateTimes.GetValueOrDefault(key, 0),
                        kolmogorovResults.ContainsKey(key) ? kolmogorovResults[key] : 0));
            }

            dgvComparison.Rows.Add("", "", "", "");
            dgvComparison.Rows.Add("Средняя длина очереди",
                CalculateAverageQueueLength(stateTimes).ToString("F4"),
                kolmogorovResults["Средняя длина очереди"].ToString("F4"),
                GetDifferenceString(CalculateAverageQueueLength(stateTimes), kolmogorovResults["Средняя длина очереди"]));

            dgvComparison.Rows.Add("Вероятность занятия обоих каналов",
                CalculateBothChannelsBusy(stateTimes).ToString("P4"),
                kolmogorovResults["Вероятность оба канала заняты"].ToString("P4"),
                GetDifferenceString(CalculateBothChannelsBusy(stateTimes), kolmogorovResults["Вероятность оба канала заняты"]));

            for (int i = 0; i < dgvComparison.Rows.Count; i++)
            {
                if (i < dgvComparison.Rows.Count - 1)
                {
                    string diffStr = dgvComparison.Rows[i].Cells[3].Value?.ToString() ?? "";
                    if (diffStr != "")
                    {
                        try
                        {
                            double diff = Math.Abs(double.Parse(diffStr));
                            if (diff > 0.1)
                                dgvComparison.Rows[i].DefaultCellStyle.BackColor = Color.LightPink;
                            else if (diff > 0.05)
                                dgvComparison.Rows[i].DefaultCellStyle.BackColor = Color.LightYellow;
                            else
                                dgvComparison.Rows[i].DefaultCellStyle.BackColor = Color.LightGreen;
                        }
                        catch { }
                    }
                }
            }

            Form comparisonForm = new Form
            {
                Text = "Сравнение результатов имитации и численного решения",
                Size = new Size(950, 600),
                StartPosition = FormStartPosition.CenterParent
            };

            comparisonForm.Controls.Add(dgvComparison);
            comparisonForm.ShowDialog();
        }

        private double CalculateAverageQueueLength(Dictionary<string, double> stateTimes)
        {
            double avg = 0;
            foreach (var kvp in stateTimes)
            {
                if (kvp.Key.StartsWith("p11") && kvp.Key.Length > 3)
                {
                    string qStr = kvp.Key.Substring(3);
                    if (int.TryParse(qStr, out int q))
                    {
                        avg += q * kvp.Value;
                    }
                }
            }
            return avg;
        }

        private double CalculateBothChannelsBusy(Dictionary<string, double> stateTimes)
        {
            double sum = 0;
            foreach (var kvp in stateTimes)
            {
                if (kvp.Key.StartsWith("p11"))
                {
                    sum += kvp.Value;
                }
            }
            return sum;
        }

        private string GetDifferenceString(double value1, double value2)
        {
            double diff = Math.Abs(value1 - value2);
            return diff.ToString("F6");
        }

        private void BtnStop_Click(object sender, EventArgs e)
        {
            simulator?.Stop();
            btnStart.Enabled = true;
            btnStop.Enabled = false;
            btnReset.Enabled = true;
            btnCompareResults.Enabled = true;
            lblStatus.Text = "Остановлено пользователем";
        }

        private void BtnReset_Click(object sender, EventArgs e)
        {
            lstQueueHistory.Items.Clear();
            lstEvents.Items.Clear();
            dgvStats.Rows.Clear();
            dgvKolmogorov.Rows.Clear();
            lblCurrentState.Text = "Состояние: Ожидание запуска";
            lblStatus.Text = "Готово к запуску";
            lblKolmResults.Text = "Результаты будут отображены здесь";
            lblKolmResults.ForeColor = Color.Black;
            pnlStateVisual.BackColor = Color.White;
            btnCompareResults.Enabled = false;
            stateTimes = null;
            kolmogorovSolver = null;
        }

        private bool ValidateInputs()
        {
            try
            {
                double lambda = double.Parse(txtLambda.Text);
                double mu = double.Parse(txtMu.Text);
                double theta = double.Parse(txtTheta.Text);
                int channelCount = int.Parse(txtChannelCount.Text);
                int maxQueue = int.Parse(txtMaxQueue.Text);
                double simTime = double.Parse(txtTime.Text);

                if (lambda <= 0 || mu <= 0 || theta < 0 || channelCount <= 0 || maxQueue <= 0 || simTime <= 0)
                {
                    MessageBox.Show("Все параметры должны быть положительными числами!",
                                  "Ошибка ввода", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    return false;
                }

                return true;
            }
            catch (FormatException)
            {
                MessageBox.Show("Пожалуйста, введите корректные числовые значения!",
                              "Ошибка ввода", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
        }

        private bool ValidateKolmogorovInputs()
        {
            try
            {
                double lambda = double.Parse(txtLambda.Text);
                double mu = double.Parse(txtMu.Text);
                double theta = double.Parse(txtTheta.Text);
                int maxQueue = int.Parse(txtMaxQueue.Text);
                double T = double.Parse(txtKolmTime.Text);
                double dt = double.Parse(txtKolmDt.Text);

                if (lambda <= 0 || mu <= 0 || theta < 0 || maxQueue <= 0 || T <= 0 || dt <= 0)
                {
                    MessageBox.Show("Все параметры должны быть положительными числами!",
                                  "Ошибка ввода", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    return false;
                }

                if (dt > T / 10)
                {
                    MessageBox.Show("Шаг интегрирования слишком велик! Рекомендуется dt < T/10",
                                  "Предупреждение", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }

                return true;
            }
            catch (FormatException)
            {
                MessageBox.Show("Пожалуйста, введите корректные числовые значения!",
                              "Ошибка ввода", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
        }
    }
}