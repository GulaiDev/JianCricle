from fastapi import APIRouter, Depends

from app.database import get_db  # noqa: F401  (保留以与文档结构一致)
from app.core.deps import get_current_user
from app.models.user import User
from app.schemas.user import UserBrief
from app.schemas.common import ApiResponse

router = APIRouter(prefix="/api/user", tags=["用户"])


@router.get("/profile", response_model=ApiResponse[UserBrief])
def get_profile(current_user: User = Depends(get_current_user)):
    """获取当前登录用户信息"""
    return ApiResponse.success(UserBrief.model_validate(current_user))
