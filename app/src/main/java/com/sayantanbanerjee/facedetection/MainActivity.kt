package com.sayantanbanerjee.facedetection

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerLocalModel
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerOptions
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var labeler: ImageLabeler
    private lateinit var faceFoundTextView: TextView
    private lateinit var withoutMaskTextView: TextView
    private lateinit var withMaskTextView: TextView
    private val REQUEST_GALLERY_CODE: Int
        get() = 101

    private var face_found : Int = 0
    private var mask_found : Int = 0
    private var nomask_found : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val localModel = AutoMLImageLabelerLocalModel.Builder()
            .setAssetFilePath("modelfiles/manifest.json")
            .build()

        val autoMLImageLabelerOptions = AutoMLImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.0F)
            .build()
        labeler = ImageLabeling.getClient(autoMLImageLabelerOptions)
        faceFoundTextView = findViewById(R.id.faceFound)
        withMaskTextView = findViewById(R.id.withMask)
        withoutMaskTextView = findViewById(R.id.withoutMask)
        imageView = findViewById(R.id.imageView)
        button = findViewById(R.id.button)
        button.setOnClickListener {
            cleanupVariables()
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.choose_Photo)),
                REQUEST_GALLERY_CODE
            )
        }
    }

    private fun cleanupVariables() {
        face_found = 0
        mask_found = 0
        nomask_found = 0
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY_CODE) {
            imageView.setImageURI(data!!.data)

            // create Firebase Vision Image
            val image: FirebaseVisionImage
            try {
                image = FirebaseVisionImage.fromFilePath(this, data.data!!)
                val bmpForTempImage : Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver , data.data)
                val bmp : Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver , data.data)
                val mutableBitmap : Bitmap = bmp.copy(Bitmap.Config.ARGB_8888,true)
                val canvas : Canvas = Canvas(mutableBitmap)

                // create Face Detector options
                // ACCURATE OR HARD -> accuracy
                // ALL_LANDMARKS OR NO_LANDMARKS -> identifying Eyes, cheeks, lips, mouth, ears etc
                // CLASSIFICATION OR NO_CLASSIFICATION -> smiling & eyes open
                val options =
                    FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .build()

                //Pass this options inside the Detector
                val detector = FirebaseVision.getInstance()
                    .getVisionFaceDetector(options)

                //Pass the image to the final detector
                val result =
                    detector.detectInImage(image)
                        .addOnSuccessListener {
                            //face_detected
                            for (face in it) {
                                face_found++
                                val bounds: Rect = face.boundingBox
                                val paint : Paint = Paint()

                                val resizedbitmap =
                                    Bitmap.createBitmap(bmpForTempImage, bounds.left, bounds.top, bounds.width(), bounds.height())

                                val tempImage =
                                    InputImage.fromBitmap(resizedbitmap, 0)

                                var mask_confidence : Float = 0F
                                var nomask_confidence : Float = 0F

                                labeler.process(tempImage)
                                    .addOnSuccessListener { labels ->
                                        for (label in labels) {
                                            val text = label.text
                                            Log.i("#########",text)
                                            if (text.equals("mask")){
                                                mask_confidence = label.confidence
                                            }else{
                                                nomask_confidence = label.confidence
                                            }
                                        }

                                        Log.i("NO MASK ###",nomask_confidence.toString())
                                        Log.i("MASK ###",mask_confidence.toString())

                                        if(nomask_confidence >= mask_confidence){
                                            nomask_found++
                                            val canvas : Canvas = Canvas(mutableBitmap)
                                            Log.i("AANDAR ###",nomask_confidence.toString())
                                            paint.color = Color.RED
                                            paint.strokeWidth = 10F
                                            paint.style = Paint.Style.STROKE
                                            canvas.drawRect(bounds,paint)
                                            withoutMaskTextView.text = getString(R.string.without_mask) + " " + nomask_found
                                        }else{
                                            mask_found++
                                            val canvas : Canvas = Canvas(mutableBitmap)
                                            paint.color = Color.GREEN
                                            paint.strokeWidth = 10F
                                            paint.style = Paint.Style.STROKE
                                            canvas.drawRect(bounds,paint)
                                            withMaskTextView.text = getString(R.string.with_mask) + " " + mask_found
                                        }
                                        imageView.setImageBitmap(mutableBitmap)
                                    }
                                    .addOnFailureListener { e ->

                                    }

                            }
                            faceFoundTextView.text = getString(R.string.face_found_number) + " " + face_found
                            withMaskTextView.text = getString(R.string.with_mask) + " " + mask_found
                            withoutMaskTextView.text = getString(R.string.without_mask) + " " + nomask_found
                        }
                        .addOnFailureListener {
                            //face_not_detected
                            faceFoundTextView.text = getString(R.string.face_found_number) + " " + face_found
                        }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
