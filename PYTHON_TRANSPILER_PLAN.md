# Kotlin to Python Transpiler Implementation Plan

This document outlines the phases and steps required to implement a Kotlin-to-Python transpiler within the Kotlin Multiplatform (KMP) compiler. The initial code in `compiler/ir/backend.python` and `libraries/stdlib/python` was largely cloned from the Kotlin/JS target, and this plan covers transforming that into a proper Python backend. The reference architecture from `krzema12/kotlin-python` is used as a conceptual guide.

## Phase 1: Cleanup of JS Heritage and Stubbing Python Base

The immediate first step is to clean out all remaining JS-specific and browser-specific code from the `libraries/stdlib/python` and IR lowering phases to establish a clean slate.

1.  **Remove JS-specific standard library APIs:**
    *   Delete the entire `libraries/stdlib/python/src/org.w3c` directory (DOM, WebGL, MediaCapture, etc. have no meaning in Python).
    *   Delete JS-specific packages and files within `libraries/stdlib/python/src/kotlin/browser`, `libraries/stdlib/python/src/kotlin/js`, and any JS-specific interop features (`kotlin.js` package).
    *   Remove `libraries/stdlib/python/src/kotlin/dom` if it exists.
2.  **Clean up standard library TODOs:**
    *   Audit files in `libraries/stdlib/python/src/kotlin/` (e.g., `ArraysJs.kt`, `NumbersJs.kt`, `uuid/UuidJs.kt`, collections). Rename them to remove "Js" suffixes (e.g., `ArraysPython.kt`) and stub out the `TODO("to be converted to python")` functions with proper Python built-ins or custom Python interop expect/actual implementations (matching what was in `krzema12/kotlin-python` where appropriate).
    *   Replace inline JS invocations (`TODO("({})")`, `TODO("[]")`) with proper Kotlin-Python equivalents.
3.  **Clean up Compiler IR JS remnants:**
    *   In `compiler/ir/backend.python/src/org/jetbrains/kotlin/ir/backend/python/`, rename files or classes that still contain "Js" (if any) to use "Python".
    *   Remove JS-specific lowering phases (like Coroutines lowering for JS, JS Name Clashing, etc.) and JS-specific annotations from the Python backend context.

## Phase 2: Python AST Definition and Generation

We must establish the target Abstract Syntax Tree (AST) for Python and update the code generator.

1.  **Define Python AST structure (`compiler/ir/backend.python/src/org/jetbrains/kotlin/ir/backend/python/ast/`):**
    *   Implement nodes for Python Statements (`If`, `For`, `While`, `FunctionDef`, `ClassDef`, `Return`, `Pass`, `Import`, `Assign`).
    *   Implement nodes for Python Expressions (`Call`, `Attribute`, `BinOp`, `UnaryOp`, `List`, `Dict`, `Tuple`, `Constants`).
    *   *Reference:* Look at how `krzema12/kotlin-python` defines its Python AST in its `python/ast/src/generated/Python` or similar paths.
2.  **Implement Python AST Printer:**
    *   Write a visitor/printer that converts the AST nodes into valid Python source code text (handling indentation natively).
3.  **Update `Transformers.kt` & `PythonCodeGenerator.kt`:**
    *   Expand `IrDeclarationToPythonTransformer` to handle `IrClass`, `IrProperty`, `IrField`, and `IrSimpleFunction`.
    *   Implement an `IrExpressionToPythonTransformer` to traverse IR expressions (Blocks, Calls, Consts, TypeOps, Control Flow) and map them to Python AST nodes.

## Phase 3: IR Lowerings for Python

Since Kotlin's IR contains Kotlin-specific constructs that don't map 1:1 to Python, we need specific lowerings before AST generation.

1.  **Control Flow Lowering:**
    *   Lower Kotlin `when` expressions into `if-elif-else` chains.
    *   Decompose complex Kotlin blocks into sequence of statements since Python lambda/expressions are limited.
2.  **Property Lowering:**
    *   Convert Kotlin properties with backing fields and custom getters/setters into Python `@property` decorators or standard accessor methods.
3.  **Class and Interface Lowering:**
    *   Map Kotlin interfaces and abstract classes to Python `abc.ABC`.
    *   Lower object declarations (singletons) into Python module-level singletons or classes with overridden instantiation.
    *   Handle constructors (primary and secondary) by mapping them to Python's `__init__` and potentially `__new__`.
4.  **Interop and Bridges:**
    *   Implement bridges for generic type erasure (if necessary) and default argument stubs.
    *   Handle Kotlin varargs to Python `*args`.

## Phase 4: Python Interop and Expect/Actual Implementations

1.  **Define Python Interop Annotations:**
    *   Create annotations/stubs for importing Python modules (e.g., `@PythonModule`, `@PythonName`).
2.  **Implement Standard Library mappings:**
    *   `kotlin.Any` -> `object` (or implicit)
    *   `kotlin.String` -> `str`
    *   `kotlin.collections.List` -> `list`
    *   `kotlin.collections.Map` -> `dict`
    *   Exceptions and throwables -> Python `Exception` subclasses.
3.  **Port core functions:**
    *   Implement `println` / `print`.
    *   Implement basic math, string manipulation, and collection utilities using native Python behavior where performant.

## Phase 5: Testing and Integration

1.  **Setup Test Infrastructure:**
    *   Fix the Gradle test tasks for the Python backend (`./gradlew :compiler:backend.python:test`).
    *   Create a test runner that transpiles Kotlin to Python, and then executes the Python script using a local python interpreter (`python3`) to capture stdout/exit codes.
2.  **Port Box Tests:**
    *   Ensure the standard IR "box" tests can be executed against the Python backend.
