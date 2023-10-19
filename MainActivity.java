package com.example.setkari;

import android.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private HashMap<String, CustomerData> customerDataHashMap;

    private EditText customerNameEditText;
    private EditText pendingAmountEditText;
    private EditText dateEditText;
    private EditText areaEditText;
    private EditText workEditText;
    private ListView customerListView;
    private SharedPreferences sharedPreferences;

    private static final String SHARED_PREF_NAME = "CustomerData";
    private static final String[] WORK_OPTIONS = {"nangar", "roter", "trolly"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customerDataHashMap = new HashMap<>();
        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        customerNameEditText = findViewById(R.id.customerName);
        pendingAmountEditText = findViewById(R.id.pendingAmount);
        dateEditText = findViewById(R.id.date);
        areaEditText = findViewById(R.id.area);
        workEditText = findViewById(R.id.work);
        customerListView = findViewById(R.id.customerList);

        retrieveCustomerData();
        updateCustomerListView();
    }
    public void showDatePickerDialog(View view) {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Do something with the selected date
                String selectedDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                dateEditText.setText(selectedDate);
            }
        };

        // Get the current date from the dateEditText field, if available
        String currentDate = dateEditText.getText().toString();
        Calendar calendar = Calendar.getInstance();
        if (!currentDate.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date parsedDate = dateFormat.parse(currentDate);
                calendar.setTime(parsedDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Create a DatePickerDialog with the current date set as the default date
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    public void addOrUpdateCustomer(View view) {
        String customerName = customerNameEditText.getText().toString().trim();
        String pendingAmountText = pendingAmountEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        String area = areaEditText.getText().toString().trim();
        String work = workEditText.getText().toString().trim();

        if (customerName.isEmpty() || pendingAmountText.isEmpty() || date.isEmpty() || area.isEmpty() || work.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double pendingAmount = Double.parseDouble(pendingAmountText);

        // Check if the customer already exists in the HashMap
        CustomerData customerData = customerDataHashMap.get(customerName);
        if (customerData != null) {
            // Customer already exists, add a new record for the customer
            customerData.addRecord(pendingAmount, date, area, work);
        } else {
            // Customer does not exist, create a new customer entry
            customerData = new CustomerData(customerName, pendingAmount, date, area, work);
            customerDataHashMap.put(customerName, customerData);
        }

        saveCustomerData();

        Toast.makeText(this, "Customer record added/updated", Toast.LENGTH_SHORT).show();

        // Clear all input fields after adding/updating customer
        customerNameEditText.setText("");
        pendingAmountEditText.setText("");
        dateEditText.setText("");
        areaEditText.setText("");
        workEditText.setText("");

        updateCustomerListView();
    }
    public void showWorkOptionsDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Work")
                .setItems(WORK_OPTIONS, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position of the selected item
                        String selectedWorkOption = WORK_OPTIONS[which];
                        workEditText.setText(selectedWorkOption);
                    }
                });
        builder.create().show();
    }

    private void retrieveCustomerData() {
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("customer_")) {
                String customerName = entry.getKey().replace("customer_", "");
                String data = entry.getValue().toString();
                String[] parts = data.split(",");
                if (parts.length == 4) {
                    double pendingAmount = Double.parseDouble(parts[0]);
                    String date = parts[1];
                    String area = parts[2];
                    String work = parts[3];
                    CustomerData customerData = new CustomerData(customerName, pendingAmount, date, area, work);
                    customerDataHashMap.put(customerName, customerData);
                }
            }
        }
    }

    private void saveCustomerData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (CustomerData customerData : customerDataHashMap.values()) {
            String key = "customer_" + customerData.getName();
            String value = customerData.getPendingAmount() + "," + customerData.getDate() + "," +
                    customerData.getArea() + "," + customerData.getWork();
            editor.putString(key, value);
        }
        editor.apply();
    }

    private void updateCustomerListView() {
        ArrayList<String> customerRecords = new ArrayList<>();
        for (CustomerData customerData : customerDataHashMap.values()) {
            String record = "Name: " + customerData.getName() +
                    ", Pending Amount: $" + customerData.getPendingAmount() +
                    ", Date: " + customerData.getDate() +
                    ", Area: " + customerData.getArea() +
                    ", Work: " + customerData.getWork();
            customerRecords.add(record);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                customerRecords
        );

        customerListView.setAdapter(adapter);
    }
}

class CustomerData {
    private String name;
    private List<Record> records;

    public CustomerData(String name, double pendingAmount, String date, String area, String work) {
        this.name = name;
        this.records = new ArrayList<>();
        addRecord(pendingAmount, date, area, work);
    }

    public void addRecord(double pendingAmount, String date, String area, String work) {
        Record record = new Record(pendingAmount, date, area, work);
        records.add(record);
    }

    public String getName() {
        return name;
    }

    public List<Record> getRecords() {
        return records;
    }

    public double getPendingAmount() {
        if (records.isEmpty()) {
            return 0;
        }
        // Sum up pending amounts from all records
        double totalPendingAmount = 0;
        for (Record record : records) {
            totalPendingAmount += record.getPendingAmount();
        }
        return totalPendingAmount;
    }

    public String getDate() {
        if (records.isEmpty()) {
            return "";
        }
        // Return the date from the first record
        return records.get(0).getDate();
    }

    public String getArea() {
        if (records.isEmpty()) {
            return "";
        }
        // Return the area from the first record
        return records.get(0).getArea();
    }

    public String getWork() {
        if (records.isEmpty()) {
            return "";
        }
        // Return the work from the first record
        return records.get(0).getWork();
    }
}

class Record {
    private double pendingAmount;
    private String date;
    private String area;
    private String work;

    public Record(double pendingAmount, String date, String area, String work) {
        this.pendingAmount = pendingAmount;
        this.date = date;
        this.area = area;
        this.work = work;
    }

    public double getPendingAmount() {
        return pendingAmount;
    }

    public String getDate() {
        return date;
    }

    public String getArea() {
        return area;
    }

    public String getWork() {
        return work;
    }
}
