from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # 应用配置
    APP_NAME: str = "简圈 API"
    DEBUG: bool = True

    # 数据库（SQLite，驱动内置于 Python 标准库，无需额外依赖）
    DATABASE_URL: str = "sqlite:///./jianquan.db"

    # JWT 配置
    SECRET_KEY: str = "your-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7天

    # 图片上传配置
    UPLOAD_DIR: str = "uploads/images"
    MAX_FILE_SIZE: int = 20 * 1024 * 1024  # 20MB
    ALLOWED_IMAGE_TYPES: list[str] = ["image/jpeg", "image/png", "image/webp"]
    MAX_ATTACHMENTS_PER_POST: int = 9

    # 静态文件访问路径前缀
    STATIC_URL_PREFIX: str = "/static"

    class Config:
        env_file = ".env"


settings = Settings()
