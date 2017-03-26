package io.github.binaryfoo.cloudtail

import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.OptionFlags
import net.sourceforge.plantuml.SourceStringReader
import java.io.File
import java.io.PrintWriter

fun main(args: Array<String>) {
    OptionFlags.getInstance().isVerbose = true
    drawSvgOfWsd(File("tmp/all.wsd"))
}

/**
 * Render a web (plantuml) sequence diagram as SVG embedded in html.
 */
fun drawSvgOfWsd(wsdFile: File) {
    val plantUml = SourceStringReader(wsdFile.readText())
    val outputFile = File(wsdFile.parentFile, wsdFile.name.replace(".wsd", ".html"))
    println("Rendering $wsdFile to $outputFile")
    outputFile.outputStream().use { out ->
        val writer = PrintWriter(out)
        writer.println("""<html>
    <head>
        <script type="text/javascript"">${readJs("scroll-header.js")}</script>
    <head>
    <body>
""")
        writer.flush()
        plantUml.generateImage(out, FileFormatOption(FileFormat.SVG))
        writer.println("""
    </body>
</html>
""")
    }
}

fun readJs(fileName: String): String {
    return Thread.currentThread().contextClassLoader.getResourceAsStream(fileName).reader().readText()
}
