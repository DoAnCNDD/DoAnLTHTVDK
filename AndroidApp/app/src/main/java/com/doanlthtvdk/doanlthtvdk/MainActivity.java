package com.doanlthtvdk.doanlthtvdk;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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

import kotlin.Unit;


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
    public static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 11;
    private Switch switchOnOff;
    private RecyclerView recyclerHistory;
    private FirebaseRecyclerAdapter<History, HistoryVH> adapter;
    private ProgressBar progressBar;
    private ProgressBar progressBarSend;
    private Switch switchOnOffSend;
    private NetworkChangeReceiver receiver;
    private ConstraintLayout container;
    private TextView textNoInternet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        switchOnOff = findViewById(R.id.switch_on_off);
        recyclerHistory = findViewById(R.id.recycler_history);
        progressBar = findViewById(R.id.progress_bar);
        progressBarSend = findViewById(R.id.progress_bar_send);
        switchOnOffSend = findViewById(R.id.switch_on_off_send);
        container = findViewById(R.id.container);
        textNoInternet = findViewById(R.id.text_no_internet);

        setupRecycler();
        setupSwitchOnOff();
        setupSwitchOnOffSend();

        receiver = new NetworkChangeReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, MyService.class));
        } else {
            startService(new Intent(this, MyService.class));
        }
    }

    @Override protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }

        receiver.setOnNetworkChange(isConnected -> {
            TransitionManager.beginDelayedTransition(findViewById(android.R.id.content),
                    new Fade().addTarget(container).addTarget(textNoInternet));
            if (isConnected) {
                container.setVisibility(View.VISIBLE);
                textNoInternet.setVisibility(View.INVISIBLE);
            } else {
                Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show();
                container.setVisibility(View.INVISIBLE);
                textNoInternet.setVisibility(View.VISIBLE);
            }
            return Unit.INSTANCE;
        });
        registerNetworkBroadcast();
    }

    @Override protected void onStop() {
        super.onStop();

        receiver.setOnNetworkChange(null);
        unregisterNetworkChanges();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        adapter.stopListening();

        receiver.setOnNetworkChange(null);
        unregisterNetworkChanges();
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

    private void setupSwitchOnOff() {
        final DatabaseReference onOffRef = FirebaseDatabase.getInstance().getReference("on_off");
        final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (__, isChecked) -> {
            onOffRef.setValue(isChecked ? "1" : "0")
                    .addOnSuccessListener(MainActivity.this, ___ -> Toast.makeText(MainActivity.this, (isChecked ? "Bật" : "Tắt") + " thành công", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(MainActivity.this, e -> Toast.makeText(MainActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        };

        // listen value
        onOffRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean onOff = Objects.equals(dataSnapshot.getValue(String.class), "1");

                if (switchOnOff.getVisibility() == View.INVISIBLE) {
                    TransitionManager.beginDelayedTransition(findViewById(android.R.id.content),
                            new TransitionSet()
                                    .addTransition(new Fade(Fade.OUT).addTarget(progressBar))
                                    .addTransition(new Fade(Fade.IN).addTarget(switchOnOff))
                                    .setDuration(700)
                    );
                    progressBar.setVisibility(View.GONE);
                    switchOnOff.setVisibility(View.VISIBLE);
                }
                switchOnOff.setOnCheckedChangeListener(null);
                switchOnOff.setChecked(onOff);
                switchOnOff.setOnCheckedChangeListener(onCheckedChangeListener);
            }

            @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSwitchOnOffSend() {
        final DatabaseReference onOffSendRef = FirebaseDatabase.getInstance().getReference("on_off_send");
        final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (__, isChecked) -> {
            onOffSendRef.setValue(isChecked ? "1" : "0")
                    .addOnSuccessListener(MainActivity.this, ___ -> Toast.makeText(MainActivity.this, (isChecked ? "Bật" : "Tắt") + " thông báo thành công", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(MainActivity.this, e -> Toast.makeText(MainActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        };

        // listen value
        onOffSendRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean onOff = Objects.equals(dataSnapshot.getValue(String.class), "1");

                if (switchOnOffSend.getVisibility() == View.INVISIBLE) {
                    TransitionManager.beginDelayedTransition(findViewById(android.R.id.content),
                            new TransitionSet()
                                    .addTransition(new Fade(Fade.OUT).addTarget(progressBarSend))
                                    .addTransition(new Fade(Fade.IN).addTarget(switchOnOffSend))
                                    .setDuration(700)
                    );
                    progressBarSend.setVisibility(View.GONE);
                    switchOnOffSend.setVisibility(View.VISIBLE);
                }
                switchOnOffSend.setOnCheckedChangeListener(null);
                switchOnOffSend.setChecked(onOff);
                switchOnOffSend.setOnCheckedChangeListener(onCheckedChangeListener);
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

    private void registerNetworkBroadcast() {
        registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
