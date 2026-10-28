import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np

# Cấu hình Style đồ thị chuẩn báo chí khoa học
plt.style.use('seaborn-v0_8-paper')
plt.rcParams['font.family'] = 'DejaVu Sans'
plt.rcParams['font.size'] = 10

def plot_speedup_distribution():
    """ Figure 4: Distribution of ORN-Miner Speedup over Baselines """
    baselines = ['RNP', 'NTP', 'CMSPADE', 'CMSPAM', 'FAST', 'PrefixSpan']
    geomean_speedups = [5.04, 30.34, 149.16, 53.20, 24.19, 35.82]
    
    fig, ax = plt.subplots(figsize=(7, 4.5))
    bars = ax.bar(baselines, geomean_speedups, color='#2b5c8f', edgecolor='black', width=0.55)
    
    ax.set_yscale('log')
    ax.set_ylabel('Speedup (Baseline / ORN-Miner, log scale)', fontsize=11, fontweight='bold')
    ax.set_xlabel('Baseline Algorithm', fontsize=11, fontweight='bold')
    ax.set_title('RQ1: Geometric Mean Speedup over Baselines (Core12 Benchmark)', fontsize=12, fontweight='bold')
    ax.axhline(1, color='red', linestyle='--', linewidth=1, label='Baseline Baseline (1x)')
    
    for bar in bars:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2.0, yval * 1.15, f'{yval:.1f}x', ha='center', va='bottom', fontweight='bold')
        
    ax.grid(axis='y', linestyle=':', alpha=0.7)
    plt.tight_layout()
    plt.savefig('results/Fig4_Speedup_Distribution.png', dpi=300)
    print("[SUCCESS] Exported 'results/Fig4_Speedup_Distribution.png'")

def plot_ablation_path():
    """ Figure 8: Runtime Distribution across Ablation Variants (V0-V3) """
    variants = ['V0\n(RNP)', 'V1\n(+Bitmap)', 'V2\n(+Pruning)', 'V3\n(Full ORN)']
    runtimes = [891.98, 3233.19, 784.61, 25.48]
    colors = ['#8c8c8c', '#e74c3c', '#3498db', '#2ecc71']
    
    fig, ax = plt.subplots(figsize=(6.5, 4))
    bars = ax.bar(variants, runtimes, color=colors, edgecolor='black', width=0.5)
    
    ax.set_yscale('log')
    ax.set_ylabel('Runtime (ms, log scale)', fontsize=11, fontweight='bold')
    ax.set_title('RQ3: Component Ablation Optimization Path (V0 -> V3)', fontsize=12, fontweight='bold')
    
    for bar in bars:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2.0, yval * 1.2, f'{yval:.1f} ms', ha='center', va='bottom')
        
    ax.grid(axis='y', linestyle=':', alpha=0.7)
    plt.tight_layout()
    plt.savefig('results/Fig8_Ablation_Path.png', dpi=300)
    print("[SUCCESS] Exported 'results/Fig8_Ablation_Path.png'")

if __name__ == '__main__':
    plot_speedup_distribution()
    plot_ablation_path()