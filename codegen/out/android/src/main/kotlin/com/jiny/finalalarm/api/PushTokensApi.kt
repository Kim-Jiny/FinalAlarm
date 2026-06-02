package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.jiny.finalalarm.api.model.PushTokenDto

interface PushTokensApi {
    /**
     * POST api/v1/push-tokens
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param body 
     * @return [PushTokenDto]
     */
    @POST("api/v1/push-tokens")
    suspend fun pushTokensControllerRegister(@Body body: kotlin.Any): Response<PushTokenDto>

    /**
     * DELETE api/v1/push-tokens/{id}
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @return [Unit]
     */
    @DELETE("api/v1/push-tokens/{id}")
    suspend fun pushTokensControllerRemove(@Path("id") id: kotlin.String): Response<Unit>

}
