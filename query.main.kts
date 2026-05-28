import java.net.URL
val url = URL("https://search.maven.org/solrsearch/select?q=g:%22org.jetbrains.jewel%22&rows=5&wt=json")
println(url.readText())