from pydantic import BaseModel, ConfigDict


class UserBrief(BaseModel):
    id: int
    username: str
    nickname: str
    avatar_url: str | None = None
    bio: str | None = None

    model_config = ConfigDict(from_attributes=True)
