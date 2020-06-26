package com.sayantanbanerjee.facedetection

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
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
    private val REQUEST_GALLERY_CODE: Int
        get() = 101

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

        imageView = findViewById(R.id.imageView)
        button = findViewById(R.id.button)
        button.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.choose_Photo)),
                REQUEST_GALLERY_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY_CODE) {
            imageView.setImageURI(data!!.data)

            // create Firebase Vision Image
            val image: FirebaseVisionImage
            try {
                image = FirebaseVisionImage.fromFilePath(this, data.data!!)
                val bmp : Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver , data.data)
                val mutableBitmap : Bitmap = bmp.copy(Bitmap.Config.ARGB_8888,true)
                val canvas : Canvas = Canvas(mutableBitmap)
                Toast.makeText(this, getString(R.string.face_detecting), Toast.LENGTH_LONG).show()

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
                                val bounds: Rect = face.boundingBox
                                val paint : Paint = Paint()

                                val resizedbitmap =
                                    Bitmap.createBitmap(mutableBitmap, bounds.left, bounds.top, bounds.width(), bounds.height())

                                val tempImage =
                                    InputImage.fromBitmap(resizedbitmap, 0)

                                var mask_confidence : Float = 0F
                                var nomask_confidence : Float = 0F

                                labeler.process(tempImage)
                                    .addOnSuccessListener { labels ->
                                        for (label in labels) {
                                            val text = label.text
                                            if (text.equals("mask")){
                                                mask_confidence = label.confidence
                                            }else{
                                                nomask_confidence = label.confidence
                                            }
                                        }

                                        if(nomask_confidence >= mask_confidence){
                                            paint.color = Color.RED
                                        }else{
                                            paint.color = Color.GREEN
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        paint.color = Color.BLUE
                                    }

                                paint.strokeWidth = 10F
                                paint.style = Paint.Style.STROKE
                                canvas.drawRect(bounds,paint)

                            }
                            imageView.setImageBitmap(mutableBitmap)
                            Toast.makeText(this, getString(R.string.face_found), Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            //face_not_detected
                            Toast.makeText(this, getString(R.string.face_not_found), Toast.LENGTH_LONG).show()
                        }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
