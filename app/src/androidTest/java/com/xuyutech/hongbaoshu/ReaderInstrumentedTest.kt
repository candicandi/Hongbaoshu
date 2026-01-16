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
        
        // 等待足够长的时间确保分页计算完成(从日志中可以看到分页需要时间)
        composeTestRule.waitForIdle()
        Thread.sleep(3000)  // 等待 3 秒确保分页完成
        
        // 验证封面不再显示 (进入了阅读页)
        composeTestRule.onNodeWithContentDescription("封面").assertDoesNotExist()
    }

    @Test
    fun readerScreen_showsMenuAndBgmControls() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(3000)  // 等待分页完成
        
        // 双击顶部区域打开菜单 (在屏幕顶部 5% 的位置双击)
        composeTestRule.onRoot().performTouchInput {
            val clickX = centerX
            val clickY = height * 0.05f  // 使用 5% 确保在顶部 20% 范围内
            doubleClick(position = androidx.compose.ui.geometry.Offset(clickX, clickY))
        }
        
        // 等待菜单动画完成
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        
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
        Thread.sleep(3000)  // 等待分页完成
        
        // 打开菜单
        composeTestRule.onRoot().performTouchInput {
            doubleClick(position = androidx.compose.ui.geometry.Offset(centerX, height * 0.05f))
        }
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        
        // 打开目录
        composeTestRule.onNodeWithText("目录").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        
        // 验证目录对话框显示 (查找"关闭"按钮,因为对话框中有两个"目录"文本)
        composeTestRule.onNodeWithText("关闭").assertIsDisplayed()
        
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
        Thread.sleep(3000)  // 等待分页完成
        
        // 执行右滑手势返回 (从左边缘向右滑动整个屏幕宽度)
        composeTestRule.onRoot().performTouchInput {
            swipe(
                start = androidx.compose.ui.geometry.Offset(width * 0.1f, centerY),
                end = androidx.compose.ui.geometry.Offset(width * 0.9f, centerY),
                durationMillis = 300
            )
        }
        
        // 等待翻页动画完成
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        
        // 验证回到封面
        composeTestRule.onNodeWithContentDescription("封面").assertIsDisplayed()
    }
}
