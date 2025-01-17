package com.francisco.geovane.marcello.felipe.projetofinalandroid.main.service

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.francisco.geovane.marcello.felipe.projetofinalandroid.BuildConfig
import com.francisco.geovane.marcello.felipe.projetofinalandroid.main.model.LocationObj
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.util.*
import kotlin.properties.Delegates

class FirebasePlaceService {
    private val db = Firebase.firestore
    private val appId: String = BuildConfig.APP_ID
    private val defaultImage: String = "https://firebasestorage.googleapis.com/v0/b/fiapandroid.appspot.com/o/placeholders%2Flocation.png?alt=media&token=3c7ac60c-6ed1-4bac-b6a0-15bf61278cb4"

    private var imageRef: StorageReference? = Firebase.storage.reference.child("locations/")

    private lateinit var auth: FirebaseAuth
    private lateinit var nameField: String
    private lateinit var addressField: String
    private lateinit var descriptionField: String
    private lateinit var phoneField: String
    private var isVisitedField by Delegates.notNull<Boolean>()
    private lateinit var flavorField: String
    private lateinit var latField: String
    private lateinit var lngField: String
    private lateinit var imageField: String
    private lateinit var userIdField: String

    fun getAllLocations():LiveData<MutableList<LocationObj>> {
        val users = MutableLiveData<MutableList<LocationObj>>()
        val listPlaces = mutableListOf<LocationObj>()
        auth = Firebase.auth

        val locationRef = db.collection("Locations")
        locationRef
            .whereEqualTo("userId", auth.currentUser?.uid)
            .whereEqualTo("flavor", appId)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val id = document.id
                    val name = document.getString("name")
                    val lat = document.get("lat")
                    val lng = document.get("lng")
                    val isVisited = document.getBoolean("isVisited")
                    val address = document.getString("address")
                    val description = document.getString("description")
                    val phoneNumber = document.getString("phoneNumber")
                    val imageUrl = document.getString("image")
                    val flavor = document.getString("flavor")
                    val userId = document.getString("userId")
                    val place = LocationObj(
                        id,
                        name!!,
                        description!!,
                        lat!!,
                        lng!!,
                        isVisited!!,
                        phoneNumber!!,
                        address!!,
                        imageUrl,
                        flavor,
                        userId
                    )

                    listPlaces.add(place)
                }
                users.value = listPlaces
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
        return users
    }

    fun saveNewLocation(fields: LocationObj) {
        val formattedFields = initializeFields(fields)
        db.collection("Locations")
            .add(mapOf(
                    "name" to formattedFields.name,
                    "address" to formattedFields.address,
                    "description" to formattedFields.description,
                    "phoneNumber" to formattedFields.phoneNumber,
                    "isVisited" to formattedFields.isVisited,
                    "flavor" to formattedFields.flavor,
                    "lat" to formattedFields.lat,
                    "lng" to formattedFields.lng,
                    "image" to formattedFields.image,
                    "userId" to formattedFields.userId
            ))
            .addOnSuccessListener {
                Log.d(TAG, "Document ${fields.name} saved successful! ")
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error saving new document:", exception)
            }
    }

    private fun initializeFields(fields: LocationObj): LocationObj {
        nameField = fields.name as String
        addressField = fields.address as String
        descriptionField = fields.description as String
        phoneField = fields.phoneNumber as String
        isVisitedField = fields.isVisited as Boolean
        flavorField = fields.flavor as String
        latField = fields.lat as String
        lngField = fields.lng as String
        imageField = fields.image as String
        userIdField = fields.userId as String

        val name = if (nameField != "") fields.name else ""
        val address = if (addressField != "") fields.address else ""
        val description = if (descriptionField != "") fields.description else ""
        val phone = if (phoneField != "") fields.phoneNumber else ""
        val isVisited = if (isVisitedField) fields.isVisited else false
        val flavor = if (flavorField != "") fields.flavor else ""
        val lat = if (latField != "") fields.lat else ""
        val lng = if (lngField != "") fields.lng else ""
        val image = if (imageField != "") fields.image else defaultImage
        val userId = if (userIdField != "") fields.userId else ""

        return LocationObj(
            "",
            name,
            description,
            lat,
            lng,
            isVisited,
            phone,
            address,
            image,
            flavor,
            userId
        )
    }

    fun saveEditedLocation(id: String?, fields: LocationObj) {
        if (id != null) {
            val fieldRef = db.collection("Locations").document(id)

            fieldRef
            .update(mapOf(
                "name" to fields.name,
                "address" to fields.address,
                "description" to fields.description,
                "phoneNumber" to fields.phoneNumber,
                "isVisited" to fields.isVisited,
                "image" to fields.image
            ))
            .addOnSuccessListener {
                Log.d("Firebase", "Document ${fields.name} saved successful! ")
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error saving document $id:", exception)
            }
        }
    }

    fun deleteLocation(id: String) {
        db.collection("Locations").document(id)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "Document $id deleted successful.") }
            .addOnFailureListener { e -> Log.d(TAG, "Error deleting document $id", e) }
    }

    fun uploadImage(imageURI: Uri): Task<Uri>? {
        val ref = imageRef?.child("${UUID.randomUUID()}")
        val upload = ref?.putFile(imageURI)
        var uriString: String = ""
        return upload?.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            ref.downloadUrl
        }
        ?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                uriString = task.result.toString()
            } else {
                Log.d("FAILED", "Image download capture failed ")
            }
        }?.addOnFailureListener {
            Log.e(TAG, "Image upload failed ")
        }
    }
}