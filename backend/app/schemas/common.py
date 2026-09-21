from typing import Generic, Optional, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    code: int = 200
    message: str = "success"
    data: Optional[T] = None

    @classmethod
    def success(cls, data: T = None, message: str = "success") -> "ApiResponse[T]":
        return cls(code=200, message=message, data=data)

    @classmethod
    def error(cls, code: int, message: str) -> "ApiResponse":
        return cls(code=code, message=message, data=None)


class PageResponse(BaseModel, Generic[T]):
    items: list[T]
    total: int
    page: int
    size: int
    has_more: bool
