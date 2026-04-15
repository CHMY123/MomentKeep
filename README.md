# MomentKeep 朝暮记

## 项目介绍

朝暮记是一款专注于日常打卡、待办管理和倒计时的个人效率工具，旨在帮助用户养成良好的生活习惯，提高生活质量。通过简洁优雅的界面设计和实用的功能，让用户能够轻松记录每一天的成长和进步。

## 核心功能

- **每日打卡**：支持早起、睡眠、用餐和运动打卡，记录每一天的生活习惯
- **今日待办**：管理每日任务，支持添加、编辑、删除和标记完成
- **未来倒计时**：创建重要事件的倒计时，时刻提醒用户珍惜时间
- **个人中心**：管理用户资料，支持头像上传和账户注销
- **AI助手**：集成DeepSeek API，提供智能对话和打卡提醒
- **设置**：个性化应用设置，包括主题、字体大小和背景图片

## 技术栈

- **前端**：Vue 3 + uni-app + Pinia + uni-ui
- **后端**：Spring Boot 3.5.13
- **数据库**：TiDB Cloud
- **存储**：缤纷云
- **AI**：DeepSeek API

## 快速开始

### 环境要求

- Node.js >= 14.0.0
- npm >= 6.0.0
- HBuilderX（推荐，用于uni-app开发）

### 前端运行步骤

1. **克隆项目**

```bash
git clone <项目地址>
cd MomentKeep/frontend/MomentKeep
```

2. **安装依赖**

```bash
npm install
```

3. **运行开发服务器**

- **Web端**

```bash
npm run dev:h5
```

- **微信小程序**

```bash
npm run dev:mp-weixin
```

- **App端**

```bash
npm run dev:app
```

4. **构建生产版本**

- **Web端**

```bash
npm run build:h5
```

- **微信小程序**

```bash
npm run build:mp-weixin
```

- **App端**

```bash
npm run build:app
```

### 后端运行步骤

1. **进入后端目录**

```bash
cd MomentKeep/backend/MomentKeep
```

2. **安装依赖**

```bash
mvn install
```

3. **运行后端服务**

```bash
mvn spring-boot:run
```

## 项目结构

### 前端结构

```
frontend/MomentKeep/
├── pages/             # 页面目录
│   ├── index/         # 首页
│   ├── profile/       # 个人中心
│   ├── daily-checkin/ # 每日打卡
│   ├── todo/          # 今日待办
│   ├── countdown/     # 未来倒计时
│   ├── settings/      # 设置
│   └── about/         # 关于我们
├── store/             # 状态管理
│   ├── index.js       # Pinia配置
│   └── user.js        # 用户状态
├── static/            # 静态资源
├── App.vue            # 应用入口组件
├── main.js            # 应用入口文件
├── pages.json         # 页面配置
├── manifest.json      # 应用配置
└── package.json       # 项目依赖
```

### 后端结构

```
backend/MomentKeep/
├── src/               # 源码目录
│   ├── main/java/     # Java源码
│   └── main/resources/ # 资源文件
├── pom.xml            # Maven配置
└── application.yml    # 应用配置
```

## 数据库设计

项目使用TiDB Cloud作为数据库，主要表结构包括：

- **user**：用户表
- **checkin**：打卡表
- **todo**：待办表
- **countdown**：倒计时表
- **feedback**：反馈表
- **admin**：管理员表
- **user_setting**：用户设置表
- **ai_chat**：AI对话记录表

详细的数据库设计请参考 `数据库设计文档.md`。

## 设计规范

### UI设计

- **配色方案**：雾感米白（#F8F6F2）、雾蓝（#94A7C8）、暖灰粉（#D8C8BE）、焦糖棕（#C2977F）
- **字体设计**：系统无衬线字体，明确的字体层级
- **图标设计**：线性图标，线条粗细1.5px，圆润边角
- **布局设计**：卡片式布局，适当留白，突出呼吸感

### 交互设计

- **动画设计**：柔和的过渡动画，时长300ms
- **反馈设计**：及时的操作反馈，温柔的提示文案
- **手势设计**：适配多端的手势操作
- **响应速度**：优化加载逻辑，响应时间≤300ms

## 注意事项

1. **环境配置**：确保已正确配置TiDB Cloud和缤纷云的连接信息
2. **API密钥**：DeepSeek API密钥需要在 `application.yml` 中配置
3. **跨域处理**：后端需要配置CORS以支持前端跨域请求
4. **性能优化**：对于大量数据的页面，建议使用分页加载

## 贡献指南

1. **Fork 项目**
2. **创建分支**：`git checkout -b feature/your-feature`
3. **提交更改**：`git commit -m 'Add some feature'`
4. **推送到分支**：`git push origin feature/your-feature`
5. **创建 Pull Request**

## 许可证

本项目采用 MIT 许可证。详情请参阅 LICENSE 文件。

## 联系我们

如果您有任何问题或建议，欢迎联系我们。