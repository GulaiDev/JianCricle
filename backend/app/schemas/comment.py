from datetime import datetime

from pydantic import BaseModel, Field, field_validator

from app.schemas.user import UserBrief


class CommentCreateRequest(BaseModel):
    content: str = Field(
        ...,
        description="评论内容，不能为空，去除首尾空格后最长 500 字",
        examples=["这条帖子很赞！"],
    )

    @field_validator("content")
    @classmethod
    def validate_content(cls, v):
        v = v.strip()
        if not v:
            raise ValueError("评论内容不能为空")
        if len(v) > 500:
            raise ValueError("评论最多500字")
        return v


class CommentResponse(BaseModel):
    id: int
    post_id: int
    content: str
    author: UserBrief
    created_at: datetime


CommentResponse.model_rebuild()
