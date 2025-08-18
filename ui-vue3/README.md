# We-Copilot UI (Vue3)

基于 Vue3 + WebContainer API 的在线代码编辑和预览环境。

## 功能特性

- 🎯 **在线代码编辑**: 基于 CodeMirror 6 的专业代码编辑器
- 🔄 **实时预览**: 基于 WebContainer 的浏览器内 Node.js 环境
- 📁 **文件管理**: 完整的文件浏览器和文件操作功能
- 💻 **集成终端**: 基于 xterm.js 的终端模拟器
- 🎨 **现代 UI**: 基于 Ant Design Vue + Tailwind CSS

## 技术栈

- **前端框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **状态管理**: Pinia
- **UI 组件**: Ant Design Vue
- **样式框架**: Tailwind CSS
- **代码编辑**: CodeMirror 6
- **容器技术**: WebContainer API
- **终端模拟**: xterm.js

## 快速开始

### 1. 安装依赖

```bash
cd apps/we-copilot/ui-vue3
npm install
# 或
pnpm install
```

### 2. 启动开发服务器

```bash
npm run dev
# 或
pnpm dev
```

### 3. 访问应用

打开浏览器访问: http://localhost:5173

## 项目结构

```
src/
├── components/          # 组件
│   ├── CodeEditor.vue   # 代码编辑器
│   ├── FileExplorer.vue # 文件浏览器
│   ├── FileNode.vue     # 文件节点
│   ├── EditorTabs.vue   # 编辑器标签页
│   ├── PreviewPanel.vue # 预览面板
│   └── TerminalPanel.vue# 终端面板
├── stores/              # 状态管理
│   ├── fileStore.ts     # 文件存储
│   └── webContainerStore.ts # WebContainer 管理
├── views/               # 页面
│   ├── Home.vue         # 首页
│   └── Editor.vue       # 编辑器页面
├── router/              # 路由
└── main.ts              # 入口文件
```

## 使用说明

### 1. 初始化容器

点击"初始化容器"按钮启动 WebContainer 环境。

### 2. 文件操作

- 点击文件浏览器中的"+"按钮创建新文件
- 点击文件名打开文件进行编辑
- 右键文件可进行重命名、删除等操作

### 3. 代码编辑

- 支持多种语言的语法高亮
- 支持代码格式化 (Ctrl+Shift+F)
- 支持文件保存 (Ctrl+S)

### 4. 预览功能

- 点击"运行项目"启动开发服务器
- 在预览面板中查看运行效果
- 支持缩放、刷新等操作

### 5. 终端操作

- 支持多终端标签页
- 完整的命令行环境
- 与 WebContainer 环境集成

## 开发说明

### 添加新的语言支持

在 `CodeEditor.vue` 中的 `getLanguageExtension` 方法中添加新的语言扩展:

```typescript
case 'py':
case 'python':
  return python()
```

### 自定义主题

可以在 CodeMirror 配置中添加自定义主题:

```typescript
import { myCustomTheme } from './themes/custom'

// 在 EditorState.create 中使用
extensions: [
  basicSetup,
  languageExtension,
  myCustomTheme,
  // ...
]
```

## 注意事项

1. **浏览器兼容性**: WebContainer 需要支持 SharedArrayBuffer 的现代浏览器
2. **HTTPS 要求**: 生产环境需要 HTTPS 协议
3. **内存使用**: WebContainer 会占用较多内存，建议在性能较好的设备上使用

## 故障排除

### WebContainer 初始化失败

1. 确保浏览器支持 SharedArrayBuffer
2. 检查是否在 HTTPS 环境下运行
3. 查看浏览器控制台错误信息

### 终端无法连接

1. 确保 WebContainer 已正确初始化
2. 检查网络连接
3. 重新启动容器

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 创建 Pull Request

## 许可证

Apache License 2.0
