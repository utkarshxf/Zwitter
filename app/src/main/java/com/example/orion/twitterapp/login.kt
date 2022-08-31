package com.example.orion.twitterapp

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.*
import com.google.firebase.storage.FileDownloadTask.TaskSnapshot
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.internal.cache.DiskLruCache
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class login : AppCompatActivity() {
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    var database=FirebaseDatabase.getInstance()
    var myRef = database.reference



    override fun onCreate(savedInstanceState: Bundle?) {
        analytics = Firebase.analytics

        auth = Firebase.auth
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        ivImagePerson.setOnClickListener(View.OnClickListener {
            checkPermission()
        })
        FirebaseMessaging.getInstance().subscribeToTopic("news")
    }
    val READIMAGE:Int=123
    fun checkPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)!=
                PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),READIMAGE)
                return
            }
        }
        loadimage()
    }
    val Pickimg:Int=123
    fun loadimage(){
        var intent=Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
            ivImagePerson.setImageBitmap(BitmapFactory.decodeFile(picturepath))
        }
    }
    fun buLogin(view: View) {
        LoginToFireBase(etEmail.text.toString(),etPassword.text.toString())

    }
    fun SaveImageInFirebase(){
        var du:String?=null
        var du1:String?=null
        var du3:String="&alt=media"
        var currentuser=auth.currentUser
        val email:String=currentuser!!.email.toString()
        val storage=FirebaseStorage.getInstance()
        var storageRef=storage.getReferenceFromUrl("gs://twitter-app-e3455.appspot.com")
        val df=SimpleDateFormat("ddMMyyHHmmss")
        val dataobj=Date()
        val imagePath=SplitString(email)+"."+df.format(dataobj)+".jpg"
        val ImageRef=storageRef.child("images/$imagePath")
        ivImagePerson.isDrawingCacheEnabled=true
        ivImagePerson.buildDrawingCache()

        val drawable=ivImagePerson.drawable as BitmapDrawable
        val bitmap=drawable.bitmap
        val baos=ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data= baos.toByteArray()
        val uploadTask=ImageRef.putBytes(data)
        uploadTask.addOnSuccessListener{taskSnapshot ->
            ImageRef.downloadUrl.addOnCompleteListener (){

                du=taskSnapshot.uploadSessionUri.toString()
                du1=du!!.substring(0,du!!.indexOf("&uploadType"))
                val downloadUrl=du1+du3
                myRef.child("Users").child(currentuser.uid).child("email").setValue(currentuser.email)
                myRef.child("Users").child(currentuser.uid).child("ProfileImage").setValue(downloadUrl)
                LoadTweet()
                Toast.makeText(applicationContext,"url : "+downloadUrl, Toast.LENGTH_LONG).show()}
        }.addOnFailureListener{
            Toast.makeText(this,"fail to upload",Toast.LENGTH_LONG).show()
        }

    }

    fun SplitString(email:String):String{
        val split= email.split("@")
        return split[0]
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
            when(requestCode){
            READIMAGE->{
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    loadimage()
                }else{
                    Toast.makeText(this,"cannot access your image",Toast.LENGTH_LONG).show()
                     }
                }
            else->super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }

    fun LoginToFireBase(email:String,password:String){

        auth!!.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){ task ->

                if (task.isSuccessful){
                    Toast.makeText(applicationContext,"Successful login",Toast.LENGTH_LONG).show()
                    SaveImageInFirebase()


                }else
                {
                    Toast.makeText(applicationContext,"fail login",Toast.LENGTH_LONG).show()
                }

            }

    }


    fun LoadTweet(){
        var currentUser =auth.currentUser

        if(currentUser!=null) {


            var intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email", currentUser.email)
            intent.putExtra("uid", currentUser.uid)

            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        LoadTweet()
    }



}