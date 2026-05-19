package org.jetbrains.kotlin.ir.backend.python.ir2python

import org.jetbrains.kotlin.ir.backend.python.ast.*

class PythonCodeGenerator {
    private val sb = StringBuilder()
    private var indentLevel = 0

    fun generate(module: PythonModule): String {
        for (statement in module.statements) {
            generateStatement(statement)
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun generateStatement(statement: PythonStatement) {
        when (statement) {
            is PythonFunctionDef -> {
                if (statement.isAsync) {
                    emit("async ")
                }
                emit("def ${statement.name}(${statement.args.joinToString(", ")}):")
                emitLine()
                indent()
                if (statement.body.isEmpty()) {
                    emit("pass")
                    emitLine()
                } else {
                    for (stmt in statement.body) {
                        generateStatement(stmt)
                    }
                }
                dedent()
                emitLine()
            }
            is PythonReturn -> {
                emit("return ")
                if (statement.value != null) {
                    generateExpression(statement.value)
                }
                emitLine()
            }
            is PythonExprStatement -> {
                generateExpression(statement.expression)
                emitLine()
            }
            is PythonAssign -> {
                for (target in statement.targets) {
                    generateExpression(target)
                    emit(" = ")
                }
                generateExpression(statement.value)
                emitLine()
            }
            is PythonIf -> {
                emit("if ")
                generateExpression(statement.condition)
                emit(":")
                emitLine()
                indent()
                if (statement.body.isEmpty()) {
                    emit("pass")
                    emitLine()
                } else {
                    for (stmt in statement.body) {
                        generateStatement(stmt)
                    }
                }
                dedent()
                if (statement.orelse.isNotEmpty()) {
                    emit("else:")
                    emitLine()
                    indent()
                    for (stmt in statement.orelse) {
                        generateStatement(stmt)
                    }
                    dedent()
                }
            }
            is PythonPass -> {
                emit("pass")
                emitLine()
            }
        }
    }

    private fun generateExpression(expression: PythonExpression) {
        when (expression) {
            is PythonName -> emit(expression.id)
            is PythonCall -> {
                generateExpression(expression.func)
                emit("(")
                for ((index, arg) in expression.args.withIndex()) {
                    if (index > 0) emit(", ")
                    generateExpression(arg)
                }
                emit(")")
            }
            is PythonAwait -> {
                emit("await ")
                generateExpression(expression.value)
            }
            is PythonStringLiteral -> {
                // Simplified escaping for MVP
                val escaped = expression.value.replace("\n", "\\n").replace("\"", "\\\"")
                emit("\"$escaped\"")
            }
            is PythonIntLiteral -> emit(expression.value.toString())
            is PythonFloatLiteral -> emit(expression.value.toString())
            is PythonBooleanLiteral -> emit(if (expression.value) "True" else "False")
            is PythonNoneLiteral -> emit("None")
            is PythonBinaryOperation -> {
                generateExpression(expression.left)
                emit(" ${expression.op} ")
                generateExpression(expression.right)
            }
            is PythonAttribute -> {
                generateExpression(expression.value)
                emit(".${expression.attr}")
            }
        }
    }

    private fun emit(s: String) {
        if (sb.isEmpty() || sb.last() == '\n') {
            sb.append("    ".repeat(indentLevel))
        }
        sb.append(s)
    }

    private fun emitLine() {
        sb.append("\n")
    }

    private fun indent() {
        indentLevel++
    }

    private fun dedent() {
        indentLevel--
    }
}
