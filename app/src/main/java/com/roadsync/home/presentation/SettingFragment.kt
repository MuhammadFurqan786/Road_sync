package com.roadsync.home.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.roadsync.R
import com.roadsync.auth.presentation.AuthActivity
import com.roadsync.databinding.FragmentSettingBinding
import com.roadsync.pref.PreferenceHelper
import com.roadsync.utils.GlobalUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


class SettingFragment : Fragment() {

    private lateinit var _binding: FragmentSettingBinding
    private val binding get() = _binding

    private lateinit var helper: PreferenceHelper
    private var cameraImageUri: Uri? = null

    // Launcher for camera
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                cameraImageUri?.let { uploadImage(it) }
            }
        }

    // Launcher for gallery
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val dataUri = result.data?.data
                if (dataUri != null) {
                    uploadImage(dataUri)
                }
            }
        }

    // Request permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                showImagePickerOptions()
            } else {
                // Permission denied
            }
        }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // Below Android 13
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else { // Android 13+
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_${timeStamp}.jpg"
        val storageDir = requireContext().cacheDir
        val file = File(storageDir, fileName)

        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider", // <- very important!
            file
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun uploadImage(uri: Uri) {
        // Show the progress bar
        binding.loadingView.visibility = View.VISIBLE

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance()
            .getReference("profile_images/$userId/${UUID.randomUUID()}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateUserProfileImage(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                // Hide the progress bar and show an error
                binding.loadingView.visibility = View.GONE
                Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfileImage(imageUrl: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        val updates = mapOf<String, Any>(
            "profileImage" to imageUrl
        )

        databaseRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show()
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.user_image)
                    .error(R.drawable.user_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.userImage)

                // Hide the progress bar after the image is updated
                binding.loadingView.visibility = View.GONE
            }
            .addOnFailureListener {
                // Hide the progress bar and show an error
                binding.loadingView.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Failed to update profile image",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun changeProfilePicture() {
        requestPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        helper = PreferenceHelper.getPref(requireContext())

        binding.trips.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
            bottomNav.selectedItemId = R.id.trips
        }

        binding.notificationLayout.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
            bottomNav.selectedItemId = R.id.notifications
        }
        binding.changePassword.setOnClickListener {
            showChangePasswordDialog(requireContext())
        }

        binding.changeNumber.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val database =
                FirebaseDatabase.getInstance().getReference("users/$userId/emergencyNumber")

            showEmergencyDialog(database)
        }

        binding.logoutButton.setOnClickListener {
            showLogoutDialog(requireContext())
        }
        binding.deleteButton.setOnClickListener {
            showDeleteAccountDialog(requireContext())
        }

        binding.personalInfo.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_profileFragment)
        }

        binding.editButton.setOnClickListener {
            changeProfilePicture()
        }

        binding.userImage.setOnClickListener {
            changeProfilePicture()
        }

        loadUserProfileImage()
    }

    private fun showEmergencyDialog(databaseRef: DatabaseReference) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_emergency_number, null)
        val editText = dialogView.findViewById<AppCompatEditText>(R.id.editEmergencyNumber)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true) // prevent skipping
            .create()

        dialogView.findViewById<AppCompatButton>(R.id.btnSave).setOnClickListener {
            val number = editText.text.toString().trim()
            if (number.isEmpty()) {
                editText.error = "Please enter a number"
                return@setOnClickListener
            }

            databaseRef.setValue(number).addOnSuccessListener {
                helper.getCurrentUser()?.emergencyNumber = number
                Toast.makeText(requireContext(), "Emergency number Update", Toast.LENGTH_SHORT)
                    .show()
                dialog.dismiss()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to Update", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun loadUserProfileImage() {
        // Show progress bar while loading

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        databaseRef.child("profileImage").get()
            .addOnSuccessListener { snapshot ->
                val imageUrl = snapshot.getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.user_image) // your default placeholder image
                        .error(R.drawable.user_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.userImage)

                    // Hide progress bar after image is loaded
                    binding.loadingView.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                // Hide progress bar and handle error
                binding.loadingView.visibility = View.GONE
            }
    }

    @SuppressLint("MissingInflatedId")
    private fun showChangePasswordDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_password, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        val currentPasswordEditText =
            dialogView.findViewById<AppCompatEditText>(R.id.currentPasswordEditText)
        val newPasswordEditText = dialogView.findViewById<AppCompatEditText>(R.id.newPasswordEditText)
        val errorTextView = dialogView.findViewById<AppCompatTextView>(R.id.errorTextView)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val changeButton = dialogView.findViewById<Button>(R.id.changeButton)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        GlobalUtils.setupPasswordToggle(
            context,
            currentPasswordEditText,
            R.drawable.ic_show_password,
            R.drawable.ic_hide_password
        )
        GlobalUtils.setupPasswordToggle(
            context,
            newPasswordEditText,
            R.drawable.ic_show_password,
            R.drawable.ic_hide_password
        )

        cancelButton.setOnClickListener { dialog.dismiss() }

        changeButton.setOnClickListener {
            val currentPassword = currentPasswordEditText.text.toString().trim()
            val newPassword = newPasswordEditText.text.toString().trim()
            val user = FirebaseAuth.getInstance().currentUser
            val email = user?.email
            errorTextView.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            if (user != null && currentPassword.isNotBlank() && newPassword.isNotBlank() && email != null) {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            user.updatePassword(newPassword)
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        progressBar.visibility = View.GONE
                                        Toast.makeText(
                                            context,
                                            "Password changed successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        dialog.dismiss()
                                    } else {
                                        progressBar.visibility = View.GONE
                                        errorTextView.text = "Failed to update password"
                                        errorTextView.visibility = View.VISIBLE
                                    }
                                }
                        } else {
                            progressBar.visibility = View.GONE
                            errorTextView.text = "Current password is incorrect"
                            errorTextView.visibility = View.VISIBLE
                        }
                    }
            } else {
                progressBar.visibility = View.GONE
                errorTextView.text = "Please fill in both fields"
                errorTextView.visibility = View.VISIBLE
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showLogoutDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val changeButton = dialogView.findViewById<Button>(R.id.changeButton)

        cancelButton.setOnClickListener { dialog.dismiss() }

        changeButton.setOnClickListener {
            helper.setUserLogin(false)
            helper.saveCurrentUser(null)
            helper.clearPreferences()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    @SuppressLint("MissingInflatedId")
    fun showDeleteAccountDialog(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_account, null)
        val dialog = AlertDialog.Builder(context).setView(view).create()

        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val errorText = view.findViewById<TextView>(R.id.errorTextView)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        cancelButton.setOnClickListener { dialog.dismiss() }

        deleteButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val uid = user.uid
                val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                progressBar.visibility = View.VISIBLE

                dbRef.removeValue().addOnCompleteListener { dataTask ->
                    if (dataTask.isSuccessful) {
                        user.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                helper.setUserLogin(false)
                                helper.saveCurrentUser(null)
                                helper.clearPreferences()
                                Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT)
                                    .show()
                                startActivity(Intent(requireContext(), AuthActivity::class.java))
                                requireActivity().finish()
                                dialog.dismiss()
                            } else {
                                progressBar.visibility = View.GONE
                                errorText.text = "Error: ${deleteTask.exception?.message}"
                                errorText.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        errorText.text = "Failed to delete user data"
                        errorText.visibility = View.VISIBLE
                    }
                }
            } else {
                progressBar.visibility = View.GONE
                errorText.text = "No logged in user"
                errorText.visibility = View.VISIBLE
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

}
