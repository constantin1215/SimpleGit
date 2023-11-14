package simplegit.objects

import java.time.LocalDateTime

class Commit(
    private val hash : String,
    private val author : String,
    private val message : String,
    private val commitTime : LocalDateTime,
    private val root : Tree
) {
    override fun toString(): String {
        return "Commit(hash='$hash', author='$author', message='$message', commitTime=$commitTime, root=$root)"
    }
}