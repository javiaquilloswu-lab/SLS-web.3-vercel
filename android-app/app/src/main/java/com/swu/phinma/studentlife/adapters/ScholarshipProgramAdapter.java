package com.swu.phinma.studentlife.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.swu.phinma.studentlife.R;
import com.swu.phinma.studentlife.models.ScholarshipProgram;
import java.util.List;

public class ScholarshipProgramAdapter extends RecyclerView.Adapter<ScholarshipProgramAdapter.ViewHolder> {

    private final List<ScholarshipProgram> programList;
    private final OnProgramClickListener listener;

    public interface OnProgramClickListener {
        void onProgramClick(ScholarshipProgram program);
    }

    public ScholarshipProgramAdapter(List<ScholarshipProgram> programList, OnProgramClickListener listener) {
        this.programList = programList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scholarship_program, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScholarshipProgram program = programList.get(position);
        holder.tvProgramCode.setText(program.getCode());
        holder.tvProgramTitle.setText(program.getTitle());
        holder.tvProgramAcadYear.setText(program.getAcademicYear());
        holder.tvProgramDeadline.setText(program.getDeadline());

        if (program.isActive() && !program.isExpired()) {
            holder.badgeProgramStatus.setText("Available");
            holder.badgeProgramStatus.setBackgroundResource(R.drawable.bg_pill_active);
            holder.badgeProgramStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_green_text));
        } else {
            holder.badgeProgramStatus.setText(program.isExpired() ? "Deadline Passed" : "Inactive");
            holder.badgeProgramStatus.setBackgroundResource(R.drawable.bg_pill_rejected);
            holder.badgeProgramStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_ruby));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProgramClick(program);
            }
        });
    }

    @Override
    public int getItemCount() {
        return programList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProgramCode, tvProgramTitle, tvProgramAcadYear, tvProgramDeadline, badgeProgramStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProgramCode = itemView.findViewById(R.id.tvProgramCode);
            tvProgramTitle = itemView.findViewById(R.id.tvProgramTitle);
            tvProgramAcadYear = itemView.findViewById(R.id.tvProgramAcadYear);
            tvProgramDeadline = itemView.findViewById(R.id.tvProgramDeadline);
            badgeProgramStatus = itemView.findViewById(R.id.badgeProgramStatus);
        }
    }
}
