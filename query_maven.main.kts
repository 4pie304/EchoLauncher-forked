import java.net.URL

val url = URL("https://search.maven.org/solrsearch/select?q=g:%22org.jetbrains.jewel%22+AND+a:%22jewel-foundation%22&rows=5&wt=json")
val connection = url.openConnection()
val response = connection.getInputStream().bufferedReader().use { it.readText() }
println(response)