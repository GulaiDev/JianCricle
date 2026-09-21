import os
import sys
from pathlib import Path

# 项目根目录（app/ 的上一级）
PROJECT_ROOT = Path(__file__).resolve().parent.parent

# 兼容直接运行本文件（IDE 运行 main.py、python app/main.py、双击 main.py）：
# 此时脚本目录是 app/ 且工作目录可能不是项目根目录，
# 需补入包搜索路径并切换工作目录，保证 app 包、.env、uploads、SQLite 路径均正确
if __package__ in (None, ""):
    sys.path.insert(0, str(PROJECT_ROOT))
    os.chdir(PROJECT_ROOT)

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import Base, engine
from app.core.exceptions import global_exception_handler
from app.api.router import api_router

# 导入所有模型，确保 Base.metadata 能收集到全部表定义
import app.models  # noqa: F401

# StaticFiles 挂载前必须确保本地目录存在（锚定到项目根目录，与工作目录无关）
STATIC_ROOT = PROJECT_ROOT / "uploads"
STATIC_ROOT.mkdir(parents=True, exist_ok=True)
(STATIC_ROOT / "images").mkdir(parents=True, exist_ok=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时自动建表（SQLite 开发环境，不使用 Alembic）
    Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(
    title=settings.APP_NAME,
    version="2.0.0",
    lifespan=lifespan,
)

# 全局异常处理（统一响应格式）
app.add_exception_handler(Exception, global_exception_handler)

# CORS 配置（允许前端跨域）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境改为前端域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载静态文件服务（图片访问）
app.mount(
    settings.STATIC_URL_PREFIX,
    StaticFiles(directory=str(STATIC_ROOT)),
    name="static",
)

# 注册路由
app.include_router(api_router)


@app.get("/")
def health_check():
    return {"status": "ok", "app": settings.APP_NAME}


if __name__ == "__main__":
    import uvicorn

    # 直接运行 main.py 即启动项目；DEBUG=True 时开启热重载（与 --reload 等价）
    # 传入导入字符串 "app.main:app"，reload 子进程会以模块方式重新导入，不会重复执行本块
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.DEBUG,
    )
