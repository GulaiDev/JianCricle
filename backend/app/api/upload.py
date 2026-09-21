from fastapi import APIRouter, Depends, UploadFile, File
from sqlalchemy.orm import Session

from app.database import get_db
from app.config import settings
from app.core.deps import get_current_user
from app.models.user import User
from app.models.upload_record import UploadRecord
from app.schemas.upload import UploadResponse
from app.schemas.common import ApiResponse
from app.services.upload_service import save_upload_file, verify_image

router = APIRouter(prefix="/api/upload", tags=["上传"])


@router.post("/image", response_model=ApiResponse[UploadResponse])
async def upload_image(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """上传图片到本地文件系统"""
    # 1. 校验文件类型
    if file.content_type not in settings.ALLOWED_IMAGE_TYPES:
        return ApiResponse.error(415, "仅支持图片格式(jpg/png/webp)")

    # 2. 读取文件内容并校验大小
    content = await file.read()
    if len(content) > settings.MAX_FILE_SIZE:
        return ApiResponse.error(413, "文件大小超过限制(20MB)")

    # 3. 校验文件内容确为图片
    if not verify_image(content):
        return ApiResponse.error(415, "图片内容校验失败，文件已损坏或格式不支持")

    # 4. 保存到本地
    file_url, stored_name = save_upload_file(content, file.content_type)

    # 5. 记录上传日志
    record = UploadRecord(
        user_id=current_user.id,
        file_name=stored_name,
        file_url=file_url,
        file_size=len(content),
    )
    db.add(record)
    db.commit()

    # 6. 返回访问 URL
    return ApiResponse.success(
        UploadResponse(
            url=file_url,
            file_name=stored_name,
            file_size=len(content),
        )
    )
