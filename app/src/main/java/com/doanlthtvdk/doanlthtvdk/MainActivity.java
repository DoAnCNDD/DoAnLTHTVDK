package com.doanlthtvdk.doanlthtvdk;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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


class HistoryVH extends RecyclerView.ViewHolder {
    final DateFormat dateFormat = new SimpleDateFormat("HH:mm, dd/MM/yyyy");
    final ImageView imageStatus;
    final ImageView imageDelete;
    final TextView textTime;
    final TextView textStatus;

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
        imageDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Delete history")
                        .setMessage("Are you sure?")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                FirebaseDatabase.getInstance()
                                        .getReference("histories/" + history.id)
                                        .removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(itemView.getContext(), "Delete successfully", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(itemView.getContext(), "Delete failure: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        })
                        .show();
            }
        });
    }
}

public class MainActivity extends AppCompatActivity {
    private Switch switchOnOff;
    private RecyclerView recyclerHistory;
    private FirebaseRecyclerAdapter<History, HistoryVH> adapter;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        switchOnOff = findViewById(R.id.switch_on_off);
        recyclerHistory = findViewById(R.id.recycler_history);

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
                .setQuery(query, new SnapshotParser<History>() {
                    @NonNull
                    @Override
                    public History parseSnapshot(@NonNull DataSnapshot snapshot) {
                        final History history = snapshot.getValue(History.class);
                        Objects.requireNonNull(history).id = snapshot.getKey();
                        return history;
                    }
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
                Log.d("###", "bind " + history);
                historyVH.bind(history);
            }
        };
        recyclerHistory.setAdapter(adapter);
        adapter.startListening();
    }

    private void setupSwitch() {
        final DatabaseReference onOffRef = FirebaseDatabase.getInstance().getReference("on_off");
        onOffRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean onOff = (boolean) dataSnapshot.getValue();
                switchOnOff.setChecked(onOff);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        switchOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                onOffRef.setValue(isChecked)
                        .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(MainActivity.this, (isChecked ? "Bật" : "Tắt") + " thành công", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(MainActivity.this, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
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

        return super.onOptionsItemSelected(item);
    }
}
