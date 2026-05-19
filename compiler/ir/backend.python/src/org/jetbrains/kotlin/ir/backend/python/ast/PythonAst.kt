package org.jetbrains.kotlin.ir.backend.python.ast

// A very simplified Python AST just for the MVP

sealed class PythonNode

sealed class PythonExpression : PythonNode()
sealed class PythonStatement : PythonNode()

// Statements

class PythonModule(val statements: List<PythonStatement>) : PythonNode()

class PythonFunctionDef(
    val name: String,
    val args: List<String>,
    val body: List<PythonStatement>,
    val isAsync: Boolean = false
) : PythonStatement()

class PythonReturn(val value: PythonExpression?) : PythonStatement()

class PythonExprStatement(val expression: PythonExpression) : PythonStatement()

class PythonAssign(val targets: List<PythonExpression>, val value: PythonExpression) : PythonStatement()

class PythonIf(
    val condition: PythonExpression,
    val body: List<PythonStatement>,
    val orelse: List<PythonStatement> = emptyList()
) : PythonStatement()

class PythonPass : PythonStatement()

// Expressions

class PythonName(val id: String) : PythonExpression()

class PythonCall(
    val func: PythonExpression,
    val args: List<PythonExpression>
) : PythonExpression()

class PythonAwait(val value: PythonExpression) : PythonExpression()

class PythonStringLiteral(val value: String) : PythonExpression()

class PythonIntLiteral(val value: Long) : PythonExpression()

class PythonFloatLiteral(val value: Double) : PythonExpression()

class PythonBooleanLiteral(val value: Boolean) : PythonExpression()

class PythonNoneLiteral : PythonExpression()

class PythonBinaryOperation(
    val left: PythonExpression,
    val op: String,
    val right: PythonExpression
) : PythonExpression()

class PythonAttribute(
    val value: PythonExpression,
    val attr: String
) : PythonExpression()
