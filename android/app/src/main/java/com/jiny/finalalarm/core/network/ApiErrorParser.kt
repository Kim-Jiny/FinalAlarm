package com.jiny.finalalarm.core.network

import com.jiny.finalalarm.data.api.ApiError
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val json = Json { ignoreUnknownKeys = true }

/**
 * API 호출에서 발생한 예외의 사용자용 메시지를 반환.
 * - HttpException 이면 응답 body의 `error.message`를 파싱
 * - 그 외면 throwable.message ?: "알 수 없는 오류"
 */
fun Throwable.userMessage(): String {
    if (this is HttpException) {
        val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
        if (!raw.isNullOrBlank()) {
            runCatching {
                val parsed = json.decodeFromString(ApiError.serializer(), raw)
                return parsed.error.message.ifBlank { codeFallback(parsed.error.code) }
            }
        }
        return "서버 오류 (${code()})"
    }
    return message ?: "알 수 없는 오류"
}

/**
 * 에러 코드만 받고 사용자용 한국어 메시지로 변환.
 */
fun Throwable.errorCode(): String? {
    if (this !is HttpException) return null
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull() ?: return null
    return runCatching { json.decodeFromString(ApiError.serializer(), raw).error.code }.getOrNull()
}

private fun codeFallback(code: String): String = when (code) {
    "UNAUTHORIZED" -> "로그인이 필요합니다"
    "FORBIDDEN" -> "권한이 없습니다"
    "NOT_FOUND" -> "찾을 수 없습니다"
    "CONFLICT" -> "충돌 — 다른 곳에서 사용 중일 수 있어요"
    "TEAM_NOT_MEMBER" -> "팀 멤버가 아닙니다"
    "INVITE_EXPIRED" -> "초대가 만료됐어요"
    "INVITE_INVALID" -> "유효하지 않은 초대"
    "WINDOW_NOT_ACTIVE" -> "현재 활성 시간대가 아닙니다"
    "REQUEST_EXPIRED" -> "요청이 만료됐어요"
    "REQUEST_INVALID_STATE" -> "요청 상태가 올바르지 않습니다"
    "EVENT_INVALID_STATE" -> "현재 상태에서 할 수 없어요"
    "MISSION_FAILED" -> "미션 실패"
    "RATE_LIMITED" -> "너무 빠릅니다. 잠시 후 다시 시도해주세요"
    else -> "오류 ($code)"
}
