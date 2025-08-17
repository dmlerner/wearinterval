package com.wearinterval.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

// Test rule to set main dispatcher for testing
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
  private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestRule {
  override fun apply(base: Statement, description: Description): Statement =
    object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        Dispatchers.setMain(testDispatcher)
        try {
          base.evaluate()
        } finally {
          Dispatchers.resetMain()
        }
      }
    }
}
