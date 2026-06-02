package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.jiny.finalalarm.api.model.AlarmEventDto
import com.jiny.finalalarm.api.model.HeartbeatAckDto
import com.jiny.finalalarm.api.model.UnlockRequestDto

interface AlarmEventsApi {
    /**
     * POST api/v1/alarm-events
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param body 
     * @return [AlarmEventDto]
     */
    @POST("api/v1/alarm-events")
    suspend fun eventsControllerCreate(@Body body: kotlin.Any): Response<AlarmEventDto>

    /**
     * POST api/v1/alarm-events/{id}/dismiss
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param id 
     * @param body 
     * @return [AlarmEventDto]
     */
    @POST("api/v1/alarm-events/{id}/dismiss")
    suspend fun eventsControllerDismiss(@Path("id") id: kotlin.String, @Body body: kotlin.Any): Response<AlarmEventDto>

    /**
     * GET api/v1/alarm-events/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @return [AlarmEventDto]
     */
    @GET("api/v1/alarm-events/{id}")
    suspend fun eventsControllerGet(@Path("id") id: kotlin.String): Response<AlarmEventDto>

    /**
     * POST api/v1/alarm-events/{id}/heartbeat
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param id 
     * @param body 
     * @return [HeartbeatAckDto]
     */
    @POST("api/v1/alarm-events/{id}/heartbeat")
    suspend fun eventsControllerHeartbeat(@Path("id") id: kotlin.String, @Body body: kotlin.Any): Response<HeartbeatAckDto>

    /**
     * GET api/v1/alarm-events/history
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param from 
     * @param to 
     * @param limit 
     * @return [kotlin.collections.List<AlarmEventDto>]
     */
    @GET("api/v1/alarm-events/history")
    suspend fun eventsControllerHistory(@Query("from") from: kotlin.String, @Query("to") to: kotlin.String, @Query("limit") limit: java.math.BigDecimal): Response<kotlin.collections.List<AlarmEventDto>>

    /**
     * GET api/v1/alarm-events
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @return [kotlin.collections.List<AlarmEventDto>]
     */
    @GET("api/v1/alarm-events")
    suspend fun eventsControllerListActive(): Response<kotlin.collections.List<AlarmEventDto>>

    /**
     * POST api/v1/alarm-events/{id}/snooze
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param id 
     * @return [AlarmEventDto]
     */
    @POST("api/v1/alarm-events/{id}/snooze")
    suspend fun eventsControllerSnooze(@Path("id") id: kotlin.String): Response<AlarmEventDto>

    /**
     * POST api/v1/alarm-events/{id}/unlock-request
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param id 
     * @return [UnlockRequestDto]
     */
    @POST("api/v1/alarm-events/{id}/unlock-request")
    suspend fun eventsControllerUnlockRequest(@Path("id") id: kotlin.String): Response<UnlockRequestDto>

}
