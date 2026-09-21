from sqlalchemy.orm import Session

from app.models.comment import Comment
from app.models.post import Post


def create_comment(db: Session, post: Post, user_id: int, content: str) -> Comment:
    """发表评论并递增帖子的评论计数"""
    comment = Comment(
        post_id=post.id,
        user_id=user_id,
        content=content,
    )
    db.add(comment)
    post.comment_count += 1
    db.commit()
    db.refresh(comment)
    return comment
