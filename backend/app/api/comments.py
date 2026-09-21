from typing import Annotated

from fastapi import APIRouter, Depends, Path, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.core.deps import get_current_user
from app.models.user import User
from app.models.post import Post
from app.models.comment import Comment
from app.schemas.comment import CommentCreateRequest, CommentResponse
from app.schemas.user import UserBrief
from app.schemas.common import ApiResponse, PageResponse
from app.services.comment_service import create_comment

router = APIRouter(prefix="/api/posts/{post_id}/comments", tags=["评论"])


@router.get("", response_model=ApiResponse[PageResponse[CommentResponse]])
def get_comments(
    post_id: Annotated[int, Path(description="帖子 ID")],
    page: int = Query(1, ge=1, description="页码，从 1 开始"),
    size: int = Query(20, ge=1, le=50, description="每页条数，取值范围 1-50"),
    db: Session = Depends(get_db),
):
    """获取评论列表（按发布时间倒序分页返回）"""
    query = db.query(Comment).filter(Comment.post_id == post_id)
    total = query.count()
    comments = (
        query.order_by(Comment.created_at.desc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )

    items = []
    for c in comments:
        author = db.query(User).filter(User.id == c.user_id).first()
        items.append(
            CommentResponse(
                id=c.id,
                post_id=c.post_id,
                content=c.content,
                author=UserBrief.model_validate(author),
                created_at=c.created_at,
            )
        )

    return ApiResponse.success(
        PageResponse(
            items=items,
            total=total,
            page=page,
            size=size,
            has_more=(page * size) < total,
        )
    )


@router.post("", response_model=ApiResponse[CommentResponse])
def post_comment(
    post_id: Annotated[int, Path(description="帖子 ID")],
    req: CommentCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """发表评论（需登录）"""
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return ApiResponse.error(404, "帖子不存在")

    comment = create_comment(db, post, current_user.id, req.content)

    return ApiResponse.success(
        CommentResponse(
            id=comment.id,
            post_id=comment.post_id,
            content=comment.content,
            author=UserBrief.model_validate(current_user),
            created_at=comment.created_at,
        )
    )
