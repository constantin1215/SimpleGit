import simplegit.repository.CommitRepository

fun main() {
    val rep = CommitRepository("src/main/resources/test")

    rep.showDirectoryContent()

    rep.commit("bob", "added stuff")
    rep.commit("bob", "added stuff")
    rep.commit("bob", "added stuff")

    rep.listCommits()
}