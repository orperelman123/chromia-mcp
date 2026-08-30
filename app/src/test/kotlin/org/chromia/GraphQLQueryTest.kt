package org.chromia

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.chromia.data.queries.AssetQueries
import org.chromia.domain.AssetFilters
import org.chromia.domain.AssetSearchFilters
import org.chromia.domain.graphqlQuery
import org.chromia.domain.presentVariable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GraphQLQueryTest {

    @Test
    fun listVariablesEncodeAsJsonArrays() {
        val query = graphqlQuery {
            query("query Q(\$ids: [String]) { x }")
            variable("ids", listOf("abc", "def"))
            variable("limit", 10)
        }
        val variables = query.toJsonObject()["variables"]!!.jsonObject
        val ids = variables["ids"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("abc", "def"), ids)
        assertEquals("10", variables["limit"]!!.jsonPrimitive.content)
    }

    @Test
    fun presentVariableDropsBlankStringsAndEmptyLists() {
        assertNull(presentVariable(""))
        assertNull(presentVariable("   "))
        assertNull(presentVariable(emptyList<String>()))
        assertNull(presentVariable(listOf("  ", "")))
        assertEquals("chr", presentVariable("chr"))
        assertEquals(listOf("keep"), presentVariable(listOf("", "  ", "keep")))
        assertEquals(10, presentVariable(10))
    }

    @Test
    fun blankStringVariableIsOmittedFromJson() {
        val query = graphqlQuery {
            query("query Q(\$brid: String) { x }")
            variable("brid", "")
            variable("assetId", "chr")
        }
        val variables = query.toJsonObject()["variables"]!!.jsonObject
        assertTrue("brid" !in variables)
        assertEquals("chr", variables["assetId"]!!.jsonPrimitive.content)
    }

    @Test
    fun emptyAndAllBlankListsAreOmittedFromJson() {
        val query = AssetQueries.getAssetDistribution(
            "chr",
            AssetFilters(
                excludeAccounts = emptyList(),
                brids = listOf("  "),
            )
        )
        val variables = query.toJsonObject()["variables"]!!.jsonObject
        assertEquals("chr", variables["assetId"]!!.jsonPrimitive.content)
        assertTrue("excludeAccounts" !in variables, "empty excludeAccounts must be omitted")
        assertTrue("brids" !in variables, "all-blank brids must be omitted")
    }

    @Test
    fun blankBridOnFilterAssetsIsOmitted() {
        val query = AssetQueries.filterAssets(AssetSearchFilters(brid = ""))
        val variables = query.toJsonObject()["variables"]
        assertTrue(variables == null || "brid" !in variables.jsonObject)
    }

    @Test
    fun mixedListKeepsNonBlankItems() {
        val query = graphqlQuery {
            query("query Q(\$brids: [String]) { x }")
            variable("brids", listOf("", "  ", "brid-1"))
        }
        val variables = query.toJsonObject()["variables"]!!.jsonObject
        val brids = variables["brids"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("brid-1"), brids)
    }
}
