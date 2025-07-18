package com.example.quizzy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class StudySetAdapter extends RecyclerView.Adapter<StudySetAdapter.StudySetViewHolder> {
    private List<StudySet> studySets;
    private Consumer<String> onClick;
    private Consumer<String> onEditClick;

    public StudySetAdapter(List<StudySet> studySets, Consumer<String> onClick, Consumer<String> onEditClick) {
        this.studySets = studySets;
        this.onClick = onClick;
        this.onEditClick = onEditClick;
    }

    @NonNull
    @Override
    public StudySetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_set, parent, false);
        return new StudySetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudySetViewHolder holder, int position) {
        StudySet item = studySets.get(position);
        holder.bind(item, onClick, onEditClick);
    }

    @Override
    public int getItemCount() {
        return studySets.size();
    }

    static class StudySetViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, termCountTextView, avatarTextView, usernameTextView, accuracyTextView;
        ImageView editIcon;

        public StudySetViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.setTitle);
            termCountTextView = itemView.findViewById(R.id.setTermCount);
            avatarTextView = itemView.findViewById(R.id.avatarTexView);
            usernameTextView = itemView.findViewById(R.id.username);
            editIcon = itemView.findViewById(R.id.editIcon);
        }

        public void bind(StudySet studySet, Consumer<String> onClick, Consumer<String> onEditClick) {
            titleTextView.setText(studySet.getTitle());
            termCountTextView.setText(studySet.getTermCount() + " terms");
            avatarTextView.setText(studySet.getAvatarLetter());
            usernameTextView.setText(studySet.getUsername());

            itemView.setOnClickListener(v -> onClick.accept(studySet.getId()));
            editIcon.setOnClickListener(v -> onEditClick.accept(studySet.getId()));
        }
    }
}