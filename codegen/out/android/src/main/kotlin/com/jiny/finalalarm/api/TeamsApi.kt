package com.jiny.finalalarm.api

import com.jiny.finalalarm.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.jiny.finalalarm.api.model.TeamDto
import com.jiny.finalalarm.api.model.TeamMemberDto
import com.jiny.finalalarm.api.model.TeamSummaryDto

interface TeamsApi {
    /**
     * PATCH api/v1/teams/{id}/members/{userId}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @param userId 
     * @param body 
     * @return [TeamMemberDto]
     */
    @PATCH("api/v1/teams/{id}/members/{userId}")
    suspend fun teamsControllerChangeRole(@Path("id") id: kotlin.String, @Path("userId") userId: kotlin.String, @Body body: kotlin.Any): Response<TeamMemberDto>

    /**
     * POST api/v1/teams
     * 
     * 
     * Responses:
     *  - 200: 
     *  - 201: 
     *
     * @param body 
     * @return [TeamDto]
     */
    @POST("api/v1/teams")
    suspend fun teamsControllerCreate(@Body body: kotlin.Any): Response<TeamDto>

    /**
     * GET api/v1/teams/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @return [TeamDto]
     */
    @GET("api/v1/teams/{id}")
    suspend fun teamsControllerGet(@Path("id") id: kotlin.String): Response<TeamDto>

    /**
     * DELETE api/v1/teams/{id}/members/{userId}
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @param userId 
     * @return [Unit]
     */
    @DELETE("api/v1/teams/{id}/members/{userId}")
    suspend fun teamsControllerKick(@Path("id") id: kotlin.String, @Path("userId") userId: kotlin.String): Response<Unit>

    /**
     * DELETE api/v1/teams/{id}/members/me
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @return [Unit]
     */
    @DELETE("api/v1/teams/{id}/members/me")
    suspend fun teamsControllerLeave(@Path("id") id: kotlin.String): Response<Unit>

    /**
     * GET api/v1/teams
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @return [kotlin.collections.List<TeamSummaryDto>]
     */
    @GET("api/v1/teams")
    suspend fun teamsControllerList(): Response<kotlin.collections.List<TeamSummaryDto>>

    /**
     * DELETE api/v1/teams/{id}
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id 
     * @return [Unit]
     */
    @DELETE("api/v1/teams/{id}")
    suspend fun teamsControllerRemove(@Path("id") id: kotlin.String): Response<Unit>

    /**
     * PATCH api/v1/teams/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id 
     * @param body 
     * @return [TeamDto]
     */
    @PATCH("api/v1/teams/{id}")
    suspend fun teamsControllerUpdate(@Path("id") id: kotlin.String, @Body body: kotlin.Any): Response<TeamDto>

}
