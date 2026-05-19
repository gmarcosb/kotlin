package org.jetbrains.kotlin.ir.backend.python

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.BackendSymbols
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport

class PythonBackendContext(
    val module: IrModuleFragment,
    override val irBuiltIns: IrBuiltIns,
    override val configuration: CompilerConfiguration,
    val messageCollector: MessageCollector,
) : CommonBackendContext {
    override var inVerbosePhase: Boolean = false
    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)
    override val irFactory: IrFactory = IrFactoryImpl

    override val diagnosticReporter: IrDiagnosticReporter
        get() = error("DiagnosticReporter is not fully implemented for Python backend")

    override val symbols: BackendSymbols
        get() = error("BackendSymbols are not fully implemented for Python backend")

    override val sharedVariablesManager: SharedVariablesManager
        get() = error("SharedVariablesManager is not supported for Python backend")

    override val innerClassesSupport: InnerClassesSupport
        get() = error("InnerClassesSupport mapping is not implemented for Python backend")
}
