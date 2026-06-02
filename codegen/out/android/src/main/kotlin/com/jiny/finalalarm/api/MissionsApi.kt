package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.jiny.finalalarm.api.model.MissionDto

interface MissionsApi {
    /**
     * POST api/v1/missions
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param body 
     * @return [MissionDto]
     */
    @POST("api/v1/missions")
    suspend fun missionsControllerCreate(@Body body: kotlin.Any): Response<MissionDto>

    /**
     * GET api/v1/missions/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @return [MissionDto]
     */
    @GET("api/v1/missions/{id}")
    suspend fun missionsControllerGet(@Path("id") id: kotlin.String): Response<MissionDto>

    /**
     * GET api/v1/missions
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @return [kotlin.collections.List<MissionDto>]
     */
    @GET("api/v1/missions")
    suspend fun missionsControllerList(): Response<kotlin.collections.List<MissionDto>>

    /**
     * DELETE api/v1/missions/{id}
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @return [Unit]
     */
    @DELETE("api/v1/missions/{id}")
    suspend fun missionsControllerRemove(@Path("id") id: kotlin.String): Response<Unit>

    /**
     * PATCH api/v1/missions/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @param body 
     * @return [MissionDto]
     */
    @PATCH("api/v1/missions/{id}")
    suspend fun missionsControllerUpdate(@Path("id") id: kotlin.String, @Body body: kotlin.Any): Response<MissionDto>

}
