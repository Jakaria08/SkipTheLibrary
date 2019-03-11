package com.stl.skipthelibrary;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.NativeActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.robotium.solo.Solo;
import com.stl.skipthelibrary.Activities.AddBooksActivity;
import com.stl.skipthelibrary.Activities.BorrowersBooksActivity;
import com.stl.skipthelibrary.Activities.LoginActivity;
import com.stl.skipthelibrary.Activities.MapBoxActivity;
import com.stl.skipthelibrary.Activities.MyBooksActivity;
import com.stl.skipthelibrary.Activities.NotificationActivity;
import com.stl.skipthelibrary.Activities.ProfileActivity;
import com.stl.skipthelibrary.Activities.ScannerActivity;
import com.stl.skipthelibrary.Activities.SearchActivity;
import com.stl.skipthelibrary.Activities.ViewBookActivity;
import com.stl.skipthelibrary.DatabaseAndAPI.DatabaseHelper;
import com.stl.skipthelibrary.Entities.Book;
import com.stl.skipthelibrary.Entities.BookDescription;
import com.stl.skipthelibrary.Entities.Notification;
import com.stl.skipthelibrary.Entities.Rating;
import com.stl.skipthelibrary.Entities.RequestHandler;
import com.stl.skipthelibrary.Entities.State;
import com.stl.skipthelibrary.Helpers.NavigationHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertTrue;

public class US060101Test extends ActivityTestRule<ViewBookActivity> {
    private Solo solo;
    private BottomNavigationView view;
    private UITestHelper uiTestHelper;
    private static final String isbn = "1234567890123";
    private static final String bookTitle = "Felix";
    private static final String ownerEmail = "Felix@gmail.com";
    private static final String ownerPassword = "123456";
    private static final String borrowerEmail = "Felix2@gmail.com";
    private static final String borrowerPassword = "123456";

    public US060101Test() throws InterruptedException {
        super(ViewBookActivity.class, false, false);
        RequestHandler requests = new RequestHandler(new State());
        BookDescription book1Description = new BookDescription(bookTitle, "Test book", "Test Author", new Rating());
        Book book1 = new Book(isbn, book1Description, "Felix", requests, null, null);

        ArrayList<Book> books = new ArrayList<>();
        books.add(book1);
        uiTestHelper = new UITestHelper(true, true, books);
    }

    private class MockScanner extends ScannerActivity {
        private String isbn;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Intent intent = new Intent();
            intent.putExtra("ISBN", isbn);
            setResult(Activity.RESULT_OK, intent);
            finish();
            return;
        }

