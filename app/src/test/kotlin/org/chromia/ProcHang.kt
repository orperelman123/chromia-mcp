package org.chromia

/**
 * Helper mains for RealProcessRunner regression tests - spawned as REAL child
 * JVMs so the tests exercise actual OS pipes and process lifetimes, not fakes.
 */
object ProcHang {
    @JvmStatic
    fun main(args: Array<String>) {
        when (if (args.isEmpty()) "" else args[0]) {
            // A child that stays alive with stdout open - models a hung chr.
            "sleep" -> Thread.sleep(args[1].toLong())
            // A child that floods stderr far past any OS pipe buffer before
            // exiting - models a chatty chr; a parent that drains stdout first
            // deadlocks against it.
            "flood" -> {
                val chunk = StringBuilder().also { sb -> repeat(8192) { sb.append('e') } }.toString()
                repeat(64) { System.err.print(chunk) } // 512 KiB
                System.err.flush()
                println("flood-done")
            }
            else -> {
                System.err.println("unknown mode")
                kotlin.system.exitProcess(2)
            }
        }
    }
}
