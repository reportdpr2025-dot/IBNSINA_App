package com.example.ibnsina;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
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
    private List<InventoryModel> fullInventoryList = new ArrayList<>();
    private List<InventoryModel> filteredList = new ArrayList<>();
    private EditText etSearch;
    private Spinner spinnerFilter;
    private String selectedFilter = "All";
    private MaterialButton btnResetAll, btnRefreshManual, btnPrint, btnCalculator;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseHelper dbHelper;

    private int currentPage = 0;
    private final int PAGE_SIZE = 30;
    private boolean isPagingEnabled = false; 
    private TextView tvPageInfo, tvCheckedCount; // tvCheckedCount যোগ করা হলো
    private MaterialButton btnNextPage, btnPrevPage;

    private boolean loadingState = false;

    private String currentInput = "";
    private String currentExpressionText = "";
    private double calcResult = 0;
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

        tvPageInfo = findViewById(R.id.tvPageInfo);
        tvCheckedCount = findViewById(R.id.tvCheckedCount); // আইডি বাইন্ডিং
        btnNextPage = findViewById(R.id.btnNextPage);
        btnPrevPage = findViewById(R.id.btnPrevPage);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setHasFixedSize(true);
        }

        String[] options = {"All", "Checked", "Unchecked", "In Stock", "Stock Out", "PHARMA", "OPTHALMIC", "HERBAL"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        if (spinnerFilter != null) spinnerFilter.setAdapter(spinnerAdapter);

        fetchData(true);

        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setOnRefreshListener(() -> { isPagingEnabled = false; fetchData(true); });

        if (btnRefreshManual != null)
            btnRefreshManual.setOnClickListener(v -> { isPagingEnabled = false; fetchData(true); });

        if (btnPrint != null) btnPrint.setOnClickListener(v -> createWebPrintJob());
        if (btnCalculator != null) btnCalculator.setOnClickListener(v -> showCalculatorDialog());

        if (btnNextPage != null) btnNextPage.setOnClickListener(v -> { isPagingEnabled = true; currentPage++; updateRecyclerView(); });
        if (btnPrevPage != null) btnPrevPage.setOnClickListener(v -> { if (currentPage > 0) { isPagingEnabled = true; currentPage--; updateRecyclerView(); } });

        if (tvPageInfo != null) tvPageInfo.setOnClickListener(v -> showGoToPageDialog());

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
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Reset All?").setMessage("আপনি কি সব চেক মার্ক মুছে ফেলতে চান?")
                        .setPositiveButton("Yes", (dialogInterface, which) -> resetAllStatusOnServer()).setNegativeButton("No", null).show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#4CAF50"));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.parseColor("#F44336"));
            });
        }

        View btnBack = findViewById(R.id.btnBottomBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // চেক করা আইটেম গুনে দেখানোর মেথড
    public void updateCheckedCount() {
        int count = 0;
        for (InventoryModel item : fullInventoryList) {
            if ("Checked".equalsIgnoreCase(item.getStatus())) {
                count++;
            }
        }
        if (tvCheckedCount != null) {
            tvCheckedCount.setText("Checked: " + count);
        }
    }

    public void showBigSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView textView = new TextView(this);
        textView.setText("\n✔\nUpdate Successful!\n");
        textView.setTextSize(22);
        textView.setTextColor(Color.parseColor("#2E7D32"));
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(20, 20, 20, 20);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        builder.setView(textView);
        final AlertDialog dialog = builder.create();
        dialog.show();
        new Handler().postDelayed(dialog::dismiss, 1500);
    }

    private void showGoToPageDialog() {
        int totalPages = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);
        if (totalPages <= 1 && isPagingEnabled) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go to Page (1 - " + totalPages + ")");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("Go", (dialog, which) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                int pageNum = Integer.parseInt(val);
                if (pageNum >= 1 && pageNum <= totalPages) { isPagingEnabled = true; currentPage = pageNum - 1; updateRecyclerView(); }
            }
        }).setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#2196F3"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.GRAY);
    }

    private void applyFilterAndSearch() {
        if (fullInventoryList == null) return;
        String query = (etSearch != null) ? etSearch.getText().toString().toLowerCase().trim() : "";
        filteredList.clear();
        for (InventoryModel item : fullInventoryList) {
            boolean matchesSearch = item.getProductName().toLowerCase().contains(query) || item.getCode().toLowerCase().contains(query);
            boolean matchesFilter = false;
            int stockCount = 0; try { stockCount = Integer.parseInt(item.getTotalQty()); } catch (Exception e) {}
            if (selectedFilter.equals("All")) matchesFilter = true;
            else if (selectedFilter.equals("Checked")) matchesFilter = item.getStatus().equalsIgnoreCase("Checked");
            else if (selectedFilter.equals("Unchecked")) matchesFilter = !item.getStatus().equalsIgnoreCase("Checked");
            else if (selectedFilter.equals("In Stock")) matchesFilter = stockCount > 0;
            else if (selectedFilter.equals("Stock Out")) matchesFilter = stockCount <= 0;
            else matchesFilter = item.getCategory().equalsIgnoreCase(selectedFilter);
            if (matchesSearch && matchesFilter) filteredList.add(item);
        }
        currentPage = 0; updateRecyclerView();
    }

    private void updateRecyclerView() {
        List<InventoryModel> displayList;
        int totalPages = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);
        if (!isPagingEnabled) {
            displayList = new ArrayList<>(filteredList);
            tvPageInfo.setText("All Items (" + filteredList.size() + ")");
            btnPrevPage.setEnabled(false);
            btnNextPage.setEnabled(filteredList.size() > PAGE_SIZE);
        } else {
            int start = currentPage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, filteredList.size());
            displayList = new ArrayList<>(filteredList.subList(start, end));
            tvPageInfo.setText("Page " + (currentPage + 1) + " of " + totalPages);
            btnPrevPage.setEnabled(currentPage > 0);
            btnNextPage.setEnabled(end < filteredList.size());
        }
        if (adapter == null) { adapter = new InventoryAdapter(displayList); recyclerView.setAdapter(adapter); }
        else { adapter.updateList(displayList); }
        recyclerView.scrollToPosition(0);
    }

    private void fetchData(boolean showProgress) {
        if (showProgress) setLoading(true);
        String url = Config.SCRIPT_URL + "?action=getJson";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    setLoading(false); if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    fullInventoryList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            fullInventoryList.add(new InventoryModel(obj.optString("sl", ""), obj.optString("category", ""), obj.optString("code", ""), obj.optString("productName", ""), obj.optString("packSize", ""), obj.optString("totalQty", ""), obj.optString("loose", ""), obj.optString("carton", ""), obj.optString("cartonSize", ""), obj.optString("shortQty", ""), obj.optString("excessQty", ""), obj.optString("remark", ""), obj.optString("status", "Unchecked")));
                        }
                        updateCheckedCount(); // শুরুতে কাউন্ট আপডেট
                        applyFilterAndSearch();
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> { setLoading(false); if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false); });
        Volley.newRequestQueue(this).add(request);
    }

    private void showCalculatorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_calculator, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        final TextView tvDisplay = view.findViewById(R.id.tvCalcDisplay);
        final TextView tvExpression = view.findViewById(R.id.tvCalcExpression);
        tvDisplay.setText(currentInput.isEmpty() ? "0" : currentInput);
        tvExpression.setText(currentExpressionText);
        View.OnClickListener numListener = v -> { Button b = (Button) v; if (currentInput.equals("0")) currentInput = ""; currentInput += b.getText().toString(); tvDisplay.setText(currentInput); };
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot};
        for(int id : ids) view.findViewById(id).setOnClickListener(numListener);
        view.findViewById(R.id.btnAdd).setOnClickListener(v -> handleOperator(tvDisplay, tvExpression, '+'));
        view.findViewById(R.id.btnSub).setOnClickListener(v -> handleOperator(tvDisplay, tvExpression, '-'));
        view.findViewById(R.id.btnMul).setOnClickListener(v -> handleOperator(tvDisplay, tvExpression, '*'));
        view.findViewById(R.id.btnDiv).setOnClickListener(v -> handleOperator(tvDisplay, tvExpression, '/'));
        view.findViewById(R.id.btnBack).setOnClickListener(v -> { if (!currentInput.isEmpty()) { currentInput = currentInput.substring(0, currentInput.length() - 1); tvDisplay.setText(currentInput.isEmpty() ? "0" : currentInput); } });
        view.findViewById(R.id.btnEqual).setOnClickListener(v -> { if (!currentInput.isEmpty() && lastOperator != ' ') { calculateFinalResult(); currentExpressionText += currentInput + " ="; tvExpression.setText(currentExpressionText); currentInput = formatResult(calcResult); tvDisplay.setText(currentInput); lastOperator = ' '; } });
        view.findViewById(R.id.btnClear).setOnClickListener(v -> { currentInput = ""; calcResult = 0; lastOperator = ' '; currentExpressionText = ""; tvDisplay.setText("0"); tvExpression.setText(""); });
        view.findViewById(R.id.btnCloseCalc).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleOperator(TextView display, TextView exprView, char op) {
        if (!currentInput.isEmpty()) {
            if (lastOperator == ' ') calcResult = Double.parseDouble(currentInput);
            else calculateFinalResult();
            lastOperator = op; currentExpressionText = formatResult(calcResult) + " " + op + " "; exprView.setText(currentExpressionText); currentInput = ""; display.setText("0");
        }
    }

    private void calculateFinalResult() { double cur = Double.parseDouble(currentInput); switch (lastOperator) { case '+': calcResult += cur; break; case '-': calcResult -= cur; break; case '*': calcResult *= cur; break; case '/': if (cur != 0) calcResult /= cur; break; } }
    private String formatResult(double d) { return (d == (long) d) ? String.format("%d", (long) d) : String.format("%s", d); }

    private void createWebPrintJob() {
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() { @Override public void onPageFinished(WebView view, String url) { PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE); printManager.print("Inventory Report", view.createPrintDocumentAdapter("Inventory Report"), new PrintAttributes.Builder().build()); } });
        String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>body { font-family: 'Times New Roman', serif; margin: 0; padding: 0; } .header { background-color: #27D3F5; padding: 20px; color: white; text-align: center; } .daily-check { margin-top: 8px; font-size: 18px; font-weight: bold; font-style: italic; } table { width: 100%; border-collapse: collapse; font-size: 9px; margin-top: 10px; } th, td { border: 1px solid #ccc; padding: 5px; text-align: left; } th { background-color: #f2f2f2; font-weight: bold; }</style></head><body>");
        html.append("<div class='header'><h1>The IBN SINA Pharmaceutical Industry PLC</h1><p>DINAJPUR DEPOT</p><div class='daily-check'>Daily Stock Check</div></div><p>Date: ").append(currentDate).append("</p><table><thead><tr><th>Category</th><th>Code</th><th>Product Name</th><th>Pack</th><th>Stock</th><th>Carton</th><th>Loose</th><th>Short</th><th>Excess</th><th>Remark</th></tr></thead><tbody>");
        for (InventoryModel item : filteredList) { html.append("<tr><td>").append(item.getCategory()).append("</td><td>").append(item.getCode()).append("</td><td>").append(item.getProductName()).append("</td><td>").append(item.getPackSize()).append("</td><td>").append(item.getTotalQty()).append("</td><td>").append(item.getCartonQty()).append("</td><td>").append(item.getLooseQty()).append("</td><td>").append(item.getShortQty()).append("</td><td>").append(item.getExcessQty()).append("</td><td>").append(item.getRemark()).append("</td></tr>"); }
        html.append("</tbody></table></body></html>");
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }

    private void resetAllStatusOnServer() {
        setLoading(true);
        String url = Config.SCRIPT_URL + "?action=resetAll";
        Volley.newRequestQueue(this).add(new StringRequest(Request.Method.GET, url, response -> {
            for (InventoryModel item : fullInventoryList) item.setStatus("Unchecked"); // লোকালে সব আনচেক করা
            updateCheckedCount(); // কাউন্ট রিসেট
            fetchData(true);
        }, error -> setLoading(false)));
    }

    public void setLoading(boolean isLoading) { this.loadingState = isLoading; if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE); }
    public boolean isLoading() { return loadingState; }
}