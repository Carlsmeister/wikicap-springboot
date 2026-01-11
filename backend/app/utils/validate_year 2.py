
from fastapi import HTTPException, status
def validate_year(year: int):
    if year < 1800 or year > 2027:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="BAD REQUEST: Year must be between 1800 and 2027"
        )