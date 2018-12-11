package com.xc.tracker.utils

/**
 * 生成唯一的id
 */
fun generateId(seed: String): Long {
  return StringBuilder(seed.hashCode()).append(System.currentTimeMillis()).toString().toLong()
}
