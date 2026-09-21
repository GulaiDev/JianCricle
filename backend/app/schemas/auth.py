import re

from pydantic import BaseModel, field_validator

from app.schemas.user import UserBrief


class RegisterRequest(BaseModel):
    username: str  # 用户名
    password: str  # 密码
    nickname: str  # 昵称

    @field_validator("username")
    @classmethod
    def validate_username(cls, v):
        if not re.match(r"^[a-zA-Z0-9_]{3,20}$", v):
            raise ValueError("用户名3-20位，仅含字母数字下划线")
        return v

    @field_validator("password")
    @classmethod
    def validate_password(cls, v):
        if len(v) < 6 or len(v) > 20:
            raise ValueError("密码长度6-20位")
        return v

    @field_validator("nickname")
    @classmethod
    def validate_nickname(cls, v):
        v = v.strip()
        if not v:
            raise ValueError("昵称不能为空")
        if len(v) > 50:
            raise ValueError("昵称最多50字")
        return v


class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    token: str
    token_type: str = "bearer"
    user: UserBrief


TokenResponse.model_rebuild()
