package io.github.binaryfoo.cloudtail.s3

import net.sourceforge.plantuml.OptionFlags
import net.sourceforge.plantuml.SourceStringReader
import java.io.File

fun main(args: Array<String>) {
    OptionFlags.getInstance().isVerbose = true

    val plantUml = SourceStringReader(File("tmp/all.wsd").readText())
    plantUml.generateImage(File("tmp/trail.png"))
}
