package com.jake.popupschool.data.settings

import com.jake.popupschool.domain.model.BubbleIconType
import com.jake.popupschool.domain.model.PopupStyle
import com.jake.popupschool.domain.model.SchoolLevel
import com.jake.popupschool.domain.model.TimetableSource

data class AppSettings(
    val apiKey: String = "",
    val officeCode: String = "",
    val schoolCode: String = "",
    val schoolName: String = "",
    val schoolLevel: SchoolLevel = SchoolLevel.HIGH,
    val grade: String = "",
    val classNum: String = "",
    val timetableSource: TimetableSource = TimetableSource.NEIS,
    val popupStyle: PopupStyle = PopupStyle.DEFAULT,
    val bubbleIconType: BubbleIconType = BubbleIconType.DEFAULT,
    val bubbleIconText: String = ""
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && officeCode.isNotBlank() && schoolCode.isNotBlank() &&
            grade.isNotBlank() && classNum.isNotBlank()
}
