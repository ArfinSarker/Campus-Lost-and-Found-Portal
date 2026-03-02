package com.sas.lostandfound;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;

public class CampusReportLostActivity extends AppCompatActivity {

    private TextInputEditText etItemName, etDateLost, etLocationLost, etDescription;
    private AutoCompleteTextView actvCategory;
    private MaterialCheckBox cbConfirm;
    private MaterialButton btnSubmit;
    private com.google.android.material.card.MaterialCardView uploadCard;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    private String userName = "";
    private String userEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_report_lost);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference("LostImages");

        initializeViews();
        setupCategoryDropdown();
        setupDatePicker();
        fetchCurrentUserData();
        
        uploadCard.setOnClickListener(v -> openGallery());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void initializeViews() {
        etItemName = findViewById(R.id.etItemName);
        actvCategory = findViewById(R.id.actvCategory);
        etDateLost = findViewById(R.id.etDateLost);
        etLocationLost = findViewById(R.id.etLocationLost);
        etDescription = findViewById(R.id.etDescription);
        cbConfirm = findViewById(R.id.cbConfirm);
        btnSubmit = findViewById(R.id.btnSubmitReport);
        uploadCard = findViewById(R.id.uploadCard); 
    }

    private void fetchCurrentUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userName = snapshot.child("name").getValue(String.class);
                    userEmail = snapshot.child("email").getValue(String.class);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Electronics", "Documents", "Books", "Personal Items", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        actvCategory.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etDateLost.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year1, month1, dayOfMonth) -> {
                        String selectedDate = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                        etDateLost.setText(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Toast.makeText(this, "Image Selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndSubmit() {
        String name = etItemName.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String date = etDateLost.getText().toString().trim();
        String location = etLocationLost.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etItemName.setError("Required");
            return;
        }
        if (!cbConfirm.isChecked()) {
            Toast.makeText(this, "Please confirm accuracy", Toast.LENGTH_SHORT).show();
            return;
        }

        submitReport(name, category, date, location, description);
    }

    private void submitReport(String itemName, String category, String date, String location, String description) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        DatabaseReference lostRef = mDatabase.child("LostItems");
        String itemId = lostRef.push().getKey();

        if (imageUri != null) {
            StorageReference fileRef = mStorageRef.child(itemId + ".jpg");
            fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> 
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveToDatabase(itemId, itemName, category, date, location, description, uri.toString(), currentUserId);
                })
            ).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            saveToDatabase(itemId, itemName, category, date, location, description, "", currentUserId);
        }
    }

    private void saveToDatabase(String itemId, String itemName, String category, String date, String location, String description, String imageUrl, String currentUserId) {
        Item item = new Item(itemId, itemName, category, description, location, date, imageUrl, "lost", currentUserId);
        item.setUserName(userName);
        item.setUserEmail(userEmail);

        mDatabase.child("LostItems").child(itemId).setValue(item).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference userItemRef = mDatabase.child("UserItems");
                HashMap<String, Object> userItem = new HashMap<>();
                userItem.put("itemType", "lost");
                userItem.put("referenceId", itemId);
                userItemRef.child(currentUserId).child(itemId).setValue(userItem);

                Toast.makeText(this, R.string.success_report_submitted, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
