package com.doanlthtvdk.doanlthtvdk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;


class HistoryVH extends RecyclerView.ViewHolder {
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm, dd/MM/yyyy");
    private final ImageView imageStatus;
    private final ImageView imageDelete;
    private final TextView textTime;
    private final TextView textStatus;

    public HistoryVH(@NonNull View itemView) {
        super(itemView);
        imageDelete = itemView.findViewById(R.id.image_delete);
        imageStatus = itemView.findViewById(R.id.image_status);
        textTime = itemView.findViewById(R.id.text_time);
        textStatus = itemView.findViewById(R.id.text_status);
    }

    public void bind(final History history) {
        textTime.setText(dateFormat.format(new Date(history.time)));
        textStatus.setText(history.verified ? "Verified" : "Not verified");
        imageStatus.setImageResource(history.verified ? R.drawable.ic_done_black_24dp : R.drawable.ic_sms_failed_black_24dp);
        imageDelete.setOnClickListener(__ -> {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Delete history")
                    .setMessage("Are you sure?")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        FirebaseDatabase.getInstance()
                                .getReference("histories/" + history.id)
                                .removeValue()
                                .addOnSuccessListener(___ -> Toast.makeText(itemView.getContext(), "Delete successfully", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(itemView.getContext(), "Delete failure: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .show();
        });
    }
}

public class MainActivity extends AppCompatActivity {
    private Switch switchOnOff;
    private RecyclerView recyclerHistory;
    private FirebaseRecyclerAdapter<History, HistoryVH> adapter;
    private FloatingActionButton fab;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });

        switchOnOff = findViewById(R.id.switch_on_off);
        recyclerHistory = findViewById(R.id.recycler_history);
        progressBar = findViewById(R.id.progress_bar);

        setupRecycler();
        setupSwitch();

        recyclerHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    fab.hide();
                } else if (dy < 0) {
                    fab.show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.stopListening();
    }

    private void setupRecycler() {
        recyclerHistory.setHasFixedSize(true);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, true);
        layoutManager.setStackFromEnd(true);
        recyclerHistory.setLayoutManager(layoutManager);

        final Query query = FirebaseDatabase.getInstance()
                .getReference("histories")
                .orderByChild("time");

        final FirebaseRecyclerOptions<History> options = new FirebaseRecyclerOptions.Builder<History>()
                .setQuery(query, snapshot -> {
                    final History history = snapshot.getValue(History.class);
                    Objects.requireNonNull(history).id = snapshot.getKey();
                    return history;
                })
                .build();
        adapter = new FirebaseRecyclerAdapter<History, HistoryVH>(options) {

            @NonNull
            @Override
            public HistoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new HistoryVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.history_recycler_item, parent, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull HistoryVH historyVH, int i, @NonNull History history) {
                historyVH.bind(history);
            }
        };
        recyclerHistory.setAdapter(adapter);
        adapter.startListening();
    }

    private void setupSwitch() {
        final DatabaseReference onOffRef = FirebaseDatabase.getInstance().getReference("on_off");

        // get initial value
        onOffRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean onOff = (boolean) dataSnapshot.getValue();
                if (switchOnOff.getVisibility() == View.INVISIBLE) {
                    TransitionManager.beginDelayedTransition(findViewById(android.R.id.content),
                            new TransitionSet()
                                    .addTransition(new Fade(Fade.OUT).addTarget(progressBar))
                                    .addTransition(new Fade(Fade.IN).addTarget(switchOnOff))
                    );
                    progressBar.setVisibility(View.GONE);
                    switchOnOff.setVisibility(View.VISIBLE);
                }
                switchOnOff.setChecked(onOff);

                // Listener change value after get initial value
                switchOnOff.setOnCheckedChangeListener((__, isChecked) -> {
                    onOffRef.setValue(isChecked)
                            .addOnSuccessListener(MainActivity.this, ___ -> Toast.makeText(MainActivity.this, (isChecked ? "Bật" : "Tắt") + " thành công", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(MainActivity.this, e -> Toast.makeText(MainActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_delete) {
            deleteAll();
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("Delete all histories")
                .setMessage("Are you sure?. This action cannot be undone")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    FirebaseDatabase
                            .getInstance()
                            .getReference("histories")
                            .removeValue()
                            .addOnSuccessListener(__ -> Toast.makeText(MainActivity.this, "Delete successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Delete failure: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .show();
    }
}
