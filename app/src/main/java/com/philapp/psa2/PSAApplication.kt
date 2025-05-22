package com.philapp.psa2

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PSAApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        testFirestore()
    }

    private fun testFirestore() {
        val db = Firebase.firestore
        
        // Create a test document
        val testData = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "message" to "Test data from PSA app",
            "status" to "active"
        )

        // Add the test document to a 'test_collection'
        db.collection("test_collection")
            .add(testData)
            .addOnSuccessListener { documentReference ->
                Log.d("FirestoreTest", "Document added successfully with ID: ${documentReference.id}")
                
                // Now try to read it back
                documentReference.get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            Log.d("FirestoreTest", "Document read successfully: ${document.data}")
                        } else {
                            Log.e("FirestoreTest", "Document does not exist")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreTest", "Error reading document", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreTest", "Error adding document", e)
            }
    }
} 