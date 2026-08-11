package com.jake.popupschool.util

import android.content.Context
import java.io.File

/** Fixed on-disk location for a user-picked bubble icon photo, copied here so it survives without needing a persisted content URI grant. */
fun bubbleIconImageFile(context: Context): File = File(context.filesDir, "bubble_icon.png")
