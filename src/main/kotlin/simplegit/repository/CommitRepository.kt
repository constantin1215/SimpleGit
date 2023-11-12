package simplegit.repository

import simplegit.hash.HashGenerator
import simplegit.objects.Blob
import simplegit.objects.CompositeElement
import simplegit.objects.Tree
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant

class CommitRepository(private val pathToDir: String) {
    enum class STATUS { STAGED, UNSTAGED }

    private val files = mutableListOf<String>()
    private val directories = mutableListOf<String>()
    private val other = mutableListOf<String>()
    private val stagingStatus = mapOf(STATUS.STAGED to mutableListOf<String>(), STATUS.UNSTAGED to mutableListOf<String>())

    private val blobs = mutableMapOf<String, Blob>()
    private val trees = mutableMapOf<String, Tree>()

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

                    if (attributes.isRegularFile && (filePath !in files))
                        files.add(filePath)

                    if (attributes.isDirectory && (filePath !in directories))
                        directories.add(filePath)

                    if (attributes.isOther && (filePath !in other))
                        other.add(filePath)
                }
            }
    }

    fun showDirectoryContent() {
        println("Files:\n$files\n\nDirectories:\n$directories\n\nOther:\n$other\n")
    }

    fun showStagedFiles() {
        println("STAGED: ${stagingStatus[STATUS.STAGED]}")
    }

    fun showUnstagedFiles() {
        println("UNSTAGED: ${stagingStatus[STATUS.UNSTAGED]}")
    }

    fun stage(path: String) {
        var occurences = 0
        var desiredPath = ""
        for (file in files)
            if (path in file) {
                occurences++
                desiredPath = file
            }

        if (occurences >= 2) {
            println("ERR: Path not specific enough.")
            return
        }

        if (occurences == 1) {
            if (!stagingStatus[STATUS.STAGED]!!.contains(desiredPath)) {

                stagingStatus[STATUS.UNSTAGED]!!.remove(desiredPath)
                stagingStatus[STATUS.STAGED]!!.add(desiredPath)

                val parentDirectories = desiredPath.split("/").dropLast(1).reversed()
                val fileContent = File(pathToDir + desiredPath).readText()
                val newBlob = Blob(HashGenerator.generateSHA1("$desiredPath${Instant.now().epochSecond}"), fileContent)

                blobs[desiredPath] = newBlob

                println(newBlob)

                var prevTree = Tree("dummy")
                for (dir in parentDirectories)
                    if(!trees.contains(dir.ifEmpty { "/" })) {
                        println("Creating new tree")

                        val newTree = Tree(HashGenerator.generateSHA1(newBlob.hash))
                        newTree.addElement(newBlob)

                        println(newTree)

                        trees[dir.ifEmpty { "/" }] = newTree
                        prevTree = newTree
                    }
                    else {
                        println("Found existing tree")

                        val existingTree = trees[dir.ifEmpty { "/" }]
                        existingTree!!.addElement(prevTree)
                        existingTree.recalculateHash()
                        prevTree = existingTree

                        println(existingTree)
                    }
            }
            else {
                println("ERR: File already staged")
            }
        }
    }
}