package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.jiny.finalalarm.api.model.AuthResponseDto

interface AuthApi {
    /**
     * POST api/v1/auth/login
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param body 
     * @return [AuthResponseDto]
     */
    @POST("api/v1/auth/login")
    suspend fun authControllerLogin(@Body body: kotlin.Any): Response<AuthResponseDto>

    /**
     * POST api/v1/auth/logout
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param body 
     * @return [Unit]
     */
    @POST("api/v1/auth/logout")
    suspend fun authControllerLogout(@Body body: kotlin.Any): Response<Unit>

    /**
     * POST api/v1/auth/refresh
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param body 
     * @return [AuthResponseDto]
     */
    @POST("api/v1/auth/refresh")
    suspend fun authControllerRefresh(@Body body: kotlin.Any): Response<AuthResponseDto>

    /**
     * POST api/v1/auth/signup
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param body 
     * @return [AuthResponseDto]
     */
    @POST("api/v1/auth/signup")
    suspend fun authControllerSignup(@Body body: kotlin.Any): Response<AuthResponseDto>

}
