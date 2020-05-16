package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.action.TranslatorAction
import cn.yiiguxing.plugin.translate.action.TranslatorActionGroup
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.invokeOnDispatchThread
import com.intellij.ide.DataManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.MemoryUsagePanel
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon

/**
 * TranslatorWidget
 */
class TranslatorWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.IconPresentation {

    private var statusBar: StatusBar? = null
    private var isInstalled: Boolean = false

    @Suppress("FunctionName")
    override fun ID(): String = javaClass.name

    override fun getTooltipText(): String = TranslateService.translator.name

    override fun getIcon(): Icon = TranslateService.translator.icon

    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        getPopupStep(it.component).apply {
            val at = Point(0, -content.preferredSize.height)
            show(RelativePoint(it.component, at))
        }
    }

    private fun getPopupStep(component: Component): ListPopup {
        val group = TranslatorActionGroup()
        val context = DataManager.getInstance().getDataContext(component)
        return JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "Translators",
                group,
                context,
                false,
                false,
                false,
                null,
                5,
                TranslatorAction.PRESELECT_CONDITION
            )
    }

    override fun dispose() {
        statusBar = null
        isInstalled = false
    }

    fun update() = invokeOnDispatchThread {
        statusBar?.updateWidget(ID())
    }

    fun install() = invokeOnDispatchThread {
        if (!isInstalled) {
            isInstalled = true
            WindowManager.getInstance()
                .getStatusBar(project)
                ?.addWidget(this, "before ${MemoryUsagePanel.WIDGET_ID}", project)
        }
    }

    fun uninstall() = invokeOnDispatchThread {
        statusBar?.removeWidget(ID())
    }
}