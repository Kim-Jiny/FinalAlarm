package app.finalalarm.data.api

import retrofit2.http.*

interface FinalAlarmApi {

    // ---- Auth ----
    @POST("auth/signup")
    suspend fun signup(@Body req: SignupReq): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body req: LoginReq): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body req: RefreshReq): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body req: LogoutReq)

    // ---- Me ----
    @GET("me")
    suspend fun getMe(): UserDto

    @PATCH("me")
    suspend fun updateMe(@Body req: UpdateMeReq): UserDto

    @DELETE("me")
    suspend fun deleteMe()

    // ---- Push tokens ----
    @POST("push-tokens")
    suspend fun registerPushToken(@Body req: RegisterPushTokenReq): PushTokenDto

    @DELETE("push-tokens/{id}")
    suspend fun deletePushToken(@Path("id") id: String)

    // ---- Teams ----
    @GET("teams")
    suspend fun listTeams(): List<TeamSummary>

    @POST("teams")
    suspend fun createTeam(@Body req: CreateTeamReq): TeamDetail

    @GET("teams/{id}")
    suspend fun getTeam(@Path("id") id: String): TeamDetail

    @PATCH("teams/{id}")
    suspend fun updateTeam(@Path("id") id: String, @Body req: UpdateTeamReq): TeamDetail

    @DELETE("teams/{id}")
    suspend fun deleteTeam(@Path("id") id: String)

    @DELETE("teams/{id}/members/me")
    suspend fun leaveTeam(@Path("id") id: String)

    @DELETE("teams/{id}/members/{userId}")
    suspend fun kickMember(@Path("id") teamId: String, @Path("userId") userId: String)

    @PATCH("teams/{id}/members/{userId}")
    suspend fun changeMemberRole(
        @Path("id") teamId: String,
        @Path("userId") userId: String,
        @Body req: ChangeRoleReq,
    ): TeamMemberDto

    // ---- Invites ----
    @GET("teams/{id}/invites")
    suspend fun listInvites(@Path("id") teamId: String): List<InviteDto>

    @POST("teams/{id}/invites")
    suspend fun createInvite(@Path("id") teamId: String, @Body req: CreateInviteReq): InviteDto

    @DELETE("teams/{id}/invites/{inviteId}")
    suspend fun revokeInvite(@Path("id") teamId: String, @Path("inviteId") inviteId: String)

    @GET("team-invites/{code}/preview")
    suspend fun previewInvite(@Path("code") code: String): InvitePreviewDto

    @POST("team-invites/{code}/redeem")
    suspend fun redeemInvite(@Path("code") code: String): RedeemResult

    // ---- Missions ----
    @GET("missions")
    suspend fun listMissions(): List<MissionDto>

    @POST("missions")
    suspend fun createMission(@Body req: CreateMissionReq): MissionDto

    @GET("missions/{id}")
    suspend fun getMission(@Path("id") id: String): MissionDto

    @PATCH("missions/{id}")
    suspend fun updateMission(@Path("id") id: String, @Body req: UpdateMissionReq): MissionDto

    @DELETE("missions/{id}")
    suspend fun deleteMission(@Path("id") id: String)

    // ---- Alarms ----
    @GET("alarms")
    suspend fun listAlarms(
        @Query("teamId") teamId: String? = null,
        @Query("kind") kind: AlarmKind? = null,
        @Query("active") active: Boolean? = null,
    ): List<AlarmDto>

    @POST("alarms")
    suspend fun createAlarm(@Body req: CreateAlarmReq): AlarmDto

    @GET("alarms/{id}")
    suspend fun getAlarm(@Path("id") id: String): AlarmDto

    @PATCH("alarms/{id}")
    suspend fun updateAlarm(@Path("id") id: String, @Body req: Map<String, kotlinx.serialization.json.JsonElement>): AlarmDto

    @DELETE("alarms/{id}")
    suspend fun deleteAlarm(@Path("id") id: String)

    // ---- Windows ----
    @GET("alarm-windows")
    suspend fun listWindows(@Query("teamId") teamId: String? = null): List<WindowDto>

    @POST("alarm-windows")
    suspend fun createWindow(@Body req: CreateWindowReq): WindowDto

    @PATCH("alarm-windows/{id}")
    suspend fun updateWindow(@Path("id") id: String, @Body req: Map<String, kotlinx.serialization.json.JsonElement>): WindowDto

    @DELETE("alarm-windows/{id}")
    suspend fun deleteWindow(@Path("id") id: String)

    // ---- Push alarm ----
    @POST("push-alarm")
    suspend fun pushAlarm(@Body req: PushAlarmReq): PushAlarmResp

    // ---- Events ----
    @GET("alarm-events")
    suspend fun listActiveEvents(): List<AlarmEventDto>

    @POST("alarm-events")
    suspend fun createEvent(@Body req: CreateEventReq): AlarmEventDto

    @GET("alarm-events/{id}")
    suspend fun getEvent(@Path("id") id: String): AlarmEventDto

    @GET("alarm-events/history")
    suspend fun history(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<AlarmEventDto>

    @POST("alarm-events/{id}/snooze")
    suspend fun snooze(@Path("id") id: String): AlarmEventDto

    @POST("alarm-events/{id}/dismiss")
    suspend fun dismiss(@Path("id") id: String, @Body req: DismissReq): AlarmEventDto

    @POST("alarm-events/{id}/unlock-request")
    suspend fun requestUnlock(@Path("id") id: String): UnlockRequestDto

    // ---- Unlock requests ----
    @GET("unlock-requests/inbox")
    suspend fun inbox(
        @Query("teamId") teamId: String,
        @Query("status") status: UnlockRequestStatus? = null,
    ): List<UnlockRequestDto>

    @GET("unlock-requests/{id}")
    suspend fun getUnlockRequest(@Path("id") id: String): UnlockRequestDto

    @POST("unlock-requests/{id}/approve")
    suspend fun approveUnlock(@Path("id") id: String): UnlockRequestDto

    @POST("unlock-requests/{id}/cancel")
    suspend fun cancelUnlock(@Path("id") id: String)
}
