# Kotlin to Python Transpiler Implementation Plan

This document outlines the phases and steps required to implement a Kotlin-to-Python transpiler within the Kotlin Multiplatform (KMP) compiler. The initial code in `compiler/ir/backend.python` and `libraries/stdlib/python` was largely cloned from the Kotlin/JS target, and this plan covers transforming that into a proper Python backend. The reference architecture from `krzema12/kotlin-python` is used as a conceptual guide.

## Phase 1: Cleanup of JS Heritage and Stubbing Python Base

The immediate first step is to clean out all remaining JS-specific and browser-specific code from the `libraries/stdlib/python` and IR lowering phases to establish a clean slate.

1.  **[COMPLETED] Remove JS-specific and Browser-specific standard library APIs:**
    *   Delete the entire `libraries/stdlib/python/src/org.w3c` directory (DOM, WebGL, MediaCapture, Fetch, etc. have no meaning in Python).
    *   Delete JS-specific packages and files within `libraries/stdlib/python/src/kotlin/browser`, `libraries/stdlib/python/src/kotlin/js`, and any JS-specific interop features (`kotlin.js` package).
    *   Remove `libraries/stdlib/python/src/kotlin/dom` if it exists.
2.  **[COMPLETED] Clean up standard library JS-specific Inline TODOs:**
    A major part of the cleanup involves locating inline JS snippets currently wrapped in `TODO("...")` and replacing them with Python equivalents (or `expect/actual` constructs). These fall into several categories:

    *   **Object Initialization (`TODO("({})")`)**
        *   **Context:** Used heavily to initialize empty Javascript objects, primarily in web APIs but also in core utilities.
        *   **Action for Web/DOM (`org.w3c.dom.*`, `org.w3c.fetch.kt`, `org.khronos.webgl.kt`):** These files contain dozens of `val o = TODO("({})")` statements. Since these are web-only APIs, they will simply be deleted entirely as part of Step 1. These include:
            *   `libraries/stdlib/python/src/org.w3c/org.w3c.dom.mediacapture.kt`
            *   `libraries/stdlib/python/src/org.w3c/org.w3c.dom.kt`
        *   **Action for Core (`kotlin/json.kt`):** Replace `val res: dynamic = TODO("({})")` with Python dictionary initialization: `{}`.
        *   **Action for Reflection (`kotlin/reflect/createInstance.kt`):** Replace `return TODO("{}")` with standard Python instantiation or interop instantiation.

    *   **Array Initialization & Array Operations (`TODO("[]")`)**
        *   **Context:** Used to initialize Javascript arrays or call `Array.prototype` methods on iterables.
        *   **Action for Slicing (`_ArraysJs.kt`, `kotlin/collections/ArrayList.kt`):** Replace JS slicing calls like `return TODO("[]").slice.call(this)` and `return TODO("[]").slice.call(array)` with Python list slicing `this[:]` or `list(this)`.
        *   **Action for Creation (`kotlin/kotlin.kt`, `kotlin/collections/InternalStringMap.kt`, `kotlin/collections/ArraySorting.kt`):** Replace empty array creations like `TODO("[]")` and `TODO("[]").unsafeCast<JsRawArray<E>>()` with Python empty lists `[]`.

    *   **Object Operations (`TODO("Object...")`)**
        *   **Context:** Used to invoke Javascript `Object` static methods.
        *   **Action for Object.keys (`kotlin/json.kt`):** Replace `val keys: Array<String> = TODO("Object").keys(other)` with Python's `list(other.keys())`.
        *   **Action for Object.create (`kotlin/collections/InternalStringMap.kt`):** Replace `val result = TODO("Object.create(null)")` with an empty Python dictionary `{}`.
        *   **Action for Object Prototypes/Constructors (`kotlin/reflect/reflection.kt`):** Replace `TODO("Object").getPrototypeOf(e).constructor` and references to `TODO("Object")` with Python's `type(e)` or `__class__`.
        *   **Action for Object.prototype.hasOwnProperty (`kotlin/text/regex.kt`):** Replace `TODO("Object").prototype.hasOwnProperty.call(o, name)` with standard Python `hasattr(o, name)` or `name in o`.

    *   **Cryptographic & Random Values (`TODO("crypto")`)**
        *   **Context:** Used in WebCrypto API implementations.
        *   **Action for UUIDs (`kotlin/uuid/UuidJs.kt`):** Replace `TODO("crypto").getRandomValues(destination)` with Python's `secrets` module or `os.urandom()`.

    *   **File-level "to be converted to python" (`TODO("to be converted to python")`)**
        *   **Context:** General placeholders marking entire files or functions that were duplicated from JS but not yet ported.
        *   **Action for Collections (`ArraysJs.kt`, `CollectionsJs.kt`, `HashMap.kt`, `HashSet.kt`, `LinkedHashMap.kt`, etc.):** Re-implement using standard Python list/dict structures, dropping the "Js" suffix.
        *   **Action for Core Types (`NumbersJs.kt`, `sequenceJs.kt`, `UnsignedJs.kt`):** Map directly to Python `int`, `float`, and generators.
        *   **Action for Text & Regex (`RegexJs.kt`, `StringEncodingTestJs.kt`, `regexp.kt`):** Wrap Python's `re` module and standard string encoding.
        *   **Action for Utilities (`Base64Js.kt`, `debug.kt`, `Comparator.kt`):** Wrap Python's `base64` module, print statements, and `functools.cmp_to_key`.

