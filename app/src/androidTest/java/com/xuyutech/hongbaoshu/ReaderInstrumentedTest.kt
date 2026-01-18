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
        
        // 等待足够长的时间确保分页计算完成
        composeTestRule.waitForIdle()
        Thread.sleep(3000)
        
        // 验证封面不再显示 (进入了阅读页)
        composeTestRule.onNodeWithContentDescription("封面").assertDoesNotExist()
    }

    @Test
    fun backGesture_navigatesToCover() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(3000)  // 等待分页完成
        
        // 执行右滑手势返回 (从左边缘向右滑动)
        composeTestRule.onRoot().performTouchInput {
            swipe(
                start = androidx.compose.ui.geometry.Offset(width * 0.2f, centerY), // Avoid system edge (0.1f -> 0.2f)
                end = androidx.compose.ui.geometry.Offset(width * 0.9f, centerY),
                durationMillis = 500 // Slower swipe
            )
        }
        
        // 等待翻页动画完成
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        
        // 验证回到封面
        composeTestRule.onNodeWithContentDescription("封面").assertIsDisplayed()
    }

    @Test
    fun openMenu_verifyDisplay() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(2000)
        
        // 尝试点击中间区域以关闭可能存在的引导层
        composeTestRule.onRoot().performTouchInput { click() }
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // 双击顶部区域唤出菜单
        composeTestRule.onRoot().performTouchInput {
            // slightly lower to ensure hit
            val topArea = androidx.compose.ui.geometry.Offset(centerX, height * 0.15f)
            doubleClick(topArea)
        }
        
        composeTestRule.waitForIdle()
        
        // 验证菜单标题显示
        composeTestRule.onNodeWithText("阅读设置").assertIsDisplayed()
    }

    @Test
    fun toggleNightMode_changesState() {
        // 进入阅读界面
        composeTestRule.onNodeWithContentDescription("封面").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        // 打开菜单
        composeTestRule.onRoot().performTouchInput {
            doubleClick(androidx.compose.ui.geometry.Offset(centerX, height * 0.15f))
        }
        composeTestRule.waitForIdle()

        // 验证夜间模式开关存在并关闭
        composeTestRule.onNodeWithContentDescription("夜间模式开关").assertIsOff()

        // 点击切换
        composeTestRule.onNodeWithContentDescription("夜间模式开关").performClick()
        composeTestRule.waitForIdle()

        // 验证开关变为打开状态
        composeTestRule.onNodeWithContentDescription("夜间模式开关").assertIsOn()
    }
}
