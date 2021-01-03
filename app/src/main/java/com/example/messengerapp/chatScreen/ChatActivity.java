
package com.example.messengerapp.chatScreen;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.icu.util.Freezable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.messengerapp.R;
import com.example.messengerapp.allContactsScreen.ContactsActivity;
import com.example.messengerapp.homeScreen.HomeActivity;
import com.example.messengerapp.interfaces.OnItemClickListener;
import com.example.messengerapp.models.Chat;
import com.example.messengerapp.models.Message;
import com.example.messengerapp.models.User;
import com.example.messengerapp.recyclerViewAdapters.ContactAdapter;
import com.example.messengerapp.recyclerViewAdapters.MessageAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.base.FinalizableWeakReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private User user;
    private Chat chat;
    private EditText editTextSms;

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> list = new ArrayList<>();
    private Message message;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        recyclerView = findViewById(R.id.recyclerViewChat);

        editTextSms = findViewById(R.id.edit_sms);
        user = (User) getIntent().getSerializableExtra("user");
        chat = (Chat) getIntent().getSerializableExtra("chat");

        if (chat == null) {
            chat = new Chat();
            ArrayList<String> userIds = new ArrayList<>();
            userIds.add(FirebaseAuth.getInstance().getUid()); // а это я
            userIds.add(user.getId()); // собеседник
            chat.setUserIds(userIds);
            chat.setSender(FirebaseAuth.getInstance().getUid());
            chat.setRecipient(user.getId());
            chat.setName(user.getName());

        } else {
            initList();
            getMessages();
        }
        setTitle(chat.getName());

    }

    private void getMessages() {
        FirebaseFirestore.getInstance().collection("chats").document(chat.getId())
                .collection("messages")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            switch (change.getType()) {
                                case ADDED:
                                    list.add(change.getDocument().toObject(Message.class));
                                    break;
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void initList() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new MessageAdapter(this, list);
        recyclerView.setAdapter(adapter);
    }

    public void onClickSend(View view) {
        String message = editTextSms.getText().toString().trim();
        if (chat.getId() != null) {
            sendMessage(message);
        } else {
            createChat(message);
        }
    }

    private void createChat(final String message) {
        FirebaseFirestore.getInstance().collection("chats").add(chat)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        chat.setId(documentReference.getId());
                        sendMessage(message);
                    }
                });
    }

    private void sendMessage(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", message);

        FirebaseFirestore.getInstance().collection("chats")
                .document(chat.getId())
                .collection("messages")
                .add(map);
    }
}