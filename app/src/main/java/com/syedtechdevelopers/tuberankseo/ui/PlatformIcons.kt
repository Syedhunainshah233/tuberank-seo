package com.syedtechdevelopers.tuberankseo.ui

import com.syedtechdevelopers.tuberankseo.R

/** Round platform icons shown in the tabs, history and result header. */
fun com.syedtechdevelopers.tuberankseo.data.Platform.icon(): Int = when (this) {
    com.syedtechdevelopers.tuberankseo.data.Platform.YouTube -> R.drawable.pf_youtube
    com.syedtechdevelopers.tuberankseo.data.Platform.TikTok -> R.drawable.pf_tiktok
    com.syedtechdevelopers.tuberankseo.data.Platform.Instagram -> R.drawable.pf_instagram
    com.syedtechdevelopers.tuberankseo.data.Platform.Facebook -> R.drawable.pf_facebook
}
