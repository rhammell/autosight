package com.example.autosight

import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import java.math.BigInteger

interface ImageApiService {
    @Multipart
    @POST(AppConfig.IMAGE_UPLOAD_ENDPOINT)
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadResponse>
}

interface MetagraphL0ApiService {
    @GET("${AppConfig.METAGRAPH_L0_REWARDS_ENDPOINT}/{walletAddress}")
    suspend fun getRewards(@Path("walletAddress") walletAddress: String): Response<RewardsResponse>
}

interface MetagraphDataL1ApiService {
    @POST(AppConfig.METAGRAPH_DATA_L1_DATA_ENDPOINT)
    suspend fun postData(@Body data: CaptureData): Response<Unit>
}

data class UploadResponse(
    val message: String,
    val path: String
)

data class RewardsResponse(
    val rewards: BigInteger
)