package com.sayantanbanerjee.facedetection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private val REQUEST_GALLERY_CODE: Int
        get() = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            val intent : Intent ?= null
            intent?.type = "image/*"
            intent?.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, REQUEST_GALLERY_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_GALLERY_CODE){
            imageView.setImageURI(data?.data)
        }
    }
}
