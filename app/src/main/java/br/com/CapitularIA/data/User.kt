package br.com.CapitularIA.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val coverPhotoUrl: String = "",
    val name_lowercase: String = "",
    val aboutMe: String = "",
    val friendCount: Long = 0
)