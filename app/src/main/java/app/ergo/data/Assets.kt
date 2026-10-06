package app.ergo.data

import android.content.Context

/** Reads the offline content bank from assets/content. */
fun loadBank(context: Context): Bank {
    fun read(name: String) = context.assets.open("content/$name").bufferedReader().use { it.readText() }
    return Bank.parse(
        topics = read("topics.json"),
        drills = read("drills.json"),
        structure = read("structure.json"),
        quizzes = read("quizzes.json"),
        motions = read("motions.json"),
        samples = read("samples.json"),
    )
}
