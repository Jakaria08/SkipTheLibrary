package com.stl.skipthelibrary.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;

import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.stl.skipthelibrary.BindersAndAdapters.BookRecyclerAdapter;
import com.stl.skipthelibrary.DatabaseAndAPI.DatabaseHelper;
import com.stl.skipthelibrary.Entities.Book;
import com.stl.skipthelibrary.Entities.RequestHandler;
import com.stl.skipthelibrary.Entities.User;
import com.stl.skipthelibrary.Enums.BookStatus;
import com.stl.skipthelibrary.Enums.HandoffState;
import com.stl.skipthelibrary.R;
import com.stl.skipthelibrary.Singletons.CurrentUser;


public class ViewBookActivity extends AppCompatActivity {
    final public static String TAG = "ViewBookActivityTag";
    private DatabaseHelper databaseHelper;

    //Book Description Elements & Fields
    private Book book;
    private User user;
    private EditText title_element;
    private EditText author_element;
    private RatingBar rating_element;
    private EditText synopsis_element;
    private ImageButton edit_button;
    private ImageButton save_button;
    private ViewStub stub;
    private View inflated;
    private ChildEventListener childEventListener;


    //Owner Requested Fields


    //Owner Handoff Elements & Fields
    private Button button;
    private View view;
    private String isbn_code;

    //Owner Return Elements & Fields


    //Borrower Request Elements & Fields


    //Borrower Handoff Elements & Fields


    //Borrower Return Elements & Fields


