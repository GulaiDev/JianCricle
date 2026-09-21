from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.sql import func

from app.database import Base


class UploadRecord(Base):
    __tablename__ = "upload_records"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    file_name = Column(String(200), nullable=False)
    file_url = Column(String(500), nullable=False)
    file_size = Column(Integer, nullable=False)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)
