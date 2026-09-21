from datetime import datetime

from pydantic import BaseModel, field_validator

from app.schemas.user import UserBrief


class PostCreateRequest(BaseModel):
    content: str
    image_urls: list[str] = []

    @field_validator("content")
    @classmethod
    def validate_content(cls, v):
        v = v.strip()
        if len(v) > 500:
            raise ValueError("内容最多500字")
        return v


class PostResponse(BaseModel):
    id: int
    content: str
    image_urls: list[str] = []
    like_count: int
    comment_count: int
    liked: bool = False
    author: UserBrief | None = None
    created_at: datetime


class LikeResult(BaseModel):
    liked: bool
    like_count: int


PostResponse.model_rebuild()
