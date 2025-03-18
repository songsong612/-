const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 5000;

// 中间件
app.use(cors());
app.use(bodyParser.json({ limit: '10mb' }));
app.use(express.static(path.join(__dirname, '/')));

// 模拟API密钥验证
function validateApiKey(service, apiKey) {
    if (!apiKey || apiKey === '') {
        return false;
    }
    
    // 在实际应用中，这里会验证真实的API密钥
    // 在这个演示中，我们只是检查非空值
    return true;
}

// 主页路由
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

// DeepSeek AI 分析API
app.post('/api/analyze/deepseek', (req, res) => {
    const { image, query, apiKey } = req.body;
    
    // 验证API密钥
    if (!validateApiKey('deepseek', apiKey)) {
        return res.status(401).json({ error: 'Invalid API key' });
    }
    
    // 在实际应用中，这里会调用DeepSeek API
    // 在这个演示中，我们返回模拟数据
    setTimeout(() => {
        res.json({
            service: 'DeepSeek',
            query: query,
            result: `这是DeepSeek AI的分析结果。\n\n您的查询是: "${query}"\n\n图像分析显示这可能是${getRandomImageContent()}。根据图像中的元素，我可以提供更详细的分析和见解。\n\n如果您有关于图像的特定问题，请随时提问。`
        });
    }, 1000);
});

// Google AI 分析API
app.post('/api/analyze/google', (req, res) => {
    const { image, query, apiKey } = req.body;
    
    // 验证API密钥
    if (!validateApiKey('google', apiKey)) {
        return res.status(401).json({ error: 'Invalid API key' });
    }
    
    // 在实际应用中，这里会调用Google AI API
    // 在这个演示中，我们返回模拟数据
    setTimeout(() => {
        res.json({
            service: 'Google AI',
            query: query,
            result: `根据Google AI的分析，这张图片显示了${getRandomImageContent()}。\n\n您的查询是: "${query}"\n\n我可以看到图片中的多个元素，包括色彩、构图和主题内容。如果您需要关于特定内容的更多信息，请详细说明您的问题。`
        });
    }, 1200);
});

// Grok 分析API
app.post('/api/analyze/grok', (req, res) => {
    const { image, query, apiKey } = req.body;
    
    // 验证API密钥
    if (!validateApiKey('grok', apiKey)) {
        return res.status(401).json({ error: 'Invalid API key' });
    }
    
    // 在实际应用中，这里会调用Grok API
    // 在这个演示中，我们返回模拟数据
    setTimeout(() => {
        res.json({
            service: 'Grok',
            query: query,
            result: `Grok分析结果：\n\n这张图片包含${getRandomImageContent()}。\n\n您询问的是: "${query}"\n\n从图像中我可以识别多种元素，其构图和内容看起来很有吸引力。图像质量良好，细节清晰可见。\n\n如果您想了解更多关于图片中特定内容的信息，请告诉我您关注的是哪个部分。`
        });
    }, 1500);
});

// 生成随机图像内容描述(用于演示)
function getRandomImageContent() {
    const subjects = [
        '自然景观', '城市风光', '人物肖像', '动物图片', 
        '建筑设计', '艺术作品', '食物照片', '科技产品',
        '抽象图案', '海洋景色', '山脉风景', '花卉植物'
    ];
    
    const details = [
        '色彩鲜明', '构图平衡', '光影效果出色', 
        '细节丰富', '主题突出', '风格独特',
        '画面清晰', '视角新颖', '情感表达强烈'
    ];
    
    const randomSubject = subjects[Math.floor(Math.random() * subjects.length)];
    const randomDetail = details[Math.floor(Math.random() * details.length)];
    
    return `${randomSubject}，${randomDetail}`;
}

// 启动服务器
app.listen(PORT, () => {
    console.log(`服务器运行在 http://localhost:${PORT}`);
});