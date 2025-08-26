package code.api.cache

import code.api.util.APIUtil
import redis.clients.jedis.Pipeline

import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Redis queue configuration per log level.
 */
case class RedisLogConfig(
                           queueName: String,
                           maxEntries: Int
                         )

/**
 * Simple Redis FIFO log writer.
 */
object RedisLogger {

  /**
   * Redis-backed logging utilities for OBP.
   */
  object LogLevel extends Enumeration {
    type LogLevel = Value
    val TRACE, DEBUG, INFO, WARNING, ERROR, ALL = Value

    /** Parse a string into LogLevel, defaulting to INFO if unknown */
    def valueOf(str: String): LogLevel = str.toUpperCase match {
      case "TRACE"   => TRACE
      case "DEBUG"   => DEBUG
      case "INFO"    => INFO
      case "WARN" | "WARNING" => WARNING
      case "ERROR"   => ERROR
      case "ALL"     => ALL
      case _         => INFO
    }
  }

  // Define FIFO queues, sizes configurable via props
  val configs: Map[LogLevel.Value, RedisLogConfig] = Map(
    LogLevel.TRACE   -> RedisLogConfig("trace_logs",   APIUtil.getPropsAsIntValue("keep_n_trace_level_logs_in_cache",   0)),
    LogLevel.DEBUG   -> RedisLogConfig("debug_logs",   APIUtil.getPropsAsIntValue("keep_n_debug_level_logs_in_cache",   0)),
    LogLevel.INFO    -> RedisLogConfig("info_logs",    APIUtil.getPropsAsIntValue("keep_n_info_level_logs_in_cache",    0)),
    LogLevel.WARNING -> RedisLogConfig("warning_logs", APIUtil.getPropsAsIntValue("keep_n_warning_level_logs_in_cache", 0)),
    LogLevel.ERROR   -> RedisLogConfig("error_logs",   APIUtil.getPropsAsIntValue("keep_n_error_level_logs_in_cache",   0)),
    LogLevel.ALL     -> RedisLogConfig("all_logs",     APIUtil.getPropsAsIntValue("keep_n_all_level_logs_in_cache",     0))
  )

  /**
   * Write a log line to Redis FIFO queue.
   */
  def log(level: LogLevel.LogLevel, message: String): Try[Unit] = Try {
    Option(Redis.jedisPool).foreach { pool =>
      val jedis = pool.getResource
      try {
        val pipeline: Pipeline = jedis.pipelined()

        // log to requested level
        configs.get(level).foreach(cfg => pushLog(pipeline, cfg, message))

        // also log to ALL
        configs.get(LogLevel.ALL).foreach(cfg => pushLog(pipeline, cfg, s"[$level] $message"))

        pipeline.sync()
      } finally {
        jedis.close()
      }
    }
  }

  private def pushLog(pipeline: Pipeline, cfg: RedisLogConfig, msg: String): Unit = {
    if (cfg.maxEntries > 0) {
      pipeline.lpush(cfg.queueName, msg)
      pipeline.ltrim(cfg.queueName, 0, cfg.maxEntries - 1)
    }
  }

  case class LogEntry(level: String, message: String)
  case class LogTail(entries: List[LogEntry])

  private val LogPattern = """\[(\w+)\]\s+(.*)""".r

  /**
   * Read latest messages from Redis FIFO queue.
   */
  def getLogTail(level: LogLevel.LogLevel): LogTail = {
    Option(Redis.jedisPool).map(_.getResource).map { jedis =>
      try {
        val cfg = configs(level)
        val rawLogs = jedis.lrange(cfg.queueName, 0, -1).asScala.toList

        val entries: List[LogEntry] = level match {
          case LogLevel.ALL =>
            rawLogs.collect {
              case LogPattern(lvl, msg) => LogEntry(lvl, msg)
            }
          case other =>
            rawLogs.map(msg => LogEntry(other.toString, msg))
        }

        LogTail(entries)
      } finally {
        jedis.close()
      }
    }.getOrElse(LogTail(Nil))
  }
}
