package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.jiny.finalalarm.api.model.InviteDto
import com.jiny.finalalarm.api.model.InvitePreviewDto
import com.jiny.finalalarm.api.model.RedeemInviteDto

interface InvitesApi {
    /**
     * POST api/v1/teams/{id}/invites
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param id 
     * @param body 
     * @return [InviteDto]
     */
    @POST("api/v1/teams/{id}/invites")
    suspend fun invitesControllerCreate(@Path("id") id: kotlin.String, @Body body: kotlin.Any): Response<InviteDto>

    /**
     * GET api/v1/teams/{id}/invites
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @return [kotlin.collections.List<InviteDto>]
     */
    @GET("api/v1/teams/{id}/invites")
    suspend fun invitesControllerList(@Path("id") id: kotlin.String): Response<kotlin.collections.List<InviteDto>>

    /**
     * GET api/v1/team-invites/{code}/preview
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param code 
     * @return [InvitePreviewDto]
     */
    @GET("api/v1/team-invites/{code}/preview")
    suspend fun invitesControllerPreview(@Path("code") code: kotlin.String): Response<InvitePreviewDto>

    /**
     * POST api/v1/team-invites/{code}/redeem
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param code 
     * @return [RedeemInviteDto]
     */
    @POST("api/v1/team-invites/{code}/redeem")
    suspend fun invitesControllerRedeem(@Path("code") code: kotlin.String): Response<RedeemInviteDto>

    /**
     * DELETE api/v1/teams/{id}/invites/{inviteId}
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @param inviteId 
     * @return [Unit]
     */
    @DELETE("api/v1/teams/{id}/invites/{inviteId}")
    suspend fun invitesControllerRevoke(@Path("id") id: kotlin.String, @Path("inviteId") inviteId: kotlin.String): Response<Unit>

}
