package com.xuyutech.hongbaoshu

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        composeTestRule.onNodeWithContentDescription("封面").assertIsDisplayed()
    }

    @Test
    fun navigateToReader_fromCover() {
        // 点击封面进入阅读
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        
        // 验证封面不再显示 (进入了阅读页)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("封面").assertDoesNotExist()
    }

    @Test
    fun readerScreen_showsMenuAndBgmControls() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        
        // 初始状态下菜单是隐藏的，双击顶部区域打开菜单
        // 获取屏幕高度的一小部分作为顶部点击区域
        composeTestRule.onRoot().performTouchInput {
            doubleClick(position = androidx.compose.ui.geometry.Offset(centerX, height * 0.1f))
        }
        composeTestRule.waitForIdle()
        
        // 验证菜单出现 (检查"阅读设置"标题)
        composeTestRule.onNodeWithText("阅读设置").assertIsDisplayed()
        
        // 验证目录按钮存在
        composeTestRule.onNodeWithText("目录").assertIsDisplayed()
        
        // 验证 BGM 控件存在 (在菜单中)
        composeTestRule.onNodeWithText("听书 & 音效").assertIsDisplayed()
    }

    @Test
    fun tocDialog_opensAndCloses() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        
        // 打开菜单
        composeTestRule.onRoot().performTouchInput {
            doubleClick(position = androidx.compose.ui.geometry.Offset(centerX, height * 0.1f))
        }
        composeTestRule.waitForIdle()
        
        // 打开目录
        composeTestRule.onNodeWithText("目录").performClick()
        composeTestRule.waitForIdle()
        
        // 验证目录对话框显示 (查找对话框标题)
        composeTestRule.onNodeWithText("目录").assertIsDisplayed()
        
        // 关闭目录
        composeTestRule.onNodeWithText("关闭").performClick()
        composeTestRule.waitForIdle()
        
        // 验证目录对话框消失
        composeTestRule.onNodeWithText("关闭").assertDoesNotExist()
    }

    @Test
    fun backGesture_navigatesToCover() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        
        // 执行右滑手势返回 (从左侧边缘向右滑)
        composeTestRule.onRoot().performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()
        
        // 验证回到封面
        composeTestRule.onNodeWithContentDescription("封面").assertIsDisplayed()
    }
}
