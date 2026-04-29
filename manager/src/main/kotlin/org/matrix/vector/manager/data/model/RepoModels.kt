package org.matrix.vector.manager.data.model

import com.google.gson.annotations.SerializedName

/** Pure Kotlin data classes representing the online repository JSON structure. */
data class OnlineModule(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("summary") val summary: String?,
    @SerializedName("latestRelease") val latestRelease: String?,
    @SerializedName("latestReleaseTime") val latestReleaseTime: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("hide") val hide: Boolean? = false,
    @SerializedName("readmeHTML") val readmeHTML: String?,
    @SerializedName("homepageUrl") val homepageUrl: String?,
    @SerializedName("sourceUrl") val sourceUrl: String?,
    @SerializedName("collaborators") val collaborators: List<Collaborator> = emptyList(),
    @SerializedName("releases") val releases: List<Release> = emptyList(),
)

data class Collaborator(
    @SerializedName("login") val login: String?,
    @SerializedName("name") val name: String?,
)

data class Release(
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("descriptionHTML") val descriptionHTML: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("releaseAssets") val releaseAssets: List<ReleaseAsset> = emptyList(),
)

data class ReleaseAsset(
    @SerializedName("name") val name: String?,
    @SerializedName("downloadUrl") val downloadUrl: String?,
    @SerializedName("size") val size: Int = 0,
)
