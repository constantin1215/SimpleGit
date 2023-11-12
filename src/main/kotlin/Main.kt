import simplegit.repository.CommitRepository

fun main() {
    val rep = CommitRepository("src/main/resources/test")

    rep.showDirectoryContent()

    rep.stage("testDir1/testDir1File.txt")
}