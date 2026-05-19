package org.jetbrains.kotlin.ir.backend.python.ir2python

import org.jetbrains.kotlin.ir.backend.python.PythonBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.python.ast.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitor

class IrModuleToPythonTransformer(val context: PythonBackendContext) {
    fun generateModule(module: IrModuleFragment): PythonModule {
        val statements = mutableListOf<PythonStatement>()

        for (file in module.files) {
            val fileTransformer = IrFileToPythonTransformer(context)
            statements.addAll(fileTransformer.generateFile(file))
        }

        return PythonModule(statements)
    }
}

class IrFileToPythonTransformer(val context: PythonBackendContext) {
    fun generateFile(file: IrFile): List<PythonStatement> {
        val statements = mutableListOf<PythonStatement>()

        val declarationTransformer = IrDeclarationToPythonTransformer(context)
        for (declaration in file.declarations) {
            val pyStmt = declaration.accept(declarationTransformer, null)
            if (pyStmt != null) {
                statements.add(pyStmt)
            }
        }

        return statements
    }
}

class IrDeclarationToPythonTransformer(val context: PythonBackendContext) : IrVisitor<PythonStatement?, Nothing?>() {

    override fun visitElement(element: IrElement, data: Nothing?): PythonStatement? {
        return null // Ignore unsupported declarations for MVP
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): PythonStatement? {
        if (declaration.body == null) return null

        val name = declaration.name.asString()
        val args = declaration.parameters.map { it.name.asString() }

        val statementTransformer = IrElementToPythonStatementTransformer(context)
        val bodyStatements = mutableListOf<PythonStatement>()

        for (stmt in declaration.body!!.statements) {
            val pyStmt = stmt.accept(statementTransformer, null)
            if (pyStmt != null) {
                bodyStatements.add(pyStmt)
            }
        }

        if (bodyStatements.isEmpty()) {
            bodyStatements.add(PythonPass())
        }

        return PythonFunctionDef(
            name = name,
            args = args,
            body = bodyStatements,
            isAsync = declaration.isSuspend
        )
    }
}

class IrElementToPythonStatementTransformer(val context: PythonBackendContext) : IrVisitor<PythonStatement?, Nothing?>() {

    private val expressionTransformer = IrElementToPythonExpressionTransformer(context)

    override fun visitElement(element: IrElement, data: Nothing?): PythonStatement? {
        // Fallback to expression statement if it's an expression
        if (element is IrExpression) {
            val expr = element.accept(expressionTransformer, null)
            if (expr != null) return PythonExprStatement(expr)
        }
        return null
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): PythonStatement? {
        val name = PythonName(declaration.name.asString())
        val init = declaration.initializer?.accept(expressionTransformer, null) ?: PythonNoneLiteral()
        return PythonAssign(listOf(name), init)
    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): PythonStatement? {
        val value = expression.value.accept(expressionTransformer, null)
        return PythonReturn(value)
    }

    override fun visitWhen(expression: IrWhen, data: Nothing?): PythonStatement? {
        if (expression.branches.isEmpty()) return null

        // Very simplified: just evaluate branches directly for MVP
        val branch = expression.branches.firstOrNull() ?: return null
        val cond = branch.condition.accept(expressionTransformer, null) ?: PythonBooleanLiteral(true)
        val bodyStmt = branch.result.accept(this, null)
        return PythonIf(cond, listOf(bodyStmt ?: PythonPass()))
    }
}

class IrElementToPythonExpressionTransformer(val context: PythonBackendContext) : IrVisitor<PythonExpression?, Nothing?>() {

    override fun visitElement(element: IrElement, data: Nothing?): PythonExpression? {
        return PythonNoneLiteral() // Fallback
    }

    override fun visitConstantValue(expression: IrConstantValue, data: Nothing?): PythonExpression? {
        // Mock fallback for constant values in MVP
        return PythonNoneLiteral()
    }

    override fun visitConst(expression: IrConst, data: Nothing?): PythonExpression? {
        val kind = expression.kind
        return when (kind) {
            is IrConstKind.String -> PythonStringLiteral(expression.value as String)
            is IrConstKind.Int -> PythonIntLiteral((expression.value as Number).toLong())
            is IrConstKind.Long -> PythonIntLiteral(expression.value as Long)
            is IrConstKind.Double -> PythonFloatLiteral(expression.value as Double)
            is IrConstKind.Float -> PythonFloatLiteral((expression.value as Float).toDouble())
            is IrConstKind.Boolean -> PythonBooleanLiteral(expression.value as Boolean)
            is IrConstKind.Null -> PythonNoneLiteral()
            else -> PythonNoneLiteral()
        }
    }

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): PythonExpression? {
        return PythonName(expression.symbol.owner.name.asString())
    }

    override fun visitCall(expression: IrCall, data: Nothing?): PythonExpression? {
        val func = expression.symbol.owner
        val name = func.name.asString()

        // Basic builtin mapping for MVP
        if (name == "println" || name == "print") {
            val args = mutableListOf<PythonExpression>()
            for (i in 0 until expression.arguments.size) {
                val arg = expression.arguments[i]?.accept(this, null) ?: PythonNoneLiteral()
                args.add(arg)
            }
            return PythonCall(PythonName("print"), args)
        }

        // Standard function call
        val pyFunc = PythonName(name)
        val args = mutableListOf<PythonExpression>()
        for (i in 0 until expression.arguments.size) {
            val arg = expression.arguments[i]?.accept(this, null) ?: PythonNoneLiteral()
            args.add(arg)
        }

        val call = PythonCall(pyFunc, args)

        return if (func.isSuspend) {
            PythonAwait(call)
        } else {
            call
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): PythonExpression? {
        // Map to a series of + operations or a formatted string.
        // For MVP, just + operations if there are elements.
        if (expression.arguments.isEmpty()) return PythonStringLiteral("")

        var current: PythonExpression = PythonCall(PythonName("str"), listOf(expression.arguments[0].accept(this, null)!!))
        for (i in 1 until expression.arguments.size) {
            val next = PythonCall(PythonName("str"), listOf(expression.arguments[i].accept(this, null)!!))
            current = PythonBinaryOperation(current, "+", next)
        }
        return current
    }
}
