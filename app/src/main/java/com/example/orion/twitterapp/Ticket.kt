package com.example.orion.twitterapp

class Ticket {
    var tweetID:String?=null
    var tweettext:String?=null
    var tweetimageurl:String?=null
    var tweetpersionuid:String?=null
    constructor(tweetID:String,tweettext:String,tweetimageurl:String,tweetpersionuid:String){
        this.tweetID=tweetID
        this.tweettext=tweettext
        this.tweetimageurl=tweetimageurl
        this.tweetpersionuid=tweetpersionuid

    }

}