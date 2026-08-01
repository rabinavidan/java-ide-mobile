package com.javaide.mobile.completion

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportInserterTest {

    @Test
    fun pendingImportsFiltersAlreadyImportedAndSorts() {
        val source = """
            |package com.example;
            |
            |import java.util.List;
            |
            |public class Foo {}
        """.trimMargin()

        val result = ImportInserter.pendingImports(
            source,
            listOf("java.util.ArrayList", "java.util.List", "java.util.Map")
        )

        assertEquals(listOf("java.util.ArrayList", "java.util.Map"), result)
    }

    @Test
    fun pendingImportsDedupsInput() {
        val result = ImportInserter.pendingImports(
            "public class Foo {}",
            listOf("java.util.List", "java.util.List")
        )

        assertEquals(listOf("java.util.List"), result)
    }

    @Test
    fun pendingImportsEmptyWhenAllAlreadyImported() {
        val source = "import java.util.List;\npublic class Foo {}"

        val result = ImportInserter.pendingImports(source, listOf("java.util.List"))

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun insertionLineAfterLastImportWhenPackageAndImportsExist() {
        val source = """
            |package com.example;
            |
            |import java.util.List;
            |
            |public class Foo {
            |    List<String> a;
            |}
        """.trimMargin()

        assertEquals(3, ImportInserter.insertionLine(source))
    }

    @Test
    fun insertionLineAtTopWhenNoPackageOrImports() {
        val source = "public class Foo {\n    ArrayList a;\n}\n"

        assertEquals(0, ImportInserter.insertionLine(source))
    }

    @Test
    fun insertionLineAfterPackageWhenNoImportsExist() {
        val source = "package com.example;\n\npublic class Foo {}\n"

        assertEquals(1, ImportInserter.insertionLine(source))
    }
}
