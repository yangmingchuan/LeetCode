package com.maiya.call.phone

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.maiya.call.R
import com.maiya.call.dialog.RingPermissionDialog
import com.maiya.call.dialog.SetRingProgressDialog
import com.maiya.call.phone.manager.CallerShowManager
import com.maiya.call.phone.manager.FloatingWindowManager
import com.maiya.call.phone.utils.CacheUtils
import com.maiya.call.util.LogUtils
import com.maiya.call.util.context.ContextUtils
import com.maiya.call.util.file.UpdateDownloadUtils
import com.maiya.call.util.file.download.FileDownloadCallback
import com.maiya.call.util.file.download.FileDownloadRequest
import com.maiya.call.util.file.download.FileDownloadTask
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_phone.*
import java.io.File


/**
 * 电话相关功能主页
 *
 */

@RequiresApi(Build.VERSION_CODES.M)
class PhoneActivity : AppCompatActivity() {
    private var setRingDialog: SetRingProgressDialog? = null
    var dialog: RingPermissionDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone)
        requestPermission()
        initView()
        initPersimmon()
    }

    private fun initView() {
    }

    private fun initPersimmon() {
        bt3.setOnClickListener {
            val intent = Intent(this, PhoneListActivity::class.java)
            startActivity(intent)
        }
        tv_set_ring.setOnClickListener{
            dialog = RingPermissionDialog(this)
            dialog?.getRingVideoPermission(this,object :CallerShowManager.OnPerManagerListener{
                override fun onGranted() {
                    val content: String = dialog!!.updateRingPerContent()
                    if(TextUtils.isEmpty(content)){
                        Toast.makeText(applicationContext, "权限全部同意，正在设置视频铃声", Toast.LENGTH_SHORT).show()
                        return
                    }
                    dialog?.show()
                }

                override fun onDenied() {
                    Toast.makeText(applicationContext, "请至权限管理同意权限，才能设置视频铃声", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    override fun onResume() {
        super.onResume()
        updateRingPerContent(dialog)
    }

    private fun updateRingPerContent(ringPerDialog: RingPermissionDialog?) {
        ringPerDialog?.let {
            if (it.isShowing) {
                val content: String = it.updateRingPerContent()
                if(TextUtils.isEmpty(content)){
                    it.dismiss()
                    downloadFile(FloatingWindowManager.instance.mp4Url)
                }
            }
        }
    }

    private fun downloadFile(url: String?) {
        val filePath: String = UpdateDownloadUtils.getApkUpdateFileName(url,this)
        val fileu = filePath.split(".apk").toTypedArray()[0] + ".mp4"
        val file = File(fileu)
        FileDownloadRequest.download(url, file, FileDownloadTask.DOWN_LOAD_NO_FILTER_TYPE, object : FileDownloadCallback() {
            private var currentProgress = 0
            private var currentTime: Long = 0
            override fun onStart() {
                super.onStart()
                LogUtils.e("视频下载开始")
                if (setRingDialog == null) {
                    setRingDialog = SetRingProgressDialog(applicationContext)
                }
                setRingDialog?.show()
            }

            override fun onProgress(progress: Int, networkSpeed: Long) {
                LogUtils.e("视频下载中：$progress")
                if (currentProgress != progress && System.currentTimeMillis() - currentTime > 100 && progress != 100) {
                    currentTime = System.currentTimeMillis()
                    return
                }
                currentProgress = progress
                setRingDialog?.updateProgress(100, progress)
            }

            override fun onDone() {
                super.onDone()
                LogUtils.e("视频下载完成")
                setRingDialog?.let {
                    if ( it.isShowing && !ContextUtils.isDestroyed(it.context)) {
                        it.dismiss()
                    }
                    Toast.makeText(applicationContext, "设置视频铃声成功", Toast.LENGTH_SHORT).show()
                    CacheUtils.putString(CacheUtils.SP_FILE_KEY, fileu)
                }

            }

            override fun onFailure() {
                super.onFailure()
                setRingDialog?.let {
                    if (it.isShowing && !ContextUtils.isDestroyed(it.context)) {
                        it.dismiss()
                    }
                }
                Toast.makeText(applicationContext, "设置视频铃声失败", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * 请求权限
     */
    private fun requestPermission() {
        AndPermission.with(this)
                .runtime()
                .permission(
                        Permission.Group.PHONE,
                        Permission.Group.LOCATION,
                        Permission.Group.CALL_LOG
                )
                .onGranted {
                    Toast.makeText(applicationContext, "权限同意", Toast.LENGTH_SHORT).show()
                }.onDenied {
                    Toast.makeText(applicationContext, "权限拒绝", Toast.LENGTH_SHORT).show()
                }.start()

    }

}
