package com.example.ibnsina;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<InventoryModel> inventoryList;
    private EditText etSearch;
    private Spinner spinnerFilter;
    private String selectedFilter = "All";
    private MaterialButton btnResetAll, btnRefreshManual, btnPrint, btnCalculator;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseHelper dbHelper;

    private boolean loadingState = false;

    // Calculator Variables
    private String currentInput = "";
    private double result = 0;
    private char lastOperator = ' ';

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        etSearch = findViewById(R.id.etSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        btnResetAll = findViewById(R.id.btnResetAll);
        btnRefreshManual = findViewById(R.id.btnRefreshManual);
        btnPrint = findViewById(R.id.btnPrint); 
        btnCalculator = findViewById(R.id.btnCalculator);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setHasFixedSize(true);
        }

        inventoryList = new ArrayList<>();

        String[] options = {"All", "Checked", "Unchecked", "In Stock", "Stock Out", "PHARMA", "OPTHALMIC", "HERBAL"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        if (spinnerFilter != null) spinnerFilter.setAdapter(spinnerAdapter);

        syncOfflineData();
        fetchData(true);

        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setOnRefreshListener(() -> { syncOfflineData(); fetchData(true); });

        if (btnRefreshManual != null)
            btnRefreshManual.setOnClickListener(v -> { syncOfflineData(); fetchData(true); });

        if (btnPrint != null) btnPrint.setOnClickListener(v -> createWebPrintJob());
        if (btnCalculator != null) btnCalculator.setOnClickListener(v -> showCalculatorDialog());

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilterAndSearch(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (spinnerFilter != null) {
            spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedFilter = options[position];
                    applyFilterAndSearch();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (btnResetAll != null) {
            btnResetAll.setOnClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("Reset All?").setMessage("আপনি কি সব চেক মার্ক মুছে ফেলতে চান?")
                        .setPositiveButton("Yes", (dialog, which) -> resetAllStatusOnServer())
                        .setNegativeButton("No", null).show();
            });
        }

        View btnBack = findViewById(R.id.btnBottomBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void showCalculatorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_calculator, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        TextView tvDisplay = view.findViewById(R.id.tvCalcDisplay);
        TextView tvExpressionView = view.findViewById(R.id.tvCalcExpression);
        
        currentInput = "";
        result = 0;
        lastOperator = ' ';

        View.OnClickListener numListener = v -> {
            Button b = (Button) v;
            String val = b.getText().toString();
            if (currentInput.equals("0")) currentInput = "";
            currentInput += val;
            tvDisplay.setText(currentInput);
        };

        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot};
        for(int id : ids) view.findViewById(id).setOnClickListener(numListener);

        view.findViewById(R.id.btnAdd).setOnClickListener(v -> handleOperator(tvDisplay, tvExpressionView, '+'));
        view.findViewById(R.id.btnSub).setOnClickListener(v -> handleOperator(tvDisplay, tvExpressionView, '-'));
        view.findViewById(R.id.btnMul).setOnClickListener(v -> handleOperator(tvDisplay, tvExpressionView, '*'));
        view.findViewById(R.id.btnDiv).setOnClickListener(v -> handleOperator(tvDisplay, tvExpressionView, '/'));
        
        // Backspace button
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (!currentInput.isEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
                tvDisplay.setText(currentInput.isEmpty() ? "0" : currentInput);
            }
        });

        // Percent button
        view.findViewById(R.id.btnPercent).setOnClickListener(v -> {
            if (!currentInput.isEmpty()) {
                double val = Double.parseDouble(currentInput) / 100;
                currentInput = String.valueOf(val);
                tvDisplay.setText(currentInput);
            }
        });

        view.findViewById(R.id.btnEqual).setOnClickListener(v -> {
            if (!currentInput.isEmpty() && lastOperator != ' ') {
                calculateFinalResult();
                tvExpressionView.setText(tvExpressionView.getText().toString() + currentInput + " =");
                tvDisplay.setText(formatResult(result));
                currentInput = String.valueOf(result);
                lastOperator = ' ';
            }
        });

        view.findViewById(R.id.btnClear).setOnClickListener(v -> {
            currentInput = "";
            result = 0;
            lastOperator = ' ';
            tvDisplay.setText("0");
            tvExpressionView.setText("");
        });

        view.findViewById(R.id.btnCloseCalc).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleOperator(TextView display, TextView exprView, char op) {
        if (!currentInput.isEmpty()) {
            if (lastOperator == ' ') {
                result = Double.parseDouble(currentInput);
            } else {
                calculateFinalResult();
            }
            lastOperator = op;
            exprView.setText(formatResult(result) + " " + op + " ");
            currentInput = "";
            display.setText("0");
        }
    }

    private void calculateFinalResult() {
        double currentVal = Double.parseDouble(currentInput);
        switch (lastOperator) {
            case '+': result += currentVal; break;
            case '-': result -= currentVal; break;
            case '*': result *= currentVal; break;
            case '/': if (currentVal != 0) result /= currentVal; break;
        }
    }

    private String formatResult(double d) {
        if (d == (long) d) return String.format("%d", (long) d);
        else return String.format("%s", d);
    }

    private void createWebPrintJob() {
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                PrintDocumentAdapter printAdapter = view.createPrintDocumentAdapter("Inventory Report");
                printManager.print(getString(R.string.app_name) + " Document", printAdapter, new PrintAttributes.Builder().build());
            }
        });

        String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>body { font-family: 'Times New Roman', serif; margin: 0; padding: 0; } .header { background-color: #27D3F5; padding: 20px; color: white; text-align: center; } .header-group { display: flex; align-items: center; justify-content: center; } .header-text { margin-left: 15px; } .header h1 { margin: 0; font-size: 18px; text-transform: uppercase; } .header p { margin: 2px 0 0 0; font-size: 15px; font-weight: bold; } .daily-check { margin-top: 8px; font-size: 18px; font-weight: bold; font-style: italic; } .report-info { padding: 10px; font-size: 11px; color: #333; display: flex; justify-content: space-between; border-bottom: 2px solid #27D3F5; } table { width: 100%; border-collapse: collapse; font-size: 9px; margin-top: 10px; } th, td { border: 1px solid #ccc; padding: 5px; text-align: left; } th { background-color: #f2f2f2; font-weight: bold; }</style></head><body>");
        html.append("<div class='header'><div class='header-group'><div style='width:55px; height:55px; background:white; border-radius:50%;'></div><div class='header-text'><h1>The IBN SINA Pharmaceutical Industry PLC</h1><p>DINAJPUR DEPOT</p></div></div><div class='daily-check'>Daily Stock Check</div></div>");
        html.append("<div class='report-info'><span>Stock Report</span><span>Date: ").append(currentDate).append("</span></div>");
        html.append("<table><thead><tr><th>Category</th><th>Code</th><th>Product Name</th><th>Pack</th><th>Stock</th><th>Carton</th><th>Loose</th><th>Short</th><th>Excess</th><th>Remark</th></tr></thead><tbody>");
        List<InventoryModel> currentList = (adapter != null) ? adapter.getList() : new ArrayList<>();
        for (InventoryModel item : currentList) {
            html.append("<tr><td>").append(item.getCategory()).append("</td><td>").append(item.getCode()).append("</td><td>").append(item.getProductName()).append("</td><td>").append(item.getPackSize()).append("</td><td>").append(item.getTotalQty()).append("</td><td>").append(item.getCartonQty()).append("</td><td>").append(item.getLooseQty()).append("</td><td>").append(item.getShortQty()).append("</td><td>").append(item.getExcessQty()).append("</td><td>").append(item.getRemark()).append("</td></tr>");
        }
        html.append("</tbody></table></body></html>");
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }

    public void setLoading(boolean isLoading) {
        this.loadingState = isLoading;
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    public boolean isLoading() { return loadingState; }

    private void syncOfflineData() {
        Cursor cursor = dbHelper.getAllPending();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                sendOfflineUpdateToServer(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CODE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SHORT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXCESS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REMARK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS)));
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void sendOfflineUpdateToServer(String code, String s, String e, String r, String status) {
        try {
            String url = "https://script.google.com/macros/s/AKfycbxf8kiSeqv49M-S9LAE956_CauL-Ow2fNnE5c6Dw7HTFMEa85INc6lz8xw3P8CY9uhjbw/exec?action=updateStock"
                    + "&code=" + URLEncoder.encode(code, "UTF-8") + "&shortQty=" + URLEncoder.encode(s, "UTF-8") + "&excessQty=" + URLEncoder.encode(e, "UTF-8") + "&remark=" + URLEncoder.encode(r, "UTF-8") + "&status=" + URLEncoder.encode(status, "UTF-8");
            Volley.newRequestQueue(this).add(new StringRequest(Request.Method.GET, url, res -> dbHelper.deleteUpdate(code), err -> {}));
        } catch (UnsupportedEncodingException ex) { ex.printStackTrace(); }
    }

    private void fetchData(boolean showProgress) {
        if (showProgress) setLoading(true);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, "https://script.google.com/macros/s/AKfycbxf8kiSeqv49M-S9LAE956_CauL-Ow2fNnE5c6Dw7HTFMEa85INc6lz8xw3P8CY9uhjbw/exec?action=getJson", null,
                response -> {
                    setLoading(false);
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    inventoryList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            inventoryList.add(new InventoryModel(obj.optString("category", ""), obj.optString("code", ""), obj.optString("productName", ""), obj.optString("packSize", ""), obj.optString("totalQty", ""), obj.optString("loose", ""), obj.optString("carton", ""), obj.optString("cartonSize", ""), obj.optString("shortQty", ""), obj.optString("excessQty", ""), obj.optString("remark", ""), obj.optString("status", "Unchecked")));
                        }
                        if (adapter == null) { adapter = new InventoryAdapter(new ArrayList<>(inventoryList)); if (recyclerView != null) recyclerView.setAdapter(adapter); } else { adapter.updateList(new ArrayList<>(inventoryList)); }
                        applyFilterAndSearch();
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> { setLoading(false); if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false); });
        request.setRetryPolicy(new DefaultRetryPolicy(30000, 1, 1f));
        Volley.newRequestQueue(this).add(request);
    }

    private void applyFilterAndSearch() {
        if (inventoryList == null) return;
        String query = (etSearch != null) ? etSearch.getText().toString().toLowerCase().trim() : "";
        List<InventoryModel> filteredList = new ArrayList<>();
        for (InventoryModel item : inventoryList) {
            boolean matchesSearch = item.getProductName().toLowerCase().contains(query) || item.getCode().toLowerCase().contains(query);
            boolean matchesFilter = false;
            int stockCount = 0;
            try { stockCount = Integer.parseInt(item.getTotalQty()); } catch (NumberFormatException e) { stockCount = 0; }
            if (selectedFilter.equals("All")) { matchesFilter = true; }
            else if (selectedFilter.equals("Checked")) { matchesFilter = item.getStatus().equalsIgnoreCase("Checked"); }
            else if (selectedFilter.equals("Unchecked")) { matchesFilter = !item.getStatus().equalsIgnoreCase("Checked"); }
            else if (selectedFilter.equals("In Stock")) { matchesFilter = stockCount > 0; }
            else if (selectedFilter.equals("Stock Out")) { matchesFilter = stockCount <= 0; }
            else { matchesFilter = item.getCategory().equalsIgnoreCase(selectedFilter); }
            if (matchesSearch && matchesFilter) filteredList.add(item);
        }
        if (adapter != null) adapter.updateList(filteredList);
    }

    private void resetAllStatusOnServer() {
        setLoading(true);
        String url = "https://script.google.com/macros/s/AKfycbxf8kiSeqv49M-S9LAE956_CauL-Ow2fNnE5c6Dw7HTFMEa85INc6lz8xw3P8CY9uhjbw/exec?action=resetAll";
        Volley.newRequestQueue(this).add(new StringRequest(Request.Method.GET, url, response -> fetchData(true), error -> setLoading(false)));
    }
}