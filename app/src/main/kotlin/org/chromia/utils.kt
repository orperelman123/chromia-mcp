package org.chromia

const val USAGE_HELP = """
    Usage: program [--sse --host <host> --port <port> | --stdio]
      --sse                    Start SSE server (127.0.0.1:3001)
      --sse --host <host>        Custom host (default: 127.0.0.1)
      --sse --port <port>       Custom port (default: 3001)
      --stdio                  Start stdio server (default)
    
    Examples:
      --sse                             # 127.0.0.1:3001
      --sse --port 8080                 # 127.0.0.1:8080
      --sse --host 0.0.0.0 --port 8080  # All interfaces:8080
"""

data class SseOption(val host: String, val port: Int)

fun parseSseArgs(args: List<String>): SseOption {
    require(args.size % 2 == 0) { "Arguments must be in [--key value] pairs" }
    val options = args.chunked(2).associate {
        it[0].removePrefix("--") to it[1]
    }
    val host = options["host"] ?: "127.0.0.1"
    val port =  try {
        options["port"]?.toInt() ?: 3001
    } catch (_: NumberFormatException) {
        throw IllegalArgumentException("Invalid port: ${options["port"]}")
    }
     require(port in 1..65535) { "Port must be between 1-65535" }

    return SseOption(host, port)
}