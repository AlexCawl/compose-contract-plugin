package com.alexcawl.contract

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ContractVersionCompatibilityTest {
    @TempDir
    lateinit var temporary: Path

    @Test
    fun oldAndRecompiledClientsRunAgainstNewVersionsWithoutPlugin() {
        val initialLibrary = compile("Library0", library(version = 0), plugin = true)
        val initialClient = compile("Client0", client(version = 0), dependencies = listOf(initialLibrary))
        val middleLibrary = compile("Library1", library(version = 1), plugin = true)
        val middleClient = compile("Client1", client(version = 1), dependencies = listOf(middleLibrary))
        val currentLibrary = compile("Library4", library(version = 4), plugin = true)
        val recompiledInitialClient = compile("RecompiledClient0", client(version = 0), dependencies = listOf(currentLibrary))
        val recompiledMiddleClient = compile("RecompiledClient1", client(version = 1), dependencies = listOf(currentLibrary))
        URLClassLoader(
            arrayOf(currentLibrary, initialClient, middleClient, recompiledInitialClient, recompiledMiddleClient)
                .map { it.toURI().toURL() }.toTypedArray(),
            javaClass.classLoader,
        ).use { loader ->
            for (count in listOf(32, 64)) {
                val contract = loader.loadClass("abi.C$count")
                val container = loader.loadClass("abi.C$count\$Contract")
                val factory = loader.loadClass("abi.C${count}ConstructorKt").declaredMethods.single {
                    it.name == "C$count" && it.parameterCount == count + 3
                }
                val first: (Any) -> Int = { 1 }
                val added: (Any) -> Int = { 999 }
                val future: (Any) -> Int = { 888 }
                val arguments = arrayOfNulls<Any>(count + 3)
                // Current order is tone01..toneNN, added, future, last.
                arguments[0] = first
                arguments[count] = added
                arguments[count + 1] = future
                arguments[arguments.lastIndex] = future
                val current = factory.invoke(null, *arguments)
                val clients = listOf(
                    "Client0" to 0, "Client1" to 1,
                    "RecompiledClient0" to 0, "RecompiledClient1" to 1,
                )
                for ((name, version) in clients) {
                    val client = loader.loadClass("abi.${name}Kt")
                    for (suffix in listOf("", "Direct", "Positional")) {
                        val created = client.getMethod("factory$count$suffix").invoke(null)
                        assertEquals(1, contract.getMethod("tone01").invoke(created))
                        assertEquals(count * 10, contract.getMethod(tone(count)).invoke(created))
                        assertEquals(count + 1, contract.getMethod("added").invoke(created))
                        assertEquals(count + 2, contract.getMethod("future").invoke(created))
                        val callbacks = client.getMethod("container$count$suffix").invoke(null)
                        assertEquals(count * 10, callback(container.getMethod("get${tone(count).replaceFirstChar { it.uppercase() }}")
                            .invoke(callbacks), created))
                        assertEquals(null, container.getMethod("getFuture").invoke(callbacks))
                        if (version == 0) assertEquals(null, container.getMethod("getAdded").invoke(callbacks))
                        val copied = client.getMethod("copy$count$suffix", contract).invoke(null, current)
                        assertEquals(10, contract.getMethod("tone01").invoke(copied))
                        assertEquals(count * 10, contract.getMethod(tone(count)).invoke(copied))
                        assertEquals(888, contract.getMethod("future").invoke(copied))
                        assertEquals(888, contract.getMethod("last").invoke(copied))
                        assertEquals(if (version == 0 || suffix.isEmpty()) 999 else count + 1,
                            contract.getMethod("added").invoke(copied))
                        val copiedCallbacks = contract.getMethod("contract").invoke(copied)
                        assertSame(future, container.getMethod("getFuture").invoke(copiedCallbacks))
                    }
                }
            }
        }
    }

    private fun library(version: Int): String = buildString {
        appendLine("package abi")
        appendLine("import com.alexcawl.contract.GenerateContract")
        appendLine("import com.alexcawl.contract.IntroducedAtContract")
        for (count in listOf(32, 64)) {
            appendLine("@GenerateContract interface C$count {")
            if (version >= 4) appendLine("@IntroducedAtContract(4) fun future(): Int = ${count + 2}")
            appendLine("fun tone01(): Int")
            if (version >= 1) appendLine("@IntroducedAtContract(1) fun added(): Int = ${count + 1}")
            for (index in 2..count) appendLine("fun ${tone(index)}(): Int = $index")
            if (version >= 4) appendLine("@IntroducedAtContract(4) fun last(): Int = ${count + 3}")
            appendLine("}")
        }
    }

    private fun client(version: Int): String = buildString {
        appendLine("package abi")
        for (count in listOf(32, 64)) {
            val partial = "tone01 = { 1 }, ${tone(count)} = { ${count * 10} }"
            val all = buildList {
                add("tone01 = { 1 }")
                for (index in 2..count) add("${tone(index)} = { ${if (index == count) count * 10 else index} }")
                if (version >= 1) add("added = { ${count + 1} }")
            }.joinToString()
            val positional = all.replace(Regex("\\w+ = "), "")
            for ((suffix, arguments) in listOf("" to partial, "Direct" to all, "Positional" to positional)) {
                appendLine("fun factory$count$suffix(): C$count = C$count($arguments)")
                appendLine("fun container$count$suffix(): C$count.Contract = C$count.Contract($arguments)")
                val replacements = if (suffix == "Positional") arguments.replaceFirst("{ 1 }", "{ 10 }")
                    else arguments.replace("tone01 = { 1 }", "tone01 = { 10 }")
                appendLine("fun copy$count$suffix(value: C$count): C$count = value.copy($replacements)")
            }
        }
    }

    private fun tone(index: Int): String = "tone${index.toString().padStart(2, '0')}"

    @Suppress("UNCHECKED_CAST")
    private fun callback(value: Any, receiver: Any): Int = (value as (Any) -> Int)(receiver)

    private fun compile(name: String, source: String, plugin: Boolean = false, dependencies: List<File> = emptyList()): File {
        val input = temporary.resolve("$name.kt").apply { writeText(source) }
        val output = temporary.resolve("$name.jar").toFile()
        val classpath = listOf(
            System.getProperty("org.jetbrains.kotlin.test.kotlin-stdlib"),
            System.getProperty("annotationsRuntime.jvm.classpath"),
        ) + dependencies.map { it.absolutePath }
        val arguments = mutableListOf(
            "-no-stdlib", "-no-reflect", "-jvm-target", "21", "-jvm-default=enable",
            "-classpath", classpath.joinToString(File.pathSeparator), "-d", output.absolutePath, input.toString(),
        )
        if (plugin) arguments.add("-Xplugin=${System.getProperty("contractPlugin.jar")}")
        val messages = ByteArrayOutputStream()
        val result = PrintStream(messages).use { K2JVMCompiler().exec(it, *arguments.toTypedArray()) }
        assertEquals(ExitCode.OK, result, messages.toString())
        return output
    }
}
