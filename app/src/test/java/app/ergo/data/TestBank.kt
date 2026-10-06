package app.ergo.data

import java.io.File

/** Loads the real content bank from the module's assets for JVM tests. */
object TestBank {
    val dir: File by lazy {
        listOfNotNull(System.getProperty("ergo.content"), "src/main/assets/content", "app/src/main/assets/content")
            .map(::File)
            .first { File(it, "topics.json").exists() }
    }

    val bank: Bank by lazy {
        fun read(n: String) = File(dir, n).readText()
        Bank.parse(read("topics.json"), read("drills.json"), read("structure.json"), read("quizzes.json"), read("motions.json"), read("samples.json"))
    }
}
