package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.jiny.finalalarm.api.model.AlarmDto

interface AlarmsApi {
    /**
     * POST api/v1/alarms
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param body 
     * @return [AlarmDto]
     */
    @POST("api/v1/alarms")
    suspend fun alarmsControllerCreate(@Body body: kotlin.Any): Response<AlarmDto>

    /**
     * GET api/v1/alarms/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @return [AlarmDto]
     */
    @GET("api/v1/alarms/{id}")
    suspend fun alarmsControllerGet(@Path("id") id: kotlin.String): Response<AlarmDto>

    /**
     * GET api/v1/alarms
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param teamId 
     * @param kind 
     * @param active 
     * @return [kotlin.collections.List<AlarmDto>]
     */
    @GET("api/v1/alarms")
    suspend fun alarmsControllerList(@Query("teamId") teamId: kotlin.String, @Query("kind") kind: kotlin.String, @Query("active") active: kotlin.Boolean): Response<kotlin.collections.List<AlarmDto>>

    /**
     * DELETE api/v1/alarms/{id}
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @return [Unit]
     */
    @DELETE("api/v1/alarms/{id}")
    suspend fun alarmsControllerRemove(@Path("id") id: kotlin.String): Response<Unit>

    /**
     * PATCH api/v1/alarms/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @param body 
     * @return [AlarmDto]
     */
    @PATCH("api/v1/alarms/{id}")
    suspend fun alarmsControllerUpdate(@Path("id") id: kotlin.String, @Body body: kotlin.Any): Response<AlarmDto>

}
