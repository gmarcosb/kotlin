package org.jetbrains.kotlin.ir.backend.python

import org.junit.Test
import kotlin.test.assertEquals
import org.jetbrains.kotlin.ir.backend.python.ast.*
import org.jetbrains.kotlin.ir.backend.python.ir2python.PythonCodeGenerator

class PythonCodeGeneratorTest {

    @Test
    fun testGenerateHelloWorld() {
        val module = PythonModule(
            listOf(
                PythonFunctionDef(
                    name = "hello",
                    args = listOf(),
                    body = listOf(
                        PythonExprStatement(
                            PythonCall(
                                func = PythonName("print"),
                                args = listOf(PythonStringLiteral("Hello, world!"))
                            )
                        )
                    )
                ),
                PythonExprStatement(
                    PythonCall(
                        func = PythonName("hello"),
                        args = listOf()
                    )
                )
            )
        )

        val generator = PythonCodeGenerator()
        val result = generator.generate(module)

        val expected = "def hello():\n    print(\"Hello, world!\")\n\nhello()\n\n"

        assertEquals(expected, result)
    }

    @Test
    fun testGenerateVariables() {
        val module = PythonModule(
            listOf(
                PythonAssign(
                    targets = listOf(PythonName("x")),
                    value = PythonIntLiteral(10)
                ),
                PythonAssign(
                    targets = listOf(PythonName("y")),
                    value = PythonBinaryOperation(PythonName("x"), "+", PythonIntLiteral(20))
                )
            )
        )

        val generator = PythonCodeGenerator()
        val result = generator.generate(module)

        val expected = "x = 10\ny = x + 20\n\n"

        assertEquals(expected, result)
    }
}
