package org.jetbrains.kotlin.ir.backend.python

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.backend.python.ir2python.PythonCodeGenerator
import org.jetbrains.kotlin.ir.backend.python.ir2python.IrModuleToPythonTransformer

fun compilePython(context: PythonBackendContext): String {
    val module = context.module
    val transformer = IrModuleToPythonTransformer(context)
    val pythonModule = transformer.generateModule(module)
    val codeGenerator = PythonCodeGenerator()
    return codeGenerator.generate(pythonModule)
}
