# 简圈（JianCircle）

一个前后端分离的轻量社交圈子应用：用户可以注册登录、发布图文帖子、浏览首页信息流、关键词搜索、点赞与评论，并管理个人主页。

- **后端**：Python + FastAPI + SQLite，提供 REST API 与图片静态服务
- **前端**：Android 原生，Kotlin + Jetpack Compose 单模块应用

## 功能特性

- 账号注册 / 登录（JWT 鉴态，Token 有效期 7 天）
- 首页信息流分页加载、关键词搜索（输入防抖）、下拉刷新
- 发布图文帖子：支持拍照 / 相册选图（最多 9 张、单张 20MB），客户端图片压缩与上传进度展示
- 帖子详情：大图预览（左右滑动）、点赞 / 取消点赞、评论分页与发表
- 个人主页：查看个人资料与我的帖子、清除缓存、退出登录
- 启动页自动校验本地 Token 并决定跳转登录页还是首页

## 技术栈

### 后端（`backend/`）

| 分类 | 选型 |
| --- | --- |
| Web 框架 | FastAPI 0.115 + Uvicorn 0.30 |
| ORM / 数据库 | SQLAlchemy 2.0 + SQLite（启动时自动建表，无迁移工具） |
| 数据校验 | Pydantic 2 / pydantic-settings |
| 鉴权 | python-jose（JWT, HS256）+ passlib / bcrypt |
| 文件 | python-multipart、aiofiles、Pillow |

依赖清单见 [backend/requirements.txt](backend/requirements.txt)。

### 前端（`frontend/`）

| 分类 | 选型 |
| --- | --- |
| 语言 / 构建 | Kotlin 2.2.10、Gradle 9.3.1、AGP 9.1.1（JDK 17+ 运行，字节码兼容 Java 11） |
| UI | Jetpack Compose（BOM 2026.06.00）、Material3 |
| 架构 | 单 Activity + Navigation Compose，MVVM（ViewModel + StateFlow） |
| 依赖注入 | Hilt 2.60（KSP） |
| 网络 | Retrofit 2.9 + OkHttp 4.12 + kotlinx.serialization |
| 图片 | Coil 2.7 |
| 本地存储 | DataStore Preferences（Token） |

版本统一定义在 [frontend/gradle/libs.versions.toml](frontend/gradle/libs.versions.toml)；`minSdk = 24`、`targetSdk/compileSdk = 36`。

## 项目结构

```
jiancircle/
├── backend/
│   ├── app/
│   │   ├── api/          # 路由：auth / user / posts / comments / upload
│   │   ├── core/         # 依赖注入、安全（JWT、密码哈希）、全局异常
│   │   ├── models/       # SQLAlchemy 模型
│   │   ├── schemas/      # Pydantic 请求/响应模型
│   │   ├── services/     # 业务逻辑层
│   │   ├── config.py     # 配置（读取 .env）
│   │   ├── database.py   # 引擎与会话
│   │   └── main.py       # 应用入口
│   ├── uploads/          # 上传图片的本地存储与静态服务目录（运行时生成）
│   ├── requirements.txt
│   └── .env              # 本地环境变量（不入库）
└── frontend/
    └── app/src/main/java/com/rdbb/jiancircle/
        ├── data/         # model / remote(Retrofit) / repository / local(Token)
        ├── di/           # Hilt 网络模块
        ├── ui/
        │   ├── navigation/
        │   ├── screen/   # splash / login / home / pulish / detail / mine / main
        │   └── theme/
        ├── util/         # 时间、文件、图片压缩、权限工具
        └── MainActivity.kt
```

## 快速开始

### 1. 启动后端

要求 Python 3.10+（仓库 `backend/venv` 为 Windows 下 Python 3.12 的虚拟环境）。

```bash
cd backend
python -m venv venv
# Windows
venv\Scripts\activate
# macOS / Linux
source venv/bin/activate

pip install -r requirements.txt
python app/main.py
```

服务监听 `0.0.0.0:8000`，`DEBUG=True` 时自动热重载：

- 健康检查：http://localhost:8000/
- 接口文档（Swagger）：http://localhost:8000/docs
- 上传文件访问前缀：`/static/...`

首次启动会在 `backend/` 下自动创建 SQLite 数据库 `jianquan.db` 并建表。

### 2. 启动前端

1. 用 Android Studio 打开 `frontend/` 目录，等待 Gradle 同步；或命令行执行：

   ```bash
   cd frontend
   ./gradlew :app:assembleDebug
   ./gradlew :app:installDebug
   ```

2. 配置后端地址。后端基础地址通过 `frontend/local.properties`（已被 `.gitignore` 忽略，不入库）中的 `BASE_URL` 注入 BuildConfig，地址必须以 `/` 结尾：

   ```properties
   # 真机调试（与电脑同一局域网）：填写电脑的局域网 IP
   BASE_URL=http://192.168.x.x:8000/
   ```

   - **Android 模拟器**访问宿主机后端：不配置即可，默认值为 `http://10.0.2.2:8000/`
   - 应用已开启明文 HTTP 流量（`usesCleartextTraffic`），仅建议开发环境使用

## API 一览

所有接口统一以 `/api` 开头，业务响应包裹在 `ApiResponse<T>` 中，分页接口返回 `PageResponse<T>`。需登录的接口通过 `Authorization: Bearer <token>` 鉴权。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/user/profile` | 当前用户资料 |
| POST | `/api/upload/image` | 上传图片（multipart，字段名 `file`） |
| POST | `/api/posts` | 发布帖子 |
| GET | `/api/posts/feed` | 信息流（`page`、`size`、`keyword`） |
| GET | `/api/posts/mine` | 我的帖子（分页） |
| GET | `/api/posts/{post_id}` | 帖子详情 |
| POST | `/api/posts/{post_id}/like` | 点赞 / 取消点赞（切换） |
| GET | `/api/posts/{post_id}/comments` | 评论列表（分页） |
| POST | `/api/posts/{post_id}/comments` | 发表评论 |

## 数据模型（SQLite）

- **users**：用户（用户名唯一、bcrypt 密码哈希、昵称、头像、简介）
- **posts**：帖子（正文、图片 URL JSON 数组、点赞/评论计数、状态、时间）
- **post_likes**：点赞关系（`post_id + user_id` 唯一约束）
- **comments**：评论（帖子、用户、内容、时间）

表结构以 [backend/app/models/](backend/app/models) 下的模型定义为唯一事实来源。

## 配置说明

后端配置项见 [backend/app/config.py](backend/app/config.py)，可通过 `backend/.env` 覆盖，主要包括：

- `DATABASE_URL`：默认 SQLite 本地文件
- `SECRET_KEY` / `ALGORITHM` / `ACCESS_TOKEN_EXPIRE_MINUTES`：JWT 配置
- `UPLOAD_DIR`、`MAX_FILE_SIZE`、`ALLOWED_IMAGE_TYPES`、`MAX_ATTACHMENTS_PER_POST`：上传限制

> 安全提示：默认 `SECRET_KEY` 仅用于本地开发，部署前务必通过 `.env` 替换为独立密钥；CORS 当前为 `allow_origins=["*"]`，生产环境应收敛为前端域名。

## 开发约定

- 时间工具仅使用 API 24 可用 API，不依赖 `java.time` desugaring
- 网络层错误在 Repository 中统一转换为失败状态，不向 UI 抛出异常
- Compose 页面全面启用 edge-to-edge，自定义顶栏使用 `statusBarsPadding()`，主 Activity 配置 `windowSoftInputMode="adjustResize"`
