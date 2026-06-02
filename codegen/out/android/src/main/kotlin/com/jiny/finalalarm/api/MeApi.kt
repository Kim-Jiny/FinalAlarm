package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.jiny.finalalarm.api.model.UserDto

interface MeApi {
    /**
     * POST api/v1/me/password
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param body 
     * @return [Unit]
     */
    @POST("api/v1/me/password")
    suspend fun meControllerChangePassword(@Body body: kotlin.Any): Response<Unit>

    /**
     * DELETE api/v1/me
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @return [Unit]
     */
    @DELETE("api/v1/me")
    suspend fun meControllerDeleteMe(): Response<Unit>

    /**
     * GET api/v1/me
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @return [UserDto]
     */
    @GET("api/v1/me")
    suspend fun meControllerGetMe(): Response<UserDto>

    /**
     * PATCH api/v1/me
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param body 
     * @return [UserDto]
     */
    @PATCH("api/v1/me")
    suspend fun meControllerUpdateMe(@Body body: kotlin.Any): Response<UserDto>

}
