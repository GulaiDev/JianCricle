from fastapi import APIRouter

from app.api import auth, upload, posts, comments, user

api_router = APIRouter()
api_router.include_router(auth.router)
api_router.include_router(upload.router)
api_router.include_router(posts.router)
api_router.include_router(comments.router)
api_router.include_router(user.router)