    //Pending Screen Elements & Fields


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Book Description
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.book_details);
        databaseHelper = new DatabaseHelper(this);
        stub = findViewById(R.id.generic_bottom_screen_id);
        user = CurrentUser.getInstance();
        bindBookDescriptionElements();
        getIncomingIntents();
    }

    /**
     * This method catches the incoming data (BookUUID) that is sent via an intent on screen switch.
     */
    private void getIncomingIntents() {
        String bookID = getIntent().getExtras().getString(BookRecyclerAdapter.BOOK_ID);

        childEventListener = databaseHelper.getDatabaseReference()
                .child("Books").orderByChild("uuid").equalTo(bookID)
                .addChildEventListener(new ChildEventListener() {
                    /**
                     * When a new child is added add it to the list of books
                     * @param dataSnapshot: the current snapshot
                     * @param s: the ID
                     */
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        book = dataSnapshot.getValue(Book.class);
                        handleBookArrival();
                    }

                    /**
                     * When a child is changes update them
                     * @param dataSnapshot: the current snapshot
                     * @param s: the ID
                     */
                    @Override
                    //TODO:Make books edit on the go
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Toast.makeText(ViewBookActivity.this, "This book has been modified.",
                                Toast.LENGTH_SHORT).show();
                        ViewBookActivity.this.finish();

                    }

                    /**
                     * If a child is deleted delete them from the list of our books
                     * @param dataSnapshot: the current snapshot
                     */
                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                        Toast.makeText(ViewBookActivity.this,
                                "The book you're looking at has been deleted.", Toast.LENGTH_SHORT).show();
                        ViewBookActivity.this.finish();
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
    }

    private void handleBookArrival() {

        fillBookDescriptionFields();

        //If user is owner of book, allow for edittability
        if (user.getUserName().equals(book.getOwnerUserName())) {
            edit_button.setVisibility(View.VISIBLE);
            save_button.setVisibility(View.GONE);

            edit_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBookDescriptionFieldsEditable(true);
                    save_button.setVisibility(View.VISIBLE);
                    edit_button.setVisibility(View.GONE);
                }
            });

            save_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBookDescriptionFieldsEditable(false);
                    save_button.setVisibility(View.GONE);
                    edit_button.setVisibility(View.VISIBLE);
                    updateBookDesriptionFields();
                }
            });
        } else {
            edit_button.setVisibility(View.GONE);
            save_button.setVisibility(View.GONE);
        }
        selectBottom();
    }

    /**
     * Get the book status and change the book state and book hand off state when the
     * hand off is over
     */
    private void selectBottom() {
        BookStatus bookStatus = book.getRequests().getState().getBookStatus();
        HandoffState bookHandoffState = book.getRequests().getState().getHandoffState();

        if (user.getUserName().equals(book.getOwnerUserName())) {//user is owner
            if (bookStatus == BookStatus.REQUESTED) {
                setBottomScreen(R.layout.bookscreen_owner_requested);
                configureOwnerRequested();
            } else if (bookHandoffState == HandoffState.READY_FOR_PICKUP) {
                setBottomScreen(R.layout.bookscreen_owner_handoff);
                configureOwnerHandOff();
            } else if (bookHandoffState == HandoffState.BORROWER_RETURNED) {
                setBottomScreen(R.layout.bookscreen_owner_return);
                configureOwnerReturn();
            } else {
                setBottomScreen(R.layout.bookscreen_pending);
                configureOwnerPending();
            }
        } else {//user is borrower
            if ((!book.userIsInterested(user.getUserName()) && bookStatus == BookStatus.REQUESTED) ||
                    bookStatus == BookStatus.AVAILABLE) {
                setBottomScreen(R.layout.bookscreen_borrower_request);
                configureBorrowerRequest();
            } else if (bookHandoffState == HandoffState.OWNER_LENT) {
                setBottomScreen(R.layout.bookscreen_borrower_handoff);
                configureBorrowerHandoff();
            } else if (bookHandoffState == HandoffState.BORROWER_RECEIVED) {
                setBottomScreen(R.layout.bookscreen_borrower_return);
                configureBorrowerReturn();
            } else {
                setBottomScreen(R.layout.bookscreen_pending);
                configureBorrowerPending();
            }
        }
    }

    private void setBottomScreen(int resourcefile) {
        stub.setLayoutResource(resourcefile);
        inflated = stub.inflate();
    }

    /**
     * Borrower request the book
     */
    private void configureBorrowerRequest() {
        Button button = inflated.findViewById(R.id.requestButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestHandler handler = book.getRequests();
                handler.addRequestor(user.getUserName());
                handler.getState().setBookStatus(BookStatus.REQUESTED);
                databaseHelper.updateBook(book);
            }
        });
    }

    /**
     * Borrower get the book and scan the book to confirm the book is borrowed
     */
    private void configureBorrowerHandoff() {
        inflated.findViewById(R.id.borrowButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewBookActivity.this, ScannerActivity.class);
                startActivityForResult(intent, ScannerActivity.SCAN_BOOK);
            }

        });

    }


    /**
     * Borrower scan the book and return the book
     */
    private void configureBorrowerReturn() {
        inflated.findViewById(R.id.returnButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewBookActivity.this, ScannerActivity.class);
                startActivityForResult(intent, ScannerActivity.SCAN_BOOK);
            }

        });
    }

    //Pending Screen
    private void configureBorrowerPending() {

    }

    //Owner Requested
    private void configureOwnerRequested() {

    }


    /**
     * Owner scan the book, and lend the book
     */
    private void configureOwnerHandOff() {
        inflated.findViewById(R.id.lendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewBookActivity.this, ScannerActivity.class);
                startActivityForResult(intent, ScannerActivity.SCAN_BOOK);
            }

        });
    }

    /**
     * Owner get the book and scan the book to confirm the book is returned
     */
    private void configureOwnerReturn() {
        inflated.findViewById(R.id.returnedButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewBookActivity.this, ScannerActivity.class);
                startActivityForResult(intent, ScannerActivity.SCAN_BOOK);
            }

        });
    }

    //Owner Pending
    private void configureOwnerPending() {

    }

    private void bindBookDescriptionElements() {
        title_element = findViewById(R.id.title_element);
        author_element = findViewById(R.id.author_element);
        rating_element = findViewById(R.id.rating_bar_element);
        synopsis_element = findViewById(R.id.synopsis_element);
        edit_button = findViewById(R.id.edit_button);
        save_button = findViewById(R.id.save_button);

        save_button.setVisibility(View.GONE);
        edit_button.setVisibility(View.GONE);
        setBookDescriptionFieldsEditable(false);
    }

    private void fillBookDescriptionFields(){
        title_element.setText(book.getDescription().getTitle());
        author_element.setText(book.getDescription().getAuthor());
        rating_element.setMax(book.getRating().getMaxRating());
        rating_element.setNumStars((int) Math.round(book.getRating().getAverageRating()));
        synopsis_element.setText(book.getDescription().getSynopsis());
    }

    private void setBookDescriptionFieldsEditable(Boolean isEditable) {
        if (isEditable) {
            title_element.setEnabled(true);
            author_element.setEnabled(true);
            rating_element.setEnabled(false);
            synopsis_element.setEnabled(true);
        } else {
            title_element.setEnabled(false);
            author_element.setEnabled(false);
            rating_element.setEnabled(false);
            synopsis_element.setEnabled(false);
        }
    }

    private void updateBookDesriptionFields(){
        book.getDescription().setTitle(title_element.getText().toString());
        book.getDescription().setAuthor(author_element.getText().toString());
        book.getDescription().setSynopsis(synopsis_element.getText().toString());
        book.getRating().addRating((double) rating_element.getNumStars());
        databaseHelper.updateBook(book);
    }


    //Owner Requested


    //Owner Return and Owner Handoff
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ScannerActivity.SCAN_BOOK) {
            if (resultCode == RESULT_OK) {
                isbn_code = data.getStringExtra("ISBN");

                if (isbn_code.equals(book.getISBN()) && CurrentUser.getInstance().getUserName().equals(book.getOwnerUserName())){
                    switch (book.getRequests().getState().getHandoffState()) {
                        case READY_FOR_PICKUP:
                            book.getRequests().lendBook();
                            Toast.makeText(this, "The Book is Lent", Toast.LENGTH_SHORT).show();
                            databaseHelper.updateBook(book);
                            break;
                        case BORROWER_RETURNED:
                            book.getRequests().confirmReturned();
                            Toast.makeText(this, "The Book is Returned", Toast.LENGTH_SHORT).show();
                            databaseHelper.updateBook(book);
                            break;
                    }
                }else if(isbn_code.equals(book.getISBN()) && !CurrentUser.getInstance().getUserName().equals(book.getOwnerUserName())){
                    switch (book.getRequests().getState().getHandoffState()) {
                        case OWNER_LENT:
                            book.getRequests().confirmBorrowed();
                            Toast.makeText(this, "The Book is Borrowed", Toast.LENGTH_SHORT).show();
                            databaseHelper.updateBook(book);
                            break;
                        case BORROWER_RECEIVED:
                            book.getRequests().returnBook();
                            Toast.makeText(this, "The Book is Returned", Toast.LENGTH_SHORT).show();
                            databaseHelper.updateBook(book);
                            break;
                    }
                }else{
                    Toast.makeText(this, "Scanning ISBN does not Match the Book ISBN", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Book ISBN not match the scanning ISBN");
                }

            } else {
                Log.d(TAG, "onActivityResult: Something went wrong in scan");
            }
        }
    }


    //Borrower Request


    //Borrower Handoff


    //Borrower Return


    //Pending Screen


    @Override
    public void finish() {
        if (childEventListener!=null){
            databaseHelper.getDatabaseReference().child("Books").orderByChild("uuid")
                    .equalTo(getIntent().getExtras().getString("bookUUID"))
                    .removeEventListener(childEventListener);
        }
        super.finish();
    }
}