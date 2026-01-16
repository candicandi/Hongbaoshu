package com.xuyutech.hongbaoshu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阅读器仪表测试
 * 测试导航流、翻页、朗读控制等功能
 */
@RunWith(AndroidJUnit4::class)
class ReaderInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun coverScreen_isDisplayed() {
        // 验证封面屏幕显示
        // UI 中仅有一张封面图，没有"红宝书"或"进入阅读"的文本元素
        composeTestRule.onNodeWithContentDescription("封面").assertIsDisplayed()
    }

    @Test
    fun navigateToReader_fromCover() {
        // 点击封面进入阅读
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        
        // 验证阅读界面显示
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("返回").assertIsDisplayed()
    }

    @Test
    fun readerScreen_showsNavigationControls() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        
        // 验证导航控件存在
        composeTestRule.onNodeWithText("上一页").assertIsDisplayed()
        composeTestRule.onNodeWithText("下一页").assertIsDisplayed()
        composeTestRule.onNodeWithText("目录").assertIsDisplayed()
    }

    @Test
    fun readerScreen_showsBgmControls() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        
        // 验证 BGM 控件存在
        composeTestRule.onNodeWithText("BGM播放").assertIsDisplayed()
        composeTestRule.onNodeWithText("下一首").assertIsDisplayed()
    }

    @Test
    fun tocDialog_opensAndCloses() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        
        // 打开目录
        composeTestRule.onNodeWithText("目录").performClick()
        composeTestRule.waitForIdle()
        
        // 验证目录对话框显示
        composeTestRule.onNodeWithText("关闭").assertIsDisplayed()
        
        // 关闭目录
        composeTestRule.onNodeWithText("关闭").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun backButton_navigatesToCover() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        
        // 点击返回
        composeTestRule.onNodeWithText("返回").performClick()
        composeTestRule.waitForIdle()
        
        // 验证回到封面
        composeTestRule.onNodeWithContentDescription("封面").assertIsDisplayed()
    }
}
