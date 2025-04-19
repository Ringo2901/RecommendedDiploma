let metricsData = {
    precision: [],
    recall: [],
    f1Score: [],
    nDCG: [],
    hitRate: [],
    coverage: [],
    personalization: []
};

function updateMetrics() {
    const limit = document.getElementById('limit').value;
    fetchMetrics(limit);
}

async function fetchMetrics(limit) {
    const response = await fetch(`/api/evaluate-model?limit=${limit}`);
    const data = await response.json();

    // Обновляем текстовые метрики на странице
    document.getElementById('precision').innerText = data.precision;
    document.getElementById('recall').innerText = data.recall;
    document.getElementById('f1Score').innerText = data.f1Score;
    document.getElementById('nDCG').innerText = data.nDCG;
    document.getElementById('hitRate').innerText = data.hitRate;
    document.getElementById('coverage').innerText = data.coverage;
    document.getElementById('personalization').innerText = data.personalization;

    // Заполняем данные для графиков
    metricsData.precision.push(data.precision);
    metricsData.recall.push(data.recall);
    metricsData.f1Score.push(data.f1Score);
    metricsData.nDCG.push(data.nDCG);
    metricsData.hitRate.push(data.hitRate);
    metricsData.coverage.push(data.coverage);
    metricsData.personalization.push(data.personalization);

    // Обновляем графики
    updateCharts();
}

function updateCharts() {
    const limit = document.getElementById('limit').value;

    const precisionChart = new Chart(document.getElementById('precisionChart'), {
        type: 'line',
        data: {
            labels: Array.from({ length: metricsData.precision.length }, (_, i) => i + 1),
            datasets: [{
                label: 'Precision',
                data: metricsData.precision,
                borderColor: 'rgb(75, 192, 192)',
                fill: false
            }]
        }
    });

    const recallChart = new Chart(document.getElementById('recallChart'), {
        type: 'line',
        data: {
            labels: Array.from({ length: metricsData.recall.length }, (_, i) => i + 1),
            datasets: [{
                label: 'Recall',
                data: metricsData.recall,
                borderColor: 'rgb(54, 162, 235)',
                fill: false
            }]
        }
    });

    const f1ScoreChart = new Chart(document.getElementById('f1ScoreChart'), {
        type: 'line',
        data: {
            labels: Array.from({ length: metricsData.f1Score.length }, (_, i) => i + 1),
            datasets: [{
                label: 'F1-Score',
                data: metricsData.f1Score,
                borderColor: 'rgb(255, 159, 64)',
                fill: false
            }]
        }
    });

    const nDCGChart = new Chart(document.getElementById('nDCGChart'), {
        type: 'line',
        data: {
            labels: Array.from({ length: metricsData.nDCG.length }, (_, i) => i + 1),
            datasets: [{
                label: 'nDCG',
                data: metricsData.nDCG,
                borderColor: 'rgb(153, 102, 255)',
                fill: false
            }]
        }
    });
}
