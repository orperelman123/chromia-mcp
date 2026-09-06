package org.chromia.tools

/**
 * One concept, one canonical argument name - and every spelling this server has
 * ever used still accepted, with no warning and no preference.
 *
 * AUDIT F10 (2026-09-06), measured across all 74 schemas:
 *
 *   concept              names in use
 *   Rell source (single) `source` (rell_check, rell_security_check) vs `rell`
 *                        (check_dapp_project, check_ft4_imports,
 *                        deployment_preflight, deploy_testnet_chain)
 *   Rell sources (map)   `files` (8 tools) - and deployment_preflight accepted
 *                        `files` only as a DEPRECATING alias: "`files` was
 *                        accepted as an alias for the `rell` parameter - prefer
 *                        `rell` in future calls"
 *   chain identifier     `rid`, `brid`, `brids`, `blockchainRid`,
 *                        `blockchainIds`, `blockchain`
 *   deployment target    `target` (deployment_preflight) vs `network` (22 tools)
 *
 * "An agent guesses `source` for deployment_preflight and `rell` for rell_check,
 * one wasted call per tool, forever" - and the one note that actively pushed the
 * agent AWAY from the majority spelling has been deleted.
 *
 * The canonical name is the one the majority of tools already used, so nothing
 * an agent has learned stops working:
 *   - Rell sources -> `files` (8 tools already; a single string is still fine)
 *   - chain id     -> `brid`  (the name verify_deployment and the yml both use)
 *   - target       -> `network` (22 tools)
 *   - chromia.yml  -> `yaml`  (3 tools; `chromiaYml` is the alias)
 *
 * [SchemaOrder] is what the pin in ArgumentVocabularyTest reads: the canonical
 * name must be DECLARED and must come FIRST among its concept's names, so an
 * agent skimming a schema meets it before any alias.
 */
object ArgumentVocabulary {

    data class Concept(val name: String, val canonical: String, val aliases: Set<String>) {
        val all: Set<String> get() = aliases + canonical
    }

    val RELL_SOURCES = Concept("Rell sources", "files", setOf("rell", "source", "src", "code"))
    val CHAIN_ID = Concept("chain id", "brid", setOf("rid", "blockchainRid", "chainRid", "dappRid"))
    val NETWORK = Concept("deployment target", "network", setOf("target", "net"))
    val CHROMIA_YML = Concept("chromia.yml", "yaml", setOf("chromiaYml", "yml"))

    val CONCEPTS = listOf(RELL_SOURCES, CHAIN_ID, NETWORK, CHROMIA_YML)

    /**
     * Tools whose plural chain-id argument is a LIST of chains rather than one
     * chain - a different concept that keeps its own plural spelling.
     */
    val PLURAL_CHAIN_ID_NAMES = setOf("brids", "blockchainIds")

    /**
     * The concept a property name belongs to, or null. Case-insensitive, and
     * punctuation-insensitive, so `module_args` and `moduleArgs` never collide
     * with anything here.
     */
    fun conceptOf(property: String): Concept? {
        val n = property.lowercase()
        if (n in PLURAL_CHAIN_ID_NAMES.map { it.lowercase() }) return null
        return CONCEPTS.firstOrNull { c -> c.all.any { it.lowercase() == n } }
    }

    /**
     * The first violation of the canonical-first rule in [propertyNames], or
     * null when the order is right. [propertyNames] must be in schema order.
     */
    fun firstViolation(propertyNames: List<String>): String? {
        CONCEPTS.forEach { concept ->
            val present = propertyNames.filter { it in concept.all }
            if (present.isEmpty()) return@forEach
            if (concept.canonical !in present) {
                return "declares ${present.joinToString(", ")} for \"${concept.name}\" but not the " +
                    "canonical `${concept.canonical}`"
            }
            if (present.first() != concept.canonical) {
                return "names `${present.first()}` before the canonical `${concept.canonical}` " +
                    "for \"${concept.name}\""
            }
        }
        return null
    }
}
