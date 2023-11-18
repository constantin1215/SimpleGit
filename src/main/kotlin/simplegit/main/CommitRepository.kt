package simplegit.main

import simplegit.hash.HashGenerator
import simplegit.objects.Blob
import simplegit.objects.Commit
import simplegit.objects.Tree
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.util.*

class CommitRepository(private val pathToDir: String) {
    enum class STATUS { STAGED, UNSTAGED }

    private val files = mutableListOf<String>()
    private val directories = mutableMapOf<String, MutableList<String>>()
    private val stagingStatus = mapOf(STATUS.STAGED to mutableListOf<String>(), STATUS.UNSTAGED to mutableListOf<String>())

    private val blobs = mutableMapOf<String, Blob>()
    private val trees = mutableMapOf<String, Tree>()
    private val commits = mutableListOf<Commit>()

    private var currentUser = ""

    init {
        scan()
        run()
    }

    private fun scan() {
        File(pathToDir)
            .walkTopDown()
            .forEach {
                run {
                    val filePath = "$it".split(pathToDir).last().ifBlank { "/" }

                    if (!stagingStatus[STATUS.STAGED]!!.contains(filePath) && !stagingStatus[STATUS.UNSTAGED]!!.contains(filePath)) {
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
    }

    private fun run() {
        println("Hello! Welcome to:")
        println("  _________.__                  .__              ________ .__   __   \n" +
                " /   _____/|__|  _____  ______  |  |    ____    /  _____/ |__|_/  |_ \n" +
                " \\_____  \\ |  | /     \\ \\____ \\ |  |  _/ __ \\  /   \\  ___ |  |\\   __\\\n" +
                " /        \\|  ||  Y Y  \\|  |_> >|  |__\\  ___/  \\    \\_\\  \\|  | |  |  \n" +
                "/_______  /|__||__|_|  /|   __/ |____/ \\___  >  \\______  /|__| |__|  \n" +
                "        \\/           \\/ |__|               \\/          \\/            ")
        println("\nType 'git help' to see available commands.")
        while (true) {
            print(">>>  ")
            val input = Scanner(System.`in`).nextLine()
            val command = if (input.count { it == ' ' } > 1) input.substringBeforeLast(' ') else input
            when(command) {
                "git help" -> help()
                "git credentials" -> enterCredentials()
                "git ls-files" -> showDirectoryContent()
                "git status" -> printStatus()
                "git add" -> stageFiles(input.substringAfterLast(' '))
                "git commit" -> commit(input)
                "git log" -> listCommits()
                "exit" -> {
                    println("Goodbye!")
                    return
                }
                else -> println("Unknown command. Try typing 'git help' to see available commands.")
            }
        }
    }

    private fun help() {
        println("SimpleGit Commands:")
        println("  git help       - Display available commands and their descriptions.")
        println("  git ls-files   - Show the content of the directory.")
        println("  git status     - Print the current staging status of files.")
        println("  git add        - Stage changes in the directory.")
        println("  git commit     - Commits the staged files. It MUST contain the '-m' flag set the commit message.")
        println("  git log        - List the commit history.")
        println("  exit           - Quit the Git command-line interface.")
    }

    private fun stageFiles(path: String) {
        if (path == ".")
            stage("/")
        else
            stage(path)
    }

    private fun enterCredentials() {
        print("Username: ")
        val input = Scanner(System.`in`).nextLine()
        currentUser = input

        println("Me Tarzan, You $currentUser :)")
    }

    private fun commitFiles() {
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
    }

    private fun commit(input : String) {
        if (!input.contains("-m")) {
            println("Command does not contain '-m' option so I cannot identify the commit message. :(")
            return
        }

        commitFiles()

        val message = input.substringAfterLast("-m")
        val date = LocalDateTime.now()

        val commit = Commit(
            HashGenerator.generateSHA1("$currentUser$message$date"),
            currentUser,
            message,
            date,
            trees["/"]!!
        )

        commits.add(commit)
    }

    private fun showDirectoryContent() {
        println("\tFiles:\n\t\t$files\n\n\tDirectories with content:\n\t\t$directories\n\n")
    }

    private fun checkIfUnstaged(fileName: String) {
        if (stagingStatus[STATUS.UNSTAGED]!!.contains(fileName)) {
            stagingStatus[STATUS.UNSTAGED]!!.remove(fileName)
            stagingStatus[STATUS.STAGED]!!.add(fileName)
        }
    }

    private fun stageParents(fileName: String) {
        var findParentFor = fileName

        while (findParentFor != "/")
        {
            for (directoryContent in directories.entries)
                if (findParentFor in directoryContent.value) {
                    checkIfUnstaged(findParentFor)
                    findParentFor = directoryContent.key
                }
        }
        checkIfUnstaged("/")
    }

    private fun stage(fileName: String) {
        if (fileName in files) {
            checkIfUnstaged(fileName)
            stageParents(fileName)
            printStatus()
            return
        }
        else if (fileName in directories.keys) {
            checkIfUnstaged(fileName)

            val directoriesToVisit = LinkedList<String>()
            for (file in directories[fileName]!!) {
                checkIfUnstaged(file)
                if (file in directories.keys)
                    directoriesToVisit.push(fileName)
            }

            while (directoriesToVisit.size != 0) {
                val directory = directoriesToVisit.pop()
                for (file in directories[directory]!!) {
                    checkIfUnstaged(file)
                    if (file in directories.keys)
                        directoriesToVisit.push(file)
                }
            }
            stageParents(fileName)
            printStatus()
            return
        }

        println("Nothing has been staged. Perhaps wrong path or file does not exists.")
    }

    private fun printStatus() {
        scan()
        println("UNSTAGED: ${stagingStatus[STATUS.UNSTAGED]}")
        println("STAGED: ${stagingStatus[STATUS.STAGED]}")
    }

    private fun listCommits() {
        println(commits)
    }
}