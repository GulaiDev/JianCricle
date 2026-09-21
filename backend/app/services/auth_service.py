from sqlalchemy.orm import Session

from app.core.security import hash_password, verify_password
from app.models.user import User


def get_user_by_username(db: Session, username: str) -> User | None:
    return db.query(User).filter(User.username == username).first()


def create_user(db: Session, username: str, password: str, nickname: str) -> User:
    """创建用户（调用前需确认用户名未被占用）"""
    user = User(
        username=username,
        password_hash=hash_password(password),
        nickname=nickname,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def authenticate(db: Session, username: str, password: str) -> User | None:
    """校验用户名+密码，成功返回用户，失败返回 None"""
    user = get_user_by_username(db, username)
    if not user or not verify_password(password, user.password_hash):
        return None
    return user
