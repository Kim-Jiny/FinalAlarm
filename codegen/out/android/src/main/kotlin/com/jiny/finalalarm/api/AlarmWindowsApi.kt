package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.jiny.finalalarm.api.model.AlarmWindowDto

interface AlarmWindowsApi {
    /**
     * POST api/v1/alarm-windows
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param body 
     * @return [AlarmWindowDto]
     */
    @POST("api/v1/alarm-windows")
    suspend fun windowsControllerCreate(@Body body: kotlin.Any): Response<AlarmWindowDto>

    /**
     * GET api/v1/alarm-windows
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param teamId 
     * @param userId 
     * @return [kotlin.collections.List<AlarmWindowDto>]
     */
    @GET("api/v1/alarm-windows")
    suspend fun windowsControllerList(@Query("teamId") teamId: kotlin.String, @Query("userId") userId: kotlin.String): Response<kotlin.collections.List<AlarmWindowDto>>

    /**
     * DELETE api/v1/alarm-windows/{id}
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @return [Unit]
     */
    @DELETE("api/v1/alarm-windows/{id}")
    suspend fun windowsControllerRemove(@Path("id") id: kotlin.String): Response<Unit>

    /**
     * PATCH api/v1/alarm-windows/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @param body 
     * @return [AlarmWindowDto]
     */
    @PATCH("api/v1/alarm-windows/{id}")
    suspend fun windowsControllerUpdate(@Path("id") id: kotlin.String, @Body body: kotlin.Any): Response<AlarmWindowDto>

}