        @Override
        public void finish() {
            super.finish();
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }
    }


    @Rule
    public ActivityTestRule<LoginActivity> rule =
            new ActivityTestRule<>(LoginActivity.class, false, true);

    @Before
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), rule.getActivity());
    }

    @Test
    public void testScanningFlow() throws Exception {

        /**
         * Test Login functionality
         */
        logInAccount("Felix@gmail.com", "123456");


        /**
         * Test AddBook functionality
         */

        enterMyBookActivity();

        solo.clickOnView(solo.getView(R.id.addBookButton));

        solo.assertCurrentActivity("Wrong Activity", AddBooksActivity.class);
        solo.enterText((EditText) solo.getView(R.id.AddBookTitle), "Felix");
        solo.enterText((EditText) solo.getView(R.id.AddBookAuthor), "Felix");
        solo.enterText((EditText) solo.getView(R.id.AddBookISBN), isbn);
        solo.enterText((EditText) solo.getView(R.id.AddBookDesc), "Felix");
        solo.clickOnView(solo.getView(R.id.SaveBookButton));

        viewBookFromMybookActivity();

        solo.goBack();

        /**
         * log out
         */
        logOutAccount();

        /**
         * Goes into another account
         */
        logInAccount("Felix2@gmail.com", "123456");

        view = (BottomNavigationView) solo.getView(R.id.bottom_navigation);
        view.setOnNavigationItemSelectedListener(new NavigationHandler(view.getContext()));
        solo.clickOnView(view.findViewById(R.id.borrow));
        solo.assertCurrentActivity("Wrong Activity", BorrowersBooksActivity.class);

        solo.clickOnView(solo.getView(R.id.searchBookButton));
        solo.waitForActivity(SearchActivity.class);
        solo.assertCurrentActivity("Wrong Activity", SearchActivity.class);
        solo.enterText((AutoCompleteTextView) solo.getView(R.id.SearchBar), "Felix");
        assertTrue(solo.waitForText("Felix", 2, 2000));


        RecyclerView searchBooksList = (RecyclerView) solo.getView(R.id.SearchRecyclerView);
        solo.clickOnView(searchBooksList.getChildAt(0));
        solo.assertCurrentActivity("Wrong Activity", ViewBookActivity.class);
        solo.clickOnView(solo.getView(R.id.requestButton));
        solo.assertCurrentActivity("Wrong Activity", SearchActivity.class);
        solo.goBack();
        logOutAccount();


        /**
         * login Book Owner Account
         */
        logInAccount("Felix@gmail.com", "123456");
        enterMyBookActivity();
        viewBookFromMybookActivity();
        RecyclerView requstedBookList = (RecyclerView) solo.getView(R.id.RequesterRecyclerView);
        solo.clickOnView(requstedBookList.getChildAt(0).findViewById(R.id.approve_button_id));
        solo.assertCurrentActivity("Wrong Activity", MapBoxActivity.class);
        solo.clickOnView(solo.getView(R.id.select_location_submit));
        solo.waitForActivity(MyBooksActivity.class);

        viewBookFromMybookActivity();
//        Intent resultData = new Intent();
//        resultData.putExtra("ISBN", isbn);
//        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
//        intending(hasComponent(ScannerActivity.class.getName())).respondWith(result);
//        Intent intent = new Intent(ViewBookActivity.this, ScannerActivity.class);
        solo.clickOnView(solo.getView(R.id.lendButton));
//        solo.sleep(50000);

//        viewBookFromMybookActivity();
//        solo.goBack();

//        viewBookFromMybookActivity();
//        solo.assertCurrentActivity("Wrong Activity", ScannerActivity.class);

//       deleteBook();
    }

    public void deleteBook() {
        RecyclerView myBooksList = (RecyclerView) solo.getView(R.id.ownerBooksRecyclerView);
        View bookToDelete = myBooksList.getChildAt(0);

        int[] location = new int[2];

        bookToDelete.getLocationInWindow(location);

        int fromX = location[0] + 800;
        int fromY = location[1];

        int toX = location[0];
        int toY = fromY;

        solo.drag(fromX, toX, fromY, toY, 10);
        solo.sleep(1000);

//        assertTrue(myBooksList.getAdapter().getItemCount() == 0);

    }


    public void logOutAccount(){
        /**
         * log out
         */
        view = (BottomNavigationView)solo.getView(R.id.bottom_navigation);
        view.setOnNavigationItemSelectedListener(new NavigationHandler(view.getContext()));
        solo.clickOnView(view.findViewById(R.id.profile));
        solo.assertCurrentActivity("Wrong Activity", ProfileActivity.class);

        solo.clickOnView(solo.getView(R.id.logoutButton));
        solo.assertCurrentActivity("Wrong Activity", LoginActivity.class);

    }

    public void logInAccount(String email, String password){
        solo.assertCurrentActivity("Wrong Activity", LoginActivity.class);
        solo.enterText((EditText) solo.getView(R.id.EmailEditText), email);
        solo.enterText((EditText) solo.getView(R.id.PasswordEditText), password);
        solo.clickOnView(solo.getView(R.id.SignInButton));
    }

    public void enterMyBookActivity(){
        view = (BottomNavigationView)solo.getView(R.id.bottom_navigation);
        view.setOnNavigationItemSelectedListener(new NavigationHandler(view.getContext()));
        solo.clickOnView(view.findViewById(R.id.my_books));
        solo.assertCurrentActivity("Wrong Activity", MyBooksActivity.class);
    }

    public void viewBookFromMybookActivity(){
        solo.assertCurrentActivity("Wrong Activity", MyBooksActivity.class);
        RecyclerView myBooksList = (RecyclerView) solo.getView(R.id.ownerBooksRecyclerView);
        solo.clickOnView(myBooksList.getChildAt(0).findViewById(R.id.BookListItemRightArrow));
        solo.assertCurrentActivity("Wrong Activity", ViewBookActivity.class);
    }
}