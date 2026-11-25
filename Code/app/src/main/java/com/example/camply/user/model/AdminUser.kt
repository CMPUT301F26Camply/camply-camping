package com.example.camply.user.model

class AdminUser (
    uid:String,
    profile: Profile): User(uid,profile){
    override fun getRole(): String {
        return "admin"
    }
}