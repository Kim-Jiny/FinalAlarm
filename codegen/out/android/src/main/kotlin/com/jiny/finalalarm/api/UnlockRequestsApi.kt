package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.jiny.finalalarm.api.model.UnlockRequestDto

interface UnlockRequestsApi {
    /**
     * POST api/v1/unlock-requests/{id}/approve
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param id 
     * @return [UnlockRequestDto]
     */
    @POST("api/v1/unlock-requests/{id}/approve")
    suspend fun unlockRequestsControllerApprove(@Path("id") id: kotlin.String): Response<UnlockRequestDto>

    /**
     * POST api/v1/unlock-requests/{id}/cancel
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @return [Unit]
     */
    @POST("api/v1/unlock-requests/{id}/cancel")
    suspend fun unlockRequestsControllerCancel(@Path("id") id: kotlin.String): Response<Unit>

    /**
     * GET api/v1/unlock-requests/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @return [UnlockRequestDto]
     */
    @GET("api/v1/unlock-requests/{id}")
    suspend fun unlockRequestsControllerGet(@Path("id") id: kotlin.String): Response<UnlockRequestDto>

    /**
     * GET api/v1/unlock-requests/inbox
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param teamId 
     * @param status 
     * @return [kotlin.collections.List<UnlockRequestDto>]
     */
    @GET("api/v1/unlock-requests/inbox")
    suspend fun unlockRequestsControllerInbox(@Query("teamId") teamId: kotlin.String, @Query("status") status: kotlin.String): Response<kotlin.collections.List<UnlockRequestDto>>

}
