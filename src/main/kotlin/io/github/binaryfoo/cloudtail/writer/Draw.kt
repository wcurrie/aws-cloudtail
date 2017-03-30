package io.github.binaryfoo.cloudtail.writer

import net.sourceforge.plantuml.OptionFlags
import java.io.File

fun main(args: Array<String>) {
    OptionFlags.getInstance().isVerbose = true
    drawSvgOfWsd(Diagram(File("tmp/all.wsd")))
}
