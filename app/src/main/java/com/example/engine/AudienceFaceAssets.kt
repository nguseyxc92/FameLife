package com.example.engine

import kotlin.math.abs

object AudienceFaceAssets {
    val FACE_FILES: List<String> = listOf(
        "000386.jpg", "000387.jpg", "000388.jpg", "000389.jpg", "000390.jpg",
        "000391.jpg", "000392.jpg", "000393.jpg", "000394.jpg", "000395.jpg",
        "000396.jpg", "000397.jpg", "000398.jpg", "000399.jpg", "000400.jpg",
        "000401.jpg", "000402.jpg", "000403.jpg", "000404.jpg", "000405.jpg",
        "000406.jpg", "000407.jpg", "000408.jpg", "000409.jpg", "000410.jpg",
        "000411.jpg", "000412.jpg", "000416.jpg", "000417.jpg", "000418.jpg",
        "000419.jpg", "000420.jpg", "000421.jpg", "000422.jpg", "000423.jpg",
        "000424.jpg", "000425.jpg", "000426.jpg", "000427.jpg", "000428.jpg",
        "000429.jpg", "000430.jpg", "000431.jpg", "000432.jpg", "000433.jpg",
        "000434.jpg", "000435.jpg", "000436.jpg", "000437.jpg", "000438.jpg",
        "000439.jpg", "000440.jpg", "000441.jpg", "000442.jpg", "000443.jpg",
        "000444.jpg", "000445.jpg", "000446.jpg", "000447.jpg", "000448.jpg",
        "000449.jpg", "000454.jpg", "000455.jpg", "000456.jpg", "000457.jpg",
        "000458.jpg", "000459.jpg", "000460.jpg", "000461.jpg", "000462.jpg",
        "000463.jpg", "000464.jpg", "000465.jpg", "000466.jpg", "000467.jpg",
        "000468.jpg", "000469.jpg", "000470.jpg", "000471.jpg", "000472.jpg",
        "000473.jpg", "000474.jpg", "000475.jpg", "000476.jpg", "000477.jpg",
        "000478.jpg", "000479.jpg", "000480.jpg", "000481.jpg"
    )

    fun assetUri(fileName: String): String = "file:///android_asset/pfp_faces/$fileName"

    fun deterministicHasProfilePicture(seed: Int): Boolean {
        return abs(seed) % 100 < 90
    }

    fun deterministicFaceUri(seed: Int): String? {
        if (!deterministicHasProfilePicture(seed)) return null
        val idx = abs(seed) % FACE_FILES.size
        return assetUri(FACE_FILES[idx])
    }
}
