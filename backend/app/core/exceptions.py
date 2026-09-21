from fastapi import Request
from fastapi.responses import JSONResponse


async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={
            "code": 500,
            "message": f"服务器内部错误: {str(exc)}",
            "data": None,
        },
    )
