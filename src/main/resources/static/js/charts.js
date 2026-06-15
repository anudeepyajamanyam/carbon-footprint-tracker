// Chart.js helper module for Carbon Tracker

let breakdownChartInstance = null;
let trendChartInstance = null;

function initCharts() {
    const ctxBreakdown = document.getElementById('breakdownChart');
    const ctxTrend = document.getElementById('trendChart');

    if (!ctxBreakdown || !ctxTrend) return;

    // Destroy existing instances to release the canvas
    if (breakdownChartInstance) {
        breakdownChartInstance.destroy();
    }
    if (trendChartInstance) {
        trendChartInstance.destroy();
    }

    // 1. Doughnut Chart for Category Breakdown
    breakdownChartInstance = new Chart(ctxBreakdown, {
        type: 'doughnut',
        data: {
            labels: ['Transport', 'Energy', 'Food', 'Waste'],
            datasets: [{
                data: [0, 0, 0, 0],
                backgroundColor: [
                    '#06b6d4', // Transport (Cyan)
                    '#fbbf24', // Energy (Yellow)
                    '#8b5cf6', // Food (Purple)
                    '#ec4899'  // Waste (Pink)
                ],
                borderWidth: 2,
                borderColor: '#111827',
                hoverOffset: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        color: '#9ca3af',
                        font: {
                            family: 'Outfit',
                            size: 12
                        },
                        padding: 15
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return ` ${context.label}: ${context.raw.toFixed(1)} kg CO2e`;
                        }
                    }
                }
            },
            cutout: '70%'
        }
    });

    // 2. Bar Chart for Monthly Comparison (Current vs Previous vs Budget)
    trendChartInstance = new Chart(ctxTrend, {
        type: 'bar',
        data: {
            labels: ['Previous Month', 'Current Month', 'Monthly Goal'],
            datasets: [{
                label: 'Emissions (kg CO2e)',
                data: [0, 0, 0],
                backgroundColor: [
                    'rgba(156, 163, 175, 0.4)', // Previous (Gray)
                    'rgba(16, 185, 129, 0.75)', // Current (Green)
                    'rgba(6, 182, 212, 0.2)'    // Target (Cyan border)
                ],
                borderColor: [
                    'rgba(156, 163, 175, 0.7)',
                    '#10b981',
                    '#06b6d4'
                ],
                borderWidth: 2,
                borderRadius: 8,
                barThickness: 32
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return ` ${context.raw.toFixed(1)} kg CO2e`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    grid: {
                        color: 'rgba(255, 255, 255, 0.05)'
                    },
                    ticks: {
                        color: '#9ca3af',
                        font: {
                            family: 'Outfit'
                        }
                    }
                },
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: '#9ca3af',
                        font: {
                            family: 'Outfit',
                            weight: '500'
                        }
                    }
                }
            }
        }
    });
}

function updateDashboardCharts(breakdown, currentTotal, previousTotal, goal) {
    if (!breakdownChartInstance || !trendChartInstance) return;

    // Update Doughnut Chart (clamped to 0 to prevent Chart.js from breaking with negative offsets)
    const transport = Math.max(0, breakdown.TRANSPORT || 0);
    const energy = Math.max(0, breakdown.ENERGY || 0);
    const food = Math.max(0, breakdown.FOOD || 0);
    const waste = Math.max(0, breakdown.WASTE || 0);

    breakdownChartInstance.data.datasets[0].data = [transport, energy, food, waste];
    breakdownChartInstance.update();

    // Update Bar Chart
    trendChartInstance.data.datasets[0].data = [
        previousTotal || 0,
        currentTotal || 0,
        goal || 0
    ];

    // Dynamic coloring based on goal status
    if (goal && currentTotal > goal) {
        trendChartInstance.data.datasets[0].backgroundColor[1] = 'rgba(244, 63, 94, 0.75)'; // Red warning
        trendChartInstance.data.datasets[0].borderColor[1] = '#f43f5e';
    } else {
        trendChartInstance.data.datasets[0].backgroundColor[1] = 'rgba(16, 185, 129, 0.75)'; // Normal green
        trendChartInstance.data.datasets[0].borderColor[1] = '#10b981';
    }

    trendChartInstance.update();
}
