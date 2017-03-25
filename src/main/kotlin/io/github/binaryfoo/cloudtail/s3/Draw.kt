package io.github.binaryfoo.cloudtail.s3

import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.OptionFlags
import net.sourceforge.plantuml.SourceStringReader
import java.io.File

fun main(args: Array<String>) {
    OptionFlags.getInstance().isVerbose = true
    drawSvgOfWsd(File("tmp/all.wsd"))
}

fun drawSvgOfWsd(wsdFile: File) {
    val plantUml = SourceStringReader(wsdFile.readText())
    File("tmp/trail.svg").outputStream().use { out ->
        plantUml.generateImage(out, FileFormatOption(FileFormat.SVG))
    }
}
