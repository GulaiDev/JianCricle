from pydantic import BaseModel


class UploadResponse(BaseModel):
    url: str  # 本地文件访问 URL，如 /static/images/2026/09/20/xxx.jpg
    file_name: str  # 存储文件名
    file_size: int
