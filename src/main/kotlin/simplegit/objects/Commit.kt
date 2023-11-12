package simplegit.objects

class Commit(
    val hash : String,
    val author : String,
    val message : String,
    val commitTime : Long,
    val root : Tree
)