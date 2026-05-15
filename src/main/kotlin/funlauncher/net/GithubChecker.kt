package funlauncher.net

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class GithubCommitResponse(
    val sha: String
)

object GithubChecker {
    suspend fun getLatestCommitHash(): String? {
        return try {
            val response: List<GithubCommitResponse> = Network.client.get("https://api.github.com/repos/Chokopieum-Software/MateriaLauncher/commits?sha=dev&per_page=1") {
                header("Accept", "application/vnd.github.v3+json")
            }.body()
            response.firstOrNull()?.sha
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
