from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.sql import func

from app.database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(50), unique=True, nullable=False, index=True, comment="用户名")
    password_hash = Column(String(255), nullable=False, comment="bcrypt 哈希密码")
    nickname = Column(String(50), nullable=False, comment="昵称")
    avatar_url = Column(String(500), nullable=True, comment="头像URL")
    bio = Column(String(200), nullable=True, comment="个人简介")
    created_at = Column(DateTime, server_default=func.now(), nullable=False)
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now(), nullable=False)
