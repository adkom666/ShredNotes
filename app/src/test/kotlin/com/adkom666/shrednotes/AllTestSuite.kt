package com.adkom666.shrednotes

import com.adkom666.shrednotes.util.StringUtilsTest
import com.adkom666.shrednotes.util.SelectorTest
import com.adkom666.shrednotes.util.paging.PagingUtilsTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    SelectorTest::class,
    StringUtilsTest::class,
    PagingUtilsTest::class
)
class AllTestSuite
