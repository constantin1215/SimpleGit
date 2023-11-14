package simplegit.repository

import simplegit.hash.HashGenerator
import simplegit.objects.Blob
import simplegit.objects.Commit
import simplegit.objects.Tree
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime

class CommitRepository(private val pathToDir: String) {
    enum class STATUS { STAGED, UNSTAGED }

    private val files = mutableListOf<String>()
    private val directories = mutableMapOf<String, MutableList<String>>()
    private val stagingStatus = mapOf(STATUS.STAGED to mutableListOf<String>(), STATUS.UNSTAGED to mutableListOf<String>())

    private val blobs = mutableMapOf<String, Blob>()
    private val trees = mutableMapOf<String, Tree>()
    private val commits = mutableListOf<Commit>()

    init {
        scan()
    }

    private fun scan() {
        File(pathToDir)
            .walkTopDown()
            .forEach {
                run {
                    val filePath = "$it".split(pathToDir).last().ifBlank { "/" }
                    stagingStatus[STATUS.UNSTAGED]!!.add(filePath)

                    val attributes = Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)

                    if (attributes.isRegularFile && (filePath !in files)) {
                        files.add(filePath)
                        for (entry in directories.entries)
                            if (filePath.substringBeforeLast("/").ifBlank { "/" } == entry.key)
                                directories[entry.key]!!.add(filePath)
                    }

                    if (attributes.isDirectory && (filePath !in directories)) {
                        directories[filePath] = mutableListOf()
                        for (entry in directories.entries)
                        {
                            val parent = filePath.substringBeforeLast("/").ifBlank { "/" }
                            if (parent == entry.key)
                                directories[entry.key]!!.add(filePath)
                        }
                    }

                    directories["/"]!!.remove("/")
                }
            }
    }

    fun commit(author : String, message : String) {
        files.forEach {
            val content = File(pathToDir + it).readText()
            blobs[it] = Blob(HashGenerator.generateSHA1(it + content), content)
        }

        directories.forEach {
            trees[it.key] = Tree(HashGenerator.generateSHA1(it.key))

            for (fileName in it.value)
                if (fileName in files)
                    trees[it.key]!!.addElement(blobs[fileName]!!)
        }

        directories.forEach {
            for (fileName in it.value)
                if (fileName in directories.keys)
                    trees[it.key]!!.addElement(trees[fileName]!!)

            trees[it.key]!!.recalculateHash()
        }

        val date = LocalDateTime.now()

        val commit = Commit(
                HashGenerator.generateSHA1("$author$message$date"),
                author,
                message,
                date,
                trees["/"]!!
            )

        println(commit)

        commits.add(commit)
    }

    fun showDirectoryContent() {
        println("Files:\n$files\n\nDirectories:\n$directories\n\n")
    }

    fun showStagedFiles() {
        println("STAGED: ${stagingStatus[STATUS.STAGED]}")
    }

    fun showUnstagedFiles() {
        println("UNSTAGED: ${stagingStatus[STATUS.UNSTAGED]}")
    }

    fun listCommits() {
        println(commits)
    }
}