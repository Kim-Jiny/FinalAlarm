package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.jiny.finalalarm.api.model.AlarmEventDto

interface PushAlarmApi {
    /**
     * POST api/v1/push-alarm
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param body 
     * @return [AlarmEventDto]
     */
    @POST("api/v1/push-alarm")
    suspend fun pushAlarmControllerPush(@Body body: kotlin.Any): Response<AlarmEventDto>

}
