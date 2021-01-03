package com.example.messengerapp.homeScreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.messengerapp.allContactsScreen.ContactsActivity;
import com.example.messengerapp.authScreen.PhoneActivity;
import com.example.messengerapp.R;
import com.example.messengerapp.chatScreen.ChatActivity;
import com.example.messengerapp.interfaces.OnItemClickListener;
import com.example.messengerapp.models.Chat;
import com.example.messengerapp.models.Message;
import com.example.messengerapp.models.User;
import com.example.messengerapp.recyclerViewAdapters.ChatAdapter;
import com.example.messengerapp.recyclerViewAdapters.ContactAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Chat> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recyclerView = findViewById(R.id.recyclerViewTals);

        String userId = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("users")
                .document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.get("name") != null) {
                    Toast.makeText(HomeActivity.this, "Hello", Toast.LENGTH_SHORT).show();
                } else {
                    showAlertDialogButtonClicked();
                }
            }
        });

        initList();
        getChats();
    }

    private void initList() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new ChatAdapter(this, list);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
                intent.putExtra("chat", list.get(position));
                startActivity(intent);
            }
        });
    }

    private void getChats() {
        String userId = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("chats")
                .whereArrayContains("userIds", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            switch (change.getType()) {
                                case ADDED:
                                    setChatUsers(change);
                                    break;
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

    }

    private void setChatUsers(DocumentChange change) {
        FirebaseFirestore.getInstance().collection("chats")
                .document(change.getDocument().getId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            List<Map<String, Object>> listU = (List<Map<String, Object>>) document.get("userIds");
                            if (listU != null) {

                                String name = String.valueOf(listU.get(1));
                                FirebaseFirestore.getInstance().collection("users").document(name)
                                        .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        String uName = (String) documentSnapshot.get("name");
                                        Log.d("TAG", "onSuccess: " + uName);
                                        Chat chat = change.getDocument().toObject(Chat.class);
                                        chat.setId(change.getDocument().getId());
                                        chat.setName(uName);
                                        list.add(chat);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                                adapter.notifyDataSetChanged();
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }


    public void showAlertDialogButtonClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.user);
        builder.setTitle("Введите имя");
        builder.setMessage("Чтобы ваши друзья заметили вас");

        final View customLayout = getLayoutInflater().inflate(R.layout.custom_alert2, null);
        builder.setView(customLayout);
        builder.setPositiveButton("Сохранить меня", null);

        AlertDialog dialog = builder.show();
        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        button.setOnClickListener(view -> {
            EditText editText = customLayout.findViewById(R.id.editTextTextPersonName);
            if (editText.getText().toString().trim().isEmpty()) {
                editText.setError("Пожалуйста, введите имя.");
            } else {
                sendName(editText);
                dialog.dismiss();
            }
        });
    }


    public void sendName(EditText editName) {
        String name = editName.getText().toString().trim();
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        String userId = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .set(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Snackbar.make(findViewById(android.R.id.content), "Имя успешно добавлено.",
                                    Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Сбой связи! Перезагрузите приложение.",
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void onClick(View view) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(HomeActivity.this, PhoneActivity.class));
        finish();
    }

    public void onClickFab(View view) {
        startActivity(new Intent(HomeActivity.this, ContactsActivity.class));
    }
}