package com.example.quizzy;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudySetModel extends ViewModel {
    private final MutableLiveData<List<StudySet>> studySets = new MutableLiveData<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public StudySetModel() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        loadStudySets();
    }

    public LiveData<List<StudySet>> getStudySets() {
        return studySets;
    }

    private void loadStudySets() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("study_sets")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        studySets.setValue(new ArrayList<>());
                        return;
                    }

                    List<StudySet> studySetList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        Long termCount = doc.getLong("termCount");
                        String username = doc.getString("username");

                        studySetList.add(new StudySet(
                                id,
                                title != null ? title : "Untitled",
                                termCount != null ? termCount.intValue() : 0,
                                username != null && !username.isEmpty() ? username : "Unknown"
                        ));
                    }
                    studySets.setValue(studySetList);
                });
    }
}