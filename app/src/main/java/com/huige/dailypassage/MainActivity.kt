package com.huige.dailypassage

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 设置 / 预览页。
 * 小部件本身没有界面，这里是它的「控制面板」：
 * 看今天的全文、换一条、授予权限、开保活白名单。
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_change).setOnClickListener {
            State.advance(this)
            PassageWidget.refreshAll(this)
            render()
            toast("已换一条")
        }

        findViewById<Button>(R.id.btn_exact).setOnClickListener { openExactAlarmSettings() }

        findViewById<Button>(R.id.btn_keepalive).setOnClickListener { openAppSettings() }

        findViewById<Button>(R.id.btn_schedule).setOnClickListener {
            DailyScheduler.scheduleNext(this)
            PassageWidget.refreshAll(this)
            render()
            toast("已重新排定每日闹钟")
        }

        findViewById<Button>(R.id.btn_add_widget).setOnClickListener {
            Toast.makeText(
                this,
                "长按桌面空白处 → 小部件 → 找到「每日一段」→ 拖到桌面",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        DailyScheduler.scheduleNext(this)
        // 顺手重绘桌面小部件：系统切深浅色后若没自动重绘，打开本页即可恢复
        PassageWidget.refreshAll(this)
        render()
        updateExactAlarmLabel()
    }

    private fun render() {
        val units = PassageRepo.all(this)
        // 条目数从实际内容库算出，避免与文案口径脱节（此前曾把条数写死在布局里）
        findViewById<TextView>(R.id.tv_footer).text = getString(R.string.main_footer, units.size)
        if (units.isEmpty()) {
            findViewById<TextView>(R.id.tv_body).text = "内容库为空"
            return
        }
        val p = units[State.currentIndex(this, units.size)]
        findViewById<TextView>(R.id.tv_title).text = "${p.id}　${p.name}"
        // 内容库只含「核心模型」单一字段，无需再拼接字段名
        findViewById<TextView>(R.id.tv_board).text = p.board
        findViewById<TextView>(R.id.tv_body).text = p.text
    }

    private fun updateExactAlarmLabel() {
        val btn = findViewById<Button>(R.id.btn_exact)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            btn.text = if (am.canScheduleExactAlarms()) "精确闹钟：已授予" else "点这里授予「精确闹钟」权限"
        } else {
            btn.text = "精确闹钟：本机系统无需授权"
            btn.isEnabled = false
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (am.canScheduleExactAlarms()) {
            toast("已授予，无需重复操作")
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (e: Exception) {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (e: Exception) {
            toast("请手动到「设置 → 应用」中查找本应用")
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
