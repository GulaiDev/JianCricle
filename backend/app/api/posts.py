from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.config import settings
from app.database import get_db
from app.core.deps import get_current_user, get_current_user_optional
from app.models.user import User
from app.models.post import Post, PostLike
from app.schemas.post import PostCreateRequest, PostResponse, LikeResult
from app.schemas.common import ApiResponse, PageResponse
from app.services.post_service import build_post_response

router = APIRouter(prefix="/api/posts", tags=["帖子"])


@router.post("", response_model=ApiResponse[PostResponse])
def create_post(
    req: PostCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """发布帖子"""
    if not req.content.strip() and not req.image_urls:
        return ApiResponse.error(400, "内容和图片至少有一项")

    if len(req.image_urls) > settings.MAX_ATTACHMENTS_PER_POST:
        return ApiResponse.error(
            400, f"最多{settings.MAX_ATTACHMENTS_PER_POST}张图片"
        )

    post = Post(
        user_id=current_user.id,
        content=req.content,
        image_urls=req.image_urls,
    )
    db.add(post)
    db.commit()
    db.refresh(post)

    return ApiResponse.success(build_post_response(post, current_user.id, db))


@router.get("/feed", response_model=ApiResponse[PageResponse[PostResponse]])
def get_feed(
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=50),
    keyword: str | None = Query(None),
    current_user: User | None = Depends(get_current_user_optional),
    db: Session = Depends(get_db),
):
    """首页信息流"""
    query = db.query(Post).filter(Post.status == 1)
    if keyword:
        query = query.filter(Post.content.contains(keyword))
    total = query.count()
    posts = (
        query.order_by(Post.created_at.desc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )
    user_id = current_user.id if current_user else None
    items = [build_post_response(p, user_id, db) for p in posts]
    return ApiResponse.success(
        PageResponse(
            items=items,
            total=total,
            page=page,
            size=size,
            has_more=(page * size) < total,
        )
    )


@router.get("/mine", response_model=ApiResponse[PageResponse[PostResponse]])
def get_my_posts(
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=50),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """我的帖子"""
    query = db.query(Post).filter(
        Post.user_id == current_user.id, Post.status == 1
    )
    total = query.count()
    posts = (
        query.order_by(Post.created_at.desc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )
    items = [build_post_response(p, current_user.id, db) for p in posts]
    return ApiResponse.success(
        PageResponse(
            items=items,
            total=total,
            page=page,
            size=size,
            has_more=(page * size) < total,
        )
    )


@router.get("/{post_id}", response_model=ApiResponse[PostResponse])
def get_post_detail(
    post_id: int,
    current_user: User | None = Depends(get_current_user_optional),
    db: Session = Depends(get_db),
):
    """帖子详情"""
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return ApiResponse.error(404, "帖子不存在")
    user_id = current_user.id if current_user else None
    return ApiResponse.success(build_post_response(post, user_id, db))


@router.post("/{post_id}/like", response_model=ApiResponse[LikeResult])
def toggle_like(
    post_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """点赞/取消点赞"""
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return ApiResponse.error(404, "帖子不存在")

    existing = (
        db.query(PostLike)
        .filter(
            PostLike.post_id == post_id,
            PostLike.user_id == current_user.id,
        )
        .first()
    )

    if existing:
        db.delete(existing)
        post.like_count = max(0, post.like_count - 1)
        db.commit()
        return ApiResponse.success(LikeResult(liked=False, like_count=post.like_count))

    like = PostLike(post_id=post_id, user_id=current_user.id)
    db.add(like)
    post.like_count += 1
    db.commit()
    return ApiResponse.success(LikeResult(liked=True, like_count=post.like_count))
