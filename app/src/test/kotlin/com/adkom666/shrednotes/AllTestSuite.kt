package com.adkom666.shrednotes

import com.adkom666.shrednotes.util.StringUtilsTest
import com.adkom666.shrednotes.util.TruncatedToMinutesDateTest
import com.adkom666.shrednotes.util.paging.PagingUtilsTest
import com.adkom666.shrednotes.util.selection.ManageableSelectionTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    ManageableSelectionTest::class,
    StringUtilsTest::class,
    PagingUtilsTest::class,
    TruncatedToMinutesDateTest::class
)
class AllTestSuite
