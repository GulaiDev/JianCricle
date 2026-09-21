from sqlalchemy.orm import Session

from app.models.post import Post, PostLike
from app.models.user import User
from app.schemas.auth import UserBrief
from app.schemas.post import PostResponse


def build_post_response(
    post: Post, current_user_id: int | None, db: Session
) -> PostResponse:
    """组装帖子响应，包含作者信息和点赞状态"""
    author = db.query(User).filter(User.id == post.user_id).first()
    liked = False
    if current_user_id:
        liked = (
            db.query(PostLike)
            .filter(
                PostLike.post_id == post.id,
                PostLike.user_id == current_user_id,
            )
            .first()
            is not None
        )

    return PostResponse(
        id=post.id,
        content=post.content,
        image_urls=post.image_urls or [],
        like_count=post.like_count,
        comment_count=post.comment_count,
        liked=liked,
        author=UserBrief.model_validate(author) if author else None,
        created_at=post.created_at,
    )
