package com.github.retrofitx.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

//TODO remove it if moshi ksp could be done within retrofit-kx library, not retrofit-kx-ksp
class ChainSymbolProcessor(
    private val processors: List<SymbolProcessor>
): SymbolProcessor {
    private var processorsFinished = 1

    override fun process(resolver: Resolver): List<KSAnnotated> {
        var currentProcessor = processors[processorsFinished]
        var invalidSymbols = currentProcessor.processSafe(resolver)
        while (invalidSymbols.isEmpty()) {
            processorsFinished++
            if (processorsFinished == processors.size) {
                processorsFinished = 1
                return emptyList()
            }
            currentProcessor = processors[processorsFinished]
            invalidSymbols = currentProcessor.processSafe(resolver)
        }
        return invalidSymbols
    }

    private fun SymbolProcessor.processSafe(resolver: Resolver): List<KSAnnotated> = try {
        process(resolver)
    } catch (e: Exception) {
        emptyList()
    }

    override fun finish() {
        super.finish()
        processors.forEach(SymbolProcessor::finish)
    }

    override fun onError() {
        super.onError()
        processors.forEach(SymbolProcessor::onError)
    }
}