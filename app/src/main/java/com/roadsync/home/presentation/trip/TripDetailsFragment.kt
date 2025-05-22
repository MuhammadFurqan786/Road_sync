package com.roadsync.home.presentation.trip

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.roadsync.auth.model.InvitedUser
import com.roadsync.auth.model.Participant
import com.roadsync.auth.presentation.InviteUserDialogFragment
import com.roadsync.databinding.FragmentTripDetailsBinding
import com.roadsync.home.models.Trip
import com.roadsync.home.presentation.MainActivity
import com.roadsync.home.presentation.trip.newtrip.ImageAdapter
import com.roadsync.home.presentation.trip.newtrip.InvitedUserAdapter
import com.roadsync.home.presentation.trip.newtrip.TripUserAdapter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


class TripDetailsFragment : Fragment() {

    private lateinit var _binding: FragmentTripDetailsBinding
    private val binding get() = _binding
    private var currentLat = 0.0
    private var currentLon = 0.0
    private lateinit var adapter: TripUserAdapter
    private lateinit var invitedUserAdapter: InvitedUserAdapter
    private lateinit var currentUserId: String
    private var isCreator = false
    private lateinit var trip: Trip
    private val viewModel: TripViewModel by viewModels()
    private var tripId: String? = null
    private lateinit var departure: String
    private lateinit var destination: String
    private var tripInviteCode: String? = null
    private val imageList = mutableListOf<String>()


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
        binding.loadingView.visibility = View.VISIBLE
        val storageRef = FirebaseStorage.getInstance()
            .getReference("trip_images/$tripId/${UUID.randomUUID()}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveImageUrl(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                binding.loadingView.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun saveImageUrl(imageUrl: String) {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips/$tripId/images")

        // Generate a unique imageId (could also be UUID)
        val imageId = UUID.randomUUID().toString()
        tripRef.child(imageId).setValue(imageUrl)
            .addOnSuccessListener {
                loadImages()
            }
            .addOnFailureListener {
                binding.loadingView.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to save image URL", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun loadImages() {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips/$tripId/images")

        tripRef.get().addOnSuccessListener { snapshot ->
            imageList.clear()
            for (child in snapshot.children) {
                val imageUrl = child.value as String
                imageList.add(imageUrl)
            }
            updateImageRecyclerView()
            binding.loadingView.visibility = View.GONE
        }.addOnFailureListener {
            binding.loadingView.visibility = View.GONE

            Toast.makeText(requireContext(), "Failed to load images", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImageRecyclerView() {
        // Update the RecyclerView or other UI component to display the images
        if (imageList.isNotEmpty()) {
            val adapter = ImageAdapter(imageList)
            binding.rvImages.adapter = adapter
            binding.rvImages.visibility = View.VISIBLE
            binding.imagesText.visibility = View.VISIBLE
        }
    }


    private fun uploadTripImage() {
        requestPermissions()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Assume invite code is passed via arguments
        tripInviteCode = arguments?.getString("inviteCode")

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { it1 ->
            viewModel.getUserLocation(it1, { lat, lon ->
                currentLat = lat
                currentLon = lon
            })

        }

        observeTripData()
        tripInviteCode?.let {
            binding.shareTripLayout.visibility = View.GONE
            binding.loadingView.visibility = View.VISIBLE
            viewModel.getTripByInviteCode(it)
        }


        binding.copyButton.setOnClickListener {
            tripInviteCode?.let {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Invite Code", it)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Invite code copied", Toast.LENGTH_SHORT).show()
            }
        }

        binding.startTrip.setOnClickListener {
            viewModel.startTrip()

            viewModel.tripStatus.observe(viewLifecycleOwner) {
                if (it == "Trip started successfully.") {
                    binding.startTrip.visibility = View.GONE
                    binding.endTrip.visibility = View.VISIBLE
                    val intent = Intent(requireContext(), ActiveTripActivity::class.java)
                    intent.putExtra("tripId", tripId)
                    intent.putExtra("departure", departure)
                    intent.putExtra("destination", destination)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }

        binding.endTrip.setOnClickListener {
            viewModel.endTrip()
            viewModel.tripStatus.observe(viewLifecycleOwner) {
                if (it == "Trip Ended successfully.") {
                    binding.endTrip.visibility = View.GONE
                    binding.startTrip.visibility = View.VISIBLE
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }

        binding.uploadImage.setOnClickListener {
            uploadTripImage()
        }

        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addCost.setOnClickListener {
            showAddCostDialog(requireContext()) { newCost ->
                // newCost is the cost entered by user
                // Now update your Trip model with new cost

                val updatedTrip = trip.copy(cost = newCost)

                // Then update to Firebase or ViewModel
                viewModel.updateTripCost(updatedTrip)
                Toast.makeText(requireContext(), "Trip cost updated!", Toast.LENGTH_SHORT).show()
                binding.totalCost.text = "Total Cost : $newCost"

            }
        }

        binding.inviteButton.setOnClickListener {

            val inviteUserDialog = InviteUserDialogFragment().apply {
                arguments = Bundle().apply {
                    putDouble("currentLat", currentLat)
                    putDouble("currentLon", currentLon)
                    putString("tripId", tripId)
                    putString("inviteCode", tripInviteCode)
                }
            }
            inviteUserDialog.show(childFragmentManager, "InviteUserDialog")
        }
    }

    private fun setupRecyclerView(userList: MutableList<Participant>) {
        adapter = TripUserAdapter(
            onRemoveClick = { participant ->
                tripId?.let { id ->
                    viewModel.removeUserFromTrip(id, participant.userId) { success, message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        if (success) {
                            adapter.removeUser(participant.userId)
                        }
                    }
                }
            },
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString(),
            users = userList
        )

        binding.rvUsers.adapter = adapter
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupRecyclerViewInvitedUsers(userList: MutableList<InvitedUser>) {
        if (userList.isEmpty()) {
            binding.noInvite.visibility = View.VISIBLE
        } else {
            invitedUserAdapter =
                InvitedUserAdapter(currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString(),
                    users = userList,
                    onRemoveClick = { participant ->
                        tripId?.let { id ->
                            viewModel.removeInvitedUserFromTrip(
                                id, participant.userId
                            ) { success, message ->
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    invitedUserAdapter.removeUser(participant.userId)
                                }
                            }
                        }
                    })
            binding.noInvite.visibility = View.GONE
            binding.rvInvitedUsers.adapter = invitedUserAdapter
            binding.rvInvitedUsers.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeTripData() {
        viewModel.tripData.observe(viewLifecycleOwner) { snapshot ->
            tripId = snapshot?.key
            viewModel.tripId = tripId
            trip = snapshot?.getValue(Trip::class.java)!!
            if (trip != null) {
                departure = trip.departure
                destination = trip.destination
                binding.inviteCode.text = trip.inviteCode
                val isCreator = trip.users[currentUserId]?.role == "creator"
                binding.tripName.text = "Trip name : ${trip.tripName}"
                binding.stayOption.text = "Stay At : ${trip.stayOption}"
                val cost = if (trip.cost == "") "N/A" else trip.cost
                binding.totalCost.text = "Total Cost : $cost"
                binding.route.text = "Route : ${trip.departure} - ${trip.destination}"
                if (trip.images.isNotEmpty()) {
                    loadImages()
                }
                if (isCreator) {
                    if (trip.active) {
                        binding.startTrip.visibility = View.GONE
                        binding.endTrip.visibility = View.VISIBLE
                    } else {
                        binding.startTrip.visibility = View.VISIBLE
                        binding.endTrip.visibility = View.GONE
                    }
                }
                val usersSnapshot = snapshot.child("users")
                val userList = mutableListOf<Participant>()
                for (child in usersSnapshot.children) {
                    val user = child.getValue(Participant::class.java)
                    if (user != null) {
                        userList.add(user)
                    }
                }
                if (userList.isNotEmpty()) {
                    setupRecyclerView(userList)
                    binding.rvUsers.visibility = View.VISIBLE
                    binding.loadingView.visibility = View.GONE
                    binding.noMember.visibility = View.GONE
                    binding.shareTripLayout.visibility = View.VISIBLE

                } else {
                    binding.shareTripLayout.visibility = View.GONE
                    binding.loadingView.visibility = View.GONE
                    binding.rvUsers.visibility = View.GONE
                    binding.noMember.visibility = View.VISIBLE
                    binding.error.visibility = View.VISIBLE
                }
                val invitedUserSnapshot = snapshot.child("invitedUsers")
                if (snapshot.exists()) {
                    val invitedUsers = mutableListOf<InvitedUser>()
                    for (child in invitedUserSnapshot.children) {
                        val user = child.getValue(InvitedUser::class.java)
                        if (user != null) {
                            invitedUsers.add(user)
                        }
                    }
                    setupRecyclerViewInvitedUsers(invitedUsers)
                    binding.rvInvitedUsers.visibility = View.VISIBLE
                    binding.loadingView.visibility = View.GONE
                    binding.noInvite.visibility = View.GONE
                    binding.shareTripLayout.visibility = View.VISIBLE
                } else {
                    binding.loadingView.visibility = View.GONE
                    binding.rvInvitedUsers.visibility = View.GONE
                    binding.noInvite.visibility = View.VISIBLE
                }

            } else {
                binding.loadingView.visibility = View.GONE
                binding.error.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Trip not found", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.tripStatus.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    fun showAddCostDialog(context: Context, onCostAdded: (String) -> Unit) {
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter trip cost"
            setPadding(50, 40, 50, 10)
        }

        AlertDialog.Builder(context)
            .setTitle("Add Trip Cost")
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val enteredCost = editText.text.toString().trim()
                if (enteredCost.isNotEmpty()) {
                    onCostAdded(enteredCost)
                } else {
                    Toast.makeText(context, "Cost cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
