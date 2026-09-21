import io
import uuid
from datetime import datetime
from pathlib import Path

from PIL import Image

from app.config import settings

EXTENSION_MAP = {
    "image/jpeg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
}


def verify_image(content: bytes) -> bool:
    """使用 Pillow 校验文件内容是否为真实图片（防止伪装扩展名）"""
    try:
        with Image.open(io.BytesIO(content)) as img:
            img.verify()
        return True
    except Exception:
        return False


def save_upload_file(content: bytes, content_type: str) -> tuple[str, str]:
    """
    将图片保存到本地文件系统。
    目录结构：uploads/images/YYYY/MM/DD/uuid.ext
    返回：(可访问URL, 存储文件名)
    """
    ext = EXTENSION_MAP.get(content_type, ".jpg")
    now = datetime.now()
    date_dir = now.strftime("%Y/%m/%d")

    # 创建目录
    save_dir = Path(settings.UPLOAD_DIR) / date_dir
    save_dir.mkdir(parents=True, exist_ok=True)

    # 生成唯一文件名
    file_name = f"{uuid.uuid4().hex}{ext}"
    file_path = save_dir / file_name

    # 写入文件
    with open(file_path, "wb") as f:
        f.write(content)

    # 可访问 URL（通过 StaticFiles 挂载）
    url = f"{settings.STATIC_URL_PREFIX}/images/{date_dir}/{file_name}"

    return url, file_name