3.  **[COMPLETED] Clean up Compiler IR JS remnants:**
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


    *   **Note on current AST & Code Generator Implementation:**
        *   The current `PythonAst.kt` is a very simplified MVP containing basic statements (`PythonIf`, `PythonFunctionDef`, `PythonReturn`) and expressions (`PythonCall`, `PythonStringLiteral`). This needs significant expansion to cover all of Kotlin's semantics. We must add:
            *   `PythonClassDef` for mapping `IrClass` declarations.
            *   `PythonImport` and `PythonImportFrom` for resolving external modules.
            *   `PythonLambda` for mapping inline lambdas and anonymous functions.
            *   `PythonListComp` and `PythonDictComp` for collection processing.
            *   `PythonDecorator` wrappers for things like `@property` or custom method annotations.
            *   `PythonTypeAnnotation` mapping Kotlin types to Python standard library `typing` module equivalents.
        *   `Transformers.kt` currently implements transformers but explicitly "ignores unsupported declarations for MVP". We must add support for:
            *   `IrClass`: Transforming properties, fields, and functions into Python class attributes and methods.
            *   `IrConstructor`: Lowering primary/secondary constructors to `__init__` and `__new__`.
            *   `IrProperty`: Transforming `val` and `var` definitions.
            *   `IrEnumEntry`: Transforming to Python `Enum` values.
            *   `IrTypeAlias`: Transforming to Python type hints.
        *   Also, the `IrExpressionToPythonTransformer` only has basic support for string concatenation, `getValue`, and direct builtin mappings. We must add:
            *   `IrWhen`: Map to `if-elif-else`.
            *   `IrTypeOperatorCall`: Map casts (`as`, `is`) to Python equivalents:
                *   `is` -> Map to python `isinstance(value, type)`
                *   `as` -> Map to direct assignments, relying on python duck typing, or explicit casts when parsing strings to numbers (`int()`, `float()`)
                *   `as?` -> Map to `value if isinstance(value, type) else None`
        *   `PythonCodeGenerator.kt` correctly handles basic indentation using a visitor pattern but needs to be enhanced to support complex nesting, multi-line strings, class generation, and Python's specific whitespace semantics.
        *   `PythonBackendContext.kt` currently throws errors for standard compiler features like `SharedVariablesManager`, `BackendSymbols`, and `InnerClassesSupport` because it is heavily stubbed. These will need to be implemented for complex Kotlin scoping/variables.
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
    *   Create annotations/stubs for importing Python modules:
        *   `@PythonModule`: To denote that an external Kotlin declaration should resolve to a specific Python module:
            *   Usage: `@PythonModule("math")` for math module functions.
        *   `@PythonName`: To override the generated Python name for a Kotlin function or class, to map natively to Python naming conventions:
            *   Usage: `@PythonName("__len__")` for a size property.
        *   `@PythonBuiltin`: To denote intrinsic Python functions that should not generate imports.
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
