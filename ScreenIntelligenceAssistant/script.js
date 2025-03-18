// 全局变量
let isServiceRunning = false;
let hasPermission = false;
let currentCapture = null;
let currentAiService = 0; // 0: DeepSeek, 1: Google AI, 2: Grok
let apiKeys = {
    deepseek: '',
    google: '',
    grok: ''
};

// DOM元素引用
const floatingWindow = document.getElementById('floating-window');
const btnStartService = document.getElementById('btn-start-service');
const btnPermission = document.getElementById('btn-permission');
const btnSettings = document.getElementById('btn-settings');
const btnExpand = document.getElementById('btn-expand');
const btnClose = document.getElementById('btn-close');
const btnCapture = document.getElementById('btn-capture');
const btnAnalyze = document.getElementById('btn-analyze');
const serviceStatus = document.getElementById('service-status');
const permissionStatus = document.getElementById('permission-status');
const collapsedView = document.getElementById('collapsed-view');
const expandedView = document.getElementById('expanded-view');
const captureArea = document.getElementById('capture-area');
const previewImage = document.getElementById('preview-image');
const capturePlaceholder = document.getElementById('capture-placeholder');
const loadingSpinner = document.getElementById('loading-spinner');
const resultText = document.getElementById('result-text');
const queryInput = document.getElementById('query-input');
const aiServiceSelect = document.getElementById('ai-service-select');
const aiServiceSpinner = document.getElementById('ai-service-spinner');
const settingsModal = new bootstrap.Modal(document.getElementById('settings-modal'));
const opacitySlider = document.getElementById('opacity-slider');
const opacityValue = document.getElementById('opacity-value');
const sizeSlider = document.getElementById('size-slider');
const sizeValue = document.getElementById('size-value');
const deepseekApiKey = document.getElementById('deepseek-api-key');
const googleApiKey = document.getElementById('google-api-key');
const grokApiKey = document.getElementById('grok-api-key');
const saveSettings = document.getElementById('save-settings');
const dragOverlay = document.getElementById('drag-overlay');

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    // 从本地存储加载设置
    loadSettings();
    
    // 设置事件监听器
    setupEventListeners();
});

// 设置事件监听器
function setupEventListeners() {
    // 权限按钮点击事件
    btnPermission.addEventListener('click', requestPermission);
    
    // 服务启动/停止按钮点击事件
    btnStartService.addEventListener('click', toggleService);
    
    // 设置按钮点击事件
    btnSettings.addEventListener('click', () => {
        settingsModal.show();
    });
    
    // 设置保存按钮点击事件
    saveSettings.addEventListener('click', saveSettingsToStorage);
    
    // 窗口展开按钮点击事件
    btnExpand.addEventListener('click', () => {
        collapsedView.style.display = 'none';
        expandedView.style.display = 'block';
        btnExpand.parentElement.style.display = 'none';
    });
    
    // 关闭按钮点击事件
    btnClose.addEventListener('click', () => {
        floatingWindow.style.display = 'none';
        isServiceRunning = false;
        updateServiceStatus();
    });
    
    // 捕获按钮点击事件
    btnCapture.addEventListener('click', captureScreen);
    
    // 分析按钮点击事件
    btnAnalyze.addEventListener('click', analyzeImage);
    
    // AI服务选择事件
    aiServiceSelect.addEventListener('change', (e) => {
        currentAiService = parseInt(e.target.value);
        aiServiceSpinner.value = currentAiService;
        localStorage.setItem('defaultAiService', currentAiService);
    });
    
    // 浮动窗口内AI服务选择事件
    aiServiceSpinner.addEventListener('change', (e) => {
        currentAiService = parseInt(e.target.value);
        aiServiceSelect.value = currentAiService;
        localStorage.setItem('defaultAiService', currentAiService);
    });
    
    // 透明度滑块事件
    opacitySlider.addEventListener('input', (e) => {
        const opacity = e.target.value;
        opacityValue.textContent = opacity + '%';
        floatingWindow.style.backgroundColor = `rgba(0, 0, 0, ${opacity / 100 * 0.8})`;
    });
    
    // 尺寸滑块事件
    sizeSlider.addEventListener('input', (e) => {
        const size = e.target.value;
        sizeValue.textContent = size + '%';
        floatingWindow.style.transform = `scale(${size / 100})`;
        floatingWindow.style.transformOrigin = 'top left';
    });
    
    // 设置浮动窗口拖动功能
    setupDraggable();
}

// 请求屏幕捕获权限（模拟）
function requestPermission() {
    // 在实际应用中，这里会请求真实的屏幕捕获权限
    // 在这个演示中，我们只是模拟权限授予
    
    hasPermission = true;
    permissionStatus.textContent = '已获取屏幕捕获权限';
    btnPermission.style.display = 'none';
    btnStartService.disabled = false;
    
    // 保存权限状态
    localStorage.setItem('hasPermission', 'true');
}

// 切换服务状态
function toggleService() {
    if (isServiceRunning) {
        // 停止服务
        isServiceRunning = false;
        floatingWindow.style.display = 'none';
    } else {
        // 启动服务
        isServiceRunning = true;
        floatingWindow.style.display = 'block';
        
        // 重置窗口位置
        floatingWindow.style.top = '100px';
        floatingWindow.style.left = '20px';
        
        // 显示折叠视图
        collapsedView.style.display = 'block';
        expandedView.style.display = 'none';
        btnExpand.parentElement.style.display = 'inline-flex';
    }
    
    updateServiceStatus();
}

// 更新服务状态显示
function updateServiceStatus() {
    if (isServiceRunning) {
        serviceStatus.textContent = '服务正在运行';
        btnStartService.textContent = '停止浮动窗口';
        btnStartService.classList.replace('btn-success', 'btn-danger');
    } else {
        serviceStatus.textContent = '服务已停止';
        btnStartService.textContent = '启动浮动窗口';
        btnStartService.classList.replace('btn-danger', 'btn-success');
    }
}

// 捕获屏幕（模拟）
function captureScreen() {
    capturePlaceholder.style.display = 'none';
    loadingSpinner.style.display = 'inline-block';
    previewImage.style.display = 'none';
    
    // 模拟捕获过程
    setTimeout(() => {
        // 在实际应用中，这里会捕获真实的屏幕内容
        // 在这个演示中，我们使用占位图像
        const timestamp = new Date().getTime();
        previewImage.src = `https://picsum.photos/800/600?t=${timestamp}`;
        previewImage.onload = () => {
            loadingSpinner.style.display = 'none';
            previewImage.style.display = 'block';
            currentCapture = previewImage.src;
        };
    }, 1000);
}

// 分析图像
function analyzeImage() {
    if (!currentCapture) {
        showToast('请先捕获屏幕内容');
        return;
    }
    
    const query = queryInput.value.trim() || '这张图片中有什么？请提供详细描述。';
    
    // 检查API密钥
    const apiKey = getApiKeyForCurrentService();
    if (!apiKey) {
        resultText.textContent = '错误：缺少API密钥。请在设置中配置API密钥。';
        return;
    }
    
    // 显示加载状态
    resultText.textContent = '分析中...';
    loadingSpinner.style.display = 'inline-block';
    
    // 准备API请求数据
    const requestData = {
        image: currentCapture,
        query: query,
        apiKey: apiKey
    };
    
    // 确定要调用的API端点
    let endpoint;
    switch (currentAiService) {
        case 0:
            endpoint = '/api/analyze/deepseek';
            break;
        case 1:
            endpoint = '/api/analyze/google';
            break;
        case 2:
            endpoint = '/api/analyze/grok';
            break;
    }
    
    // 发送API请求
    axios.post(endpoint, requestData)
        .then(response => {
            const data = response.data;
            resultText.textContent = `[${data.service}分析]\n查询: "${data.query}"\n\n${data.result}`;
        })
        .catch(error => {
            let errorMessage = '分析失败：';
            
            if (error.response) {
                // 服务器返回错误状态码
                if (error.response.status === 401) {
                    errorMessage += 'API密钥无效或不正确。请检查您的设置。';
                } else {
                    errorMessage += `服务器错误 (${error.response.status})`;
                }
            } else if (error.request) {
                // 请求发送但没有收到响应
                errorMessage += '无法连接到服务器。请检查您的网络连接。';
            } else {
                // 请求设置出错
                errorMessage += error.message;
            }
            
            resultText.textContent = errorMessage;
        })
        .finally(() => {
            loadingSpinner.style.display = 'none';
        });
}

