package io.nativeplanet.urbit.launcher.guest

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Guest launcher — opens Android apps via standard Intents.
 * These are "guest" apps: sandboxed, no bridge to the Urbit ship.
 */
object GuestLauncher {

    fun launchPackage(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    fun launchDialer(context: Context, number: String? = null) {
        val intent = if (number != null) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        } else {
            Intent(Intent.ACTION_DIAL)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun launchSms(context: Context, number: String? = null) {
        val intent = if (number != null) {
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
        } else {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun launchCamera(context: Context) {
        val intent = Intent("android.media.action.STILL_IMAGE_CAMERA")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun launchMaps(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun launchEmail(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun launchBrowser(context: Context, url: String? = null) {
        val intent = if (url != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } else {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_BROWSER)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun launchEssential(context: Context, essentialId: String) {
        when (essentialId) {
            "phone" -> launchDialer(context)
            "sms" -> launchSms(context)
            "camera" -> launchCamera(context)
            "maps" -> launchMaps(context)
            "email" -> launchEmail(context)
            "browser" -> launchBrowser(context)
            "2fa" -> launchPackage(context, "com.aegis.authenticator")
                    || launchPackage(context, "com.google.android.apps.authenticator2")
        }
    }

    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
