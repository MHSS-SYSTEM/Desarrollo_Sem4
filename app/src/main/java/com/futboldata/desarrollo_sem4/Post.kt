package com.futboldata.desarrollo_sem4

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val texto: String = "",
    val fecha: Timestamp = Timestamp.now()
)