// 获取当前服务的API密钥
function getApiKeyForCurrentService() {
    switch (currentAiService) {
        case 0: return apiKeys.deepseek;
        case 1: return apiKeys.google;
        case 2: return apiKeys.grok;
        default: return '';
    }
}

// 从本地存储加载设置
function loadSettings() {
    // 加载API密钥
    apiKeys.deepseek = localStorage.getItem('deepseekApiKey') || '';
    apiKeys.google = localStorage.getItem('googleApiKey') || '';
    apiKeys.grok = localStorage.getItem('grokApiKey') || '';
    
    deepseekApiKey.value = apiKeys.deepseek;
    googleApiKey.value = apiKeys.google;
    grokApiKey.value = apiKeys.grok;
    
    // 加载窗口设置
    const opacity = localStorage.getItem('windowOpacity') || '70';
    const size = localStorage.getItem('windowSize') || '100';
    
    opacitySlider.value = opacity;
    opacityValue.textContent = opacity + '%';
    floatingWindow.style.backgroundColor = `rgba(0, 0, 0, ${opacity / 100 * 0.8})`;
    
    sizeSlider.value = size;
    sizeValue.textContent = size + '%';
    floatingWindow.style.transform = `scale(${size / 100})`;
    floatingWindow.style.transformOrigin = 'top left';
    
    // 加载默认AI服务
    const defaultService = localStorage.getItem('defaultAiService') || '0';
    currentAiService = parseInt(defaultService);
    aiServiceSelect.value = currentAiService;
    aiServiceSpinner.value = currentAiService;
    
    // 加载权限状态
    if (localStorage.getItem('hasPermission') === 'true') {
        hasPermission = true;
        permissionStatus.textContent = '已获取屏幕捕获权限';
        btnPermission.style.display = 'none';
        btnStartService.disabled = false;
    }
}

// 保存设置到本地存储
function saveSettingsToStorage() {
    // 保存API密钥
    apiKeys.deepseek = deepseekApiKey.value;
    apiKeys.google = googleApiKey.value;
    apiKeys.grok = grokApiKey.value;
    
    localStorage.setItem('deepseekApiKey', apiKeys.deepseek);
    localStorage.setItem('googleApiKey', apiKeys.google);
    localStorage.setItem('grokApiKey', apiKeys.grok);
    
    // 保存窗口设置
    localStorage.setItem('windowOpacity', opacitySlider.value);
    localStorage.setItem('windowSize', sizeSlider.value);
    
    // 隐藏设置模态框
    settingsModal.hide();
    
    // 显示保存成功提示
    showToast('设置已保存');
}

// 设置浮动窗口可拖动
function setupDraggable() {
    const header = document.getElementById('floating-window-header');
    let isDragging = false;
    let offsetX, offsetY;
    
    header.addEventListener('mousedown', (e) => {
        isDragging = true;
        offsetX = e.clientX - floatingWindow.getBoundingClientRect().left;
        offsetY = e.clientY - floatingWindow.getBoundingClientRect().top;
        
        // 显示拖动覆盖层
        dragOverlay.style.display = 'block';
    });
    
    document.addEventListener('mousemove', (e) => {
        if (!isDragging) return;
        
        const x = e.clientX - offsetX;
        const y = e.clientY - offsetY;
        
        floatingWindow.style.left = `${x}px`;
        floatingWindow.style.top = `${y}px`;
    });
    
    document.addEventListener('mouseup', () => {
        isDragging = false;
        dragOverlay.style.display = 'none';
    });
}

// 显示提示消息
function showToast(message) {
    // 创建提示元素
    const toast = document.createElement('div');
    toast.className = 'toast align-items-center text-white bg-primary border-0 position-fixed bottom-0 end-0 m-3';
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');
    toast.setAttribute('aria-atomic', 'true');
    
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;
    
    document.body.appendChild(toast);
    
    const bsToast = new bootstrap.Toast(toast, {
        delay: 3000
    });
    
    bsToast.show();
    
    // 自动删除
    toast.addEventListener('hidden.bs.toast', () => {
        document.body.removeChild(toast);
    });
}