from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database import get_db
from app.core.security import create_access_token
from app.schemas.auth import RegisterRequest, LoginRequest, TokenResponse
from app.schemas.common import ApiResponse
from app.schemas.user import UserBrief
from app.services.auth_service import authenticate, create_user, get_user_by_username

router = APIRouter(prefix="/api/auth", tags=["认证"])


@router.post("/register", response_model=ApiResponse[TokenResponse])
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    """注册：用户名+密码"""
    # 检查用户名是否已存在
    if get_user_by_username(db, req.username):
        return ApiResponse.error(409, "用户名已存在")

    # 创建用户
    user = create_user(db, req.username, req.password, req.nickname)

    # 直接返回 Token（注册即登录）
    token = create_access_token({"sub": str(user.id)})
    return ApiResponse.success(
        TokenResponse(token=token, user=UserBrief.model_validate(user))
    )


@router.post("/login", response_model=ApiResponse[TokenResponse])
def login(req: LoginRequest, db: Session = Depends(get_db)):
    """登录：用户名+密码"""
    user = authenticate(db, req.username, req.password)
    if not user:
        return ApiResponse.error(401, "用户名或密码错误")

    token = create_access_token({"sub": str(user.id)})
    return ApiResponse.success(
        TokenResponse(token=token, user=UserBrief.model_validate(user))
    )
