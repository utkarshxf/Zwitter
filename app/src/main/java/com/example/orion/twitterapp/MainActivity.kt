package com.example.orion.twitterapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.icu.number.NumberFormatter.with
import android.icu.number.NumberRangeFormatter.with
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets_tickets.view.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity() {
    var listTweet=ArrayList<Ticket>()
    var adapter:mytwitterAdpator?=null
    var myemail:String?=null
    var UserUID:String?=null
    var database= FirebaseDatabase.getInstance()
    var myRef = database.reference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        var b:Bundle=intent.extras!!

        myemail=b.getString("email")
        UserUID=b.getString("uid")
        Toast.makeText(this, "login", Toast.LENGTH_SHORT).show()

        //dummy data
        listTweet.add(Ticket("0","him","url","add"))

        adapter=mytwitterAdpator(this,listTweet)
        lvTweets.adapter=adapter
        loadpost()
    }

    inner class mytwitterAdpator: BaseAdapter {
        var Context: Context?=null
        var listnoteadpt:ArrayList<Ticket>
        constructor(Context: Context, listnoteadpt:ArrayList<Ticket>):super(){
            this.listnoteadpt=listnoteadpt
            this.Context=Context

        }

        override fun getCount(): Int {
            return listnoteadpt.size

        }

        override fun getItem(p0: Int): Any {
            return listnoteadpt[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {

            var mytweet=listnoteadpt[p0]
            if (mytweet.tweetpersionuid.equals("add")){
                var myView=layoutInflater.inflate(R.layout.add_ticket,null)
                myView.iv_attach.setOnClickListener(View.OnClickListener {
                loadimage()
                })

                myView.iv_post.setOnClickListener(View.OnClickListener{
                        myRef.child("posts").push().setValue(postinfo(UserUID!!,myView.etPost.text.toString(),downloadurl!!))
                myView.etPost.setText("")
                })
                return myView
            }else if (mytweet.tweetpersionuid.equals("loading")){
                var myView=layoutInflater.inflate(R.layout.loading_ticket,null)
                return myView
            }
            else{
                var myView=layoutInflater.inflate(R.layout.tweets_tickets,null)
                myView.txt_tweet.setText(mytweet.tweettext)

//                Glide.with(Context!!).load(mytweet.tweetimageurl!!.toUri()).into(myView.tweet_picture);
                Picasso.get().load(mytweet.tweetimageurl!!.toUri()).into(myView.tweet_picture);
                myRef.child("Users").child(mytweet.tweetpersionuid!!)
                    .addValueEventListener(object :ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            try{

                                var td= snapshot.value as HashMap<String,Any>
                                for (key in td.keys){
                                    var userinfo =td[key] as String
                                    if (key.equals("ProfileImage")){
                                    Glide.with(Context!!).load(userinfo).circleCrop().into(myView.picture_path);

                                    }else{
                                        myView.txtUserName.setText(SplitString(userinfo))
                                    }
                                }
                            }catch (ex:Exception){}
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })



                return myView

            }


        }


    }
    val Pickimg:Int=123
    fun loadimage(){
        var intent= Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(intent,Pickimg)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==Pickimg&&resultCode== RESULT_OK&&data!=null){
            val selectedimg=data.data
            val filepathcoloum= arrayOf(MediaStore.Images.Media.DATA)
            val cursur=contentResolver.query(selectedimg!!,filepathcoloum,null,null,null)
            cursur!!.moveToFirst()
            val coulomindex=cursur.getColumnIndex(filepathcoloum[0])
            val picturepath=cursur.getString(coulomindex)
            cursur.close()
            Uploadimage(BitmapFactory.decodeFile(picturepath))
        }
    }
        var downloadurl:String?=""
        fun Uploadimage(bitmap: Bitmap){
            listTweet.add(0,Ticket("0","him","url","loading"))
            adapter!!.notifyDataSetChanged()

            var du:String?=null
            var du1:String?=null
            var du3:String="&alt=media"
            val storage= FirebaseStorage.getInstance()
            var storageRef=storage.getReferenceFromUrl("gs://twitter-app-e3455.appspot.com")
            val df= SimpleDateFormat("ddMMyyHHmmss")
            val dataobj= Date()
            val imagePath=SplitString(myemail!!)+"."+df.format(dataobj)+".jpg"
            val ImageRef=storageRef.child("imagesPost/$imagePath")

            val baos= ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
            val data= baos.toByteArray()
            val uploadTask=ImageRef.putBytes(data)
            uploadTask.addOnSuccessListener {taskSnapshot ->
                ImageRef.downloadUrl.addOnCompleteListener (){

                    du=taskSnapshot.uploadSessionUri.toString()
                    du1=du!!.substring(0,du!!.indexOf("&uploadType"))
                    downloadurl=du1+du3
                    listTweet.removeAt(0)
                    adapter!!.notifyDataSetChanged()
                    Toast.makeText(applicationContext,"Image uploaded", Toast.LENGTH_LONG).show()}
            }.addOnFailureListener{
                Toast.makeText(this,"fail to upload", Toast.LENGTH_LONG).show()
            }

        }


    fun SplitString(email:String):String{
        val split= email.split("@")
        return split[0]
    }


    fun loadpost(){
        myRef.child("posts")
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    try{
                        listTweet.clear()
                        listTweet.add(Ticket("0","him","url","add"))
//                        listTweet.add(Ticket("0","him","url","ads"))
                        var td= snapshot.value as HashMap<String,Any>
                        for (key in td.keys){
                        var post=td[key] as HashMap<String,Any>
                            listTweet.add(Ticket(key,
                                post["text"] as String,
                                post["postImange"] as String,
                                post["userUID"] as String))
                        }
                        adapter!!.notifyDataSetChanged()
                    }catch (ex:Exception){}
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }
}