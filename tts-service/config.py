from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    host: str = "0.0.0.0"
    port: int = 8000
    cors_origins: str = "*"

    class Config:
        env_file = ".env"
        env_prefix = "TTS_"

settings = Settings()
