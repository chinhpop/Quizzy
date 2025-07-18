package com.example.quizzy;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EditStudySetActivity extends AppCompatActivity {
    private LinearLayout termList;
    private Button btnAddTerm, btnDone, btnDeleteSet;
    private EditText etSetTitle;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String setId;
    private List<FlashCard> flashCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_study_set);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra đăng nhập
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để chỉnh sửa bộ học", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lấy setId từ Intent
        setId = getIntent().getStringExtra("setId");
        if (setId == null) {
            Toast.makeText(this, "Không tìm thấy bộ học", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khởi tạo views
        termList = findViewById(R.id.termList);
        btnAddTerm = findViewById(R.id.btnAddTerm);
        btnDone = findViewById(R.id.btnDone);
        btnDeleteSet = findViewById(R.id.btnDeleteSet);
        etSetTitle = findViewById(R.id.etSetTitle);
        flashCards = new ArrayList<>();

        // Tải dữ liệu study set và flashcard
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            DialogUtils.showConfirmationDialog(
                    this,
                    "Xác nhận",
                    "Bạn có muốn hủy chỉnh sửa bộ học không?",
                    "Có",
                    "Không",
                    (dialog, which) -> finish(),
                    (dialog, which) -> dialog.dismiss()
            );
        });
        loadStudySet();

        // Xử lý sự kiện thêm term
        btnAddTerm.setOnClickListener(v -> addTermView());

        // Xử lý sự kiện lưu thay đổi
        btnDone.setOnClickListener(v -> saveStudySet());

        // Xử lý sự kiện xóa study set
        btnDeleteSet.setOnClickListener(v -> deleteStudySet());
    }

    private void loadStudySet() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("study_sets").document(setId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String title = documentSnapshot.getString("title");
                    etSetTitle.setText(title != null ? title : "");

                    // Tải danh sách flashcard
                    db.collection("users").document(userId).collection("study_sets")
                            .document(setId).collection("flashcards")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                flashCards.clear();
                                termList.removeAllViews();
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    String front = doc.getString("front");
                                    String back = doc.getString("back");
                                    String flashcardId = doc.getId();
                                    flashCards.add(new FlashCard(front, back, flashcardId));
                                    addTermView(front, back);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi khi tải flashcard: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải bộ học: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addTermView() {
        addTermView("", "");
        flashCards.add(new FlashCard("", "", UUID.randomUUID().toString()));
    }

    private void addTermView(String front, String back) {
        View termView = getLayoutInflater().inflate(R.layout.item_term, null, false);
        EditText etFront = termView.findViewById(R.id.etTerm);
        EditText etBack = termView.findViewById(R.id.etDefinition);
        etFront.setText(front);
        etBack.setText(back);
        ImageView btnRemove = termView.findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(v -> {
            int index = termList.indexOfChild(termView);
            if (index < flashCards.size()) {
                termList.removeView(termView);
                flashCards.remove(index);
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp margin
        params.setMargins(0, margin, 0, margin);
        termView.setLayoutParams(params);
        termList.addView(termView);
    }

    private void saveStudySet() {
        String setTitle = etSetTitle.getText().toString().trim();
        if (setTitle.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
            return;
        }

        List<FlashCard> updatedFlashCards = new ArrayList<>();
        for (int i = 0; i < termList.getChildCount(); i++) {
            View termView = termList.getChildAt(i);
            EditText etFront = termView.findViewById(R.id.etTerm);
            EditText etBack = termView.findViewById(R.id.etDefinition);
            String front = etFront.getText().toString().trim();
            String back = etBack.getText().toString().trim();
            if (!front.isEmpty() && !back.isEmpty()) {
                String flashcardId = i < flashCards.size() ? flashCards.get(i).getId() : UUID.randomUUID().toString();
                updatedFlashCards.add(new FlashCard(front, back, flashcardId));
            }
        }

        if (updatedFlashCards.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất một flashcard hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> studySet = new HashMap<>();
        studySet.put("title", setTitle);
        studySet.put("termCount", updatedFlashCards.size());
        studySet.put("username", mAuth.getCurrentUser().getDisplayName() != null && !mAuth.getCurrentUser().getDisplayName().isEmpty() ?
                mAuth.getCurrentUser().getDisplayName() : "Unknown");
        studySet.put("accuracy", 0.0);

        db.collection("users").document(userId).collection("study_sets").document(setId)
                .set(studySet, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(userId).collection("study_sets")
                            .document(setId).collection("flashcards")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().delete();
                                }
                                for (FlashCard flashCard : updatedFlashCards) {
                                    Map<String, Object> flashCardData = new HashMap<>();
                                    flashCardData.put("front", flashCard.getFront());
                                    flashCardData.put("back", flashCard.getBack());
                                    db.collection("users").document(userId)
                                            .collection("study_sets").document(setId)
                                            .collection("flashcards").document(flashCard.getId())
                                            .set(flashCardData, SetOptions.merge());
                                }
                                Toast.makeText(this, "Đã lưu bộ học", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi khi xóa flashcard cũ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu bộ học: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteStudySet() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("study_sets").document(setId)
                .collection("flashcards")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    db.collection("users").document(userId).collection("study_sets").document(setId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã xóa bộ học", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi khi xóa bộ học: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi xóa flashcard: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}