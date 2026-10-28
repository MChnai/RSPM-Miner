const colors = ['#3069e0','#10a58d','#e38b37','#915bd2','#e15d6c','#418eaa','#ac8e2d','#4c9d59'];
const steps = ['Bắt đầu', 'Nạp dữ liệu', 'Dựng Vertical DB', 'Tìm L1', 'DFS / khai phá', 'Xuất kết quả'];
const runs = new Map();
const $ = id => document.getElementById(id);
const fmt = (n, digits = 2) => n == null ? '—' : Number(n).toLocaleString('vi-VN', {maximumFractionDigits: digits});
const color = index => colors[index % colors.length];
let charts = {};
let pollTimer = null;

async function jsonApi(path, options) {
  const res = await fetch(path, options);
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  return data;
}

function makeChart(id, labels, yTitle, numericX = false) {
  const chart = new Chart($(id), {
    type: 'line',
    data: {labels: labels || [], datasets: []},
    options: {
      responsive: true, maintainAspectRatio: false,
      animation: false, interaction: {mode: 'nearest', intersect: false},
      elements: {line: {tension: 0.2, borderWidth: 2}, point: {radius: 2.5, hoverRadius: 5}},
      plugins: {legend: {position:'bottom', labels: {boxWidth: 12, usePointStyle: true, font: {size: 10}}}},
      scales: {
        x: {type: numericX ? 'linear' : 'category', title: {display: true, text: numericX ? 'Thời gian từ lúc bắt đầu (ms)' : 'Các giai đoạn thực thi'}, grid: {color:'#f0f2f7'}, ticks: {maxRotation: 0}},
        y: {beginAtZero: true, title: {display:true, text:yTitle}, grid: {color:'#eef1f6'}}
      }
    }
  });
  return chart;
}

function refreshCharts() {
  if (!window.Chart) return;
  const finished = [...runs.values()].filter(r => r.status !== 'ERROR');
  for (const [kind, chart] of Object.entries(charts)) {
    chart.data.datasets = finished.map((run, i) => {
      const palette = color(i);
      const common = {label: `#${run.id} · ${run.dataset} (sup=${run.minSup})`, borderColor:palette,
                      backgroundColor: palette, fill: false, spanGaps: true, pointStyle:'circle'};
      if (kind === 'runtime') {
        const lookup = new Map(run.stages.map(s => [s.name, s.elapsedMs]));
        return {...common, data: steps.map((stage, j) => j === 0 ? 0 : (lookup.get(stage) ?? null))};
      }
      return {...common, data:run.samples.map(s => ({x:s.elapsedMs,
        y:kind === 'cpu' ? s.cpuPercent : s.heapMB}))};
    });
    chart.update('none');
  }
}

function updatePage() {
  const list = [...runs.values()].sort((a,b)=>a.id-b.id);
  const latest = [...list].reverse().find(r => r.status === 'DONE');
  if (latest) {
    $('metricTime').textContent = fmt(latest.durationMs) + ' ms';
    $('metricCpu').textContent = latest.avgCpuPercent == null ? 'N/A' : fmt(latest.avgCpuPercent) + '%';
    $('metricMemory').textContent = fmt(latest.peakHeapMB) + ' MiB';
    $('metricPatterns').textContent = fmt(latest.patterns, 0);
  }
  const tbody = $('historyRows');
  tbody.replaceChildren();
  if (!list.length) {
    const tr=document.createElement('tr'); const td=document.createElement('td');td.colSpan=8;
    td.textContent='Chưa có lần chạy nào.';tr.append(td);tbody.append(tr);
  } else {
    for (const r of [...list].reverse()) {
      const tr=document.createElement('tr');
      const values=[`#${r.id}`,r.dataset,fmt(r.minSup),r.status==='DONE'?fmt(r.durationMs):'—',
        r.status==='DONE'?fmt(r.avgCpuPercent):'—',r.status==='DONE'?fmt(r.peakHeapMB):'—',
        r.status==='DONE'?fmt(r.patterns,0):'—'];
      for (const value of values) {const td=document.createElement('td');td.textContent=value;tr.append(td);}
      const td=document.createElement('td');const badge=document.createElement('span');
      badge.className='status-pill '+r.status;
      badge.textContent={QUEUED:'Đang chờ',RUNNING:'Đang chạy',DONE:'Thành công',ERROR:'Lỗi'}[r.status]||r.status;
      td.append(badge);tr.append(td);tbody.append(tr);
    }
  }
  refreshCharts();
}

function setStatus(text, error=false) {$('status').textContent=text;$('status').classList.toggle('error',error);}

async function trackRun(id) {
  clearInterval(pollTimer);
  const poll = async () => {
    try {
      const run = await jsonApi('/api/runs/'+id);
      runs.set(run.id,run);updatePage();
      if (run.status==='DONE' || run.status==='ERROR') {
        clearInterval(pollTimer);pollTimer=null;$('runButton').disabled=false;
        setStatus(run.status==='DONE' ? `Đã chạy xong #${id}: ${fmt(run.durationMs)} ms, ${run.patterns} mẫu.` : `Lỗi #${id}: ${run.error}`,run.status==='ERROR');
      } else setStatus(`Đang chạy #${id} trên ${run.dataset}… dữ liệu CPU/RAM được lấy mẫu liên tục.`);
    } catch(err) {clearInterval(pollTimer);pollTimer=null;$('runButton').disabled=false;setStatus(err.message,true);}
  };
  await poll();
  if (pollTimer === null && $('runButton').disabled) pollTimer=setInterval(poll,750);
}

async function initialize() {
  if (window.Chart) {
    charts.runtime=makeChart('runtimeChart',steps,'Thời gian tích lũy (ms)');
    charts.cpu=makeChart('cpuChart',null,'CPU (%)',true);
    charts.memory=makeChart('memoryChart',null,'JVM heap đã dùng (MiB)',true);
  } else setStatus('Không thể tải Chart.js. Hãy kiểm tra kết nối Internet.',true);
  try {
    const [datasets, history] = await Promise.all([jsonApi('/api/datasets'),jsonApi('/api/runs')]);
    $('dataset').replaceChildren();
    for (const name of datasets) {const opt=document.createElement('option');opt.value=name;opt.textContent=name;$('dataset').append(opt);}
    const defaultName=datasets.includes('sample_small.txt')?'sample_small.txt':datasets[0];
    if (defaultName) $('dataset').value=defaultName;
    for (const r of history) runs.set(r.id,r);
    updatePage();
    const unfinished=history.find(r=>r.status==='RUNNING'||r.status==='QUEUED');
    if (unfinished) {$('runButton').disabled=true; await trackRun(unfinished.id);}
    else setStatus(`${datasets.length} tập dữ liệu khả dụng. Chọn dữ liệu rồi nhấn Chạy ORN-Miner.`);
  } catch(err) {setStatus(err.message,true);}
}

$('runForm').addEventListener('submit',async event=>{
  event.preventDefault();
  const dataset=$('dataset').value;
  const minSup=$('minSup').value;
  $('runButton').disabled=true;
  setStatus(`Đang gửi yêu cầu chạy ${dataset}…`);
  try {
    const body=new URLSearchParams({dataset,minSup});
    const run=await jsonApi('/api/runs',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body});
    runs.set(run.id,run);updatePage();await trackRun(run.id);
  } catch(err) {$('runButton').disabled=false;setStatus(err.message,true);}
});

initialize();
