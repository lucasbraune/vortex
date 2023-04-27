val download by tasks.registering(Exec::class) {
    val cmd = """
        curl -L https://github.com/jepsen-io/maelstrom/releases/download/v0.2.3/maelstrom.tar.bz2 \
        | tar xf - -C $buildDir
    """.trimIndent()
    commandLine("bash", "-c", cmd)
    outputs.dir("$buildDir/maelstrom")
}

val maelstromBin: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(maelstromBin.name, file("$buildDir/maelstrom/maelstrom")) {
        builtBy(download)
    }
}
