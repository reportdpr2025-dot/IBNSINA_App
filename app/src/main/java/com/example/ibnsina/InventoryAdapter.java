package com.example.ibnsina;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<InventoryModel> list;
    private Context context;
    private DatabaseHelper dbHelper;

    public InventoryAdapter(List<InventoryModel> list) {
        this.list = list;
    }

    public void updateList(List<InventoryModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public List<InventoryModel> getList() {
        return list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        dbHelper = new DatabaseHelper(context);
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryModel model = list.get(position);

        if (holder.tvSl != null) {
            String slValue = (model.getSl() != null && !model.getSl().isEmpty()) ? model.getSl() : String.valueOf(position + 1);
            holder.tvSl.setText(slValue);
        }
        
        if (holder.tvCategory != null) holder.tvCategory.setText(model.getCategory());
        if (holder.tvProductName != null) holder.tvProductName.setText(model.getProductName());
        if (holder.tvCode != null) holder.tvCode.setText(model.getCode());
        if (holder.tvPackSize != null) holder.tvPackSize.setText("Pack size: " + model.getPackSize());
        
        int totalStock = 0;
        int cartonSize = 0;
        int calculatedCartonQty = 0;
        int calculatedLooseQty = 0;

        try {
            totalStock = Integer.parseInt(model.getTotalQty().trim());
            cartonSize = Integer.parseInt(model.getCartonSize().trim());
            if (cartonSize > 0) {
                calculatedCartonQty = totalStock / cartonSize;
                calculatedLooseQty = totalStock - (calculatedCartonQty * cartonSize);
            } else {
                calculatedLooseQty = totalStock;
            }
        } catch (Exception e) {
            calculatedLooseQty = 0;
            calculatedCartonQty = 0;
        }

        if (holder.tvTotalQty != null) holder.tvTotalQty.setText("Total Stock: " + totalStock);
        if (holder.tvCartonSize != null) holder.tvCartonSize.setText("Crton Size: " + cartonSize);
        if (holder.tvCarton != null) holder.tvCarton.setText("Carton Qty: " + calculatedCartonQty);
        if (holder.tvLoose != null) holder.tvLoose.setText("Loose Qty: " + calculatedLooseQty);

        if (holder.etShortQty != null) holder.etShortQty.setText(model.getShortQty());
        if (holder.etExcessQty != null) holder.etExcessQty.setText(model.getExcessQty());
        if (holder.etRemark != null) holder.etRemark.setText(model.getRemark());

        if ("Checked".equalsIgnoreCase(model.getStatus())) {
            holder.itemContainer.setBackgroundColor(Color.parseColor("#C8E6C9"));
            holder.btnCheckUpdate.setChecked(true);
        } else {
            holder.itemContainer.setBackgroundColor(Color.WHITE);
            holder.btnCheckUpdate.setChecked(false);
        }

        holder.btnCheckUpdate.setOnClickListener(v -> {
            if (context instanceof MainActivity && ((MainActivity) context).isLoading()) {
                holder.btnCheckUpdate.setChecked(!holder.btnCheckUpdate.isChecked());
                Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (holder.btnCheckUpdate.isChecked()) {
                String status = "Checked";
                holder.itemContainer.setBackgroundColor(Color.parseColor("#C8E6C9"));
                model.setStatus(status);
                sendData(model.getCode(), holder.etShortQty.getText().toString(),
                        holder.etExcessQty.getText().toString(), holder.etRemark.getText().toString(), status);
                
                // কাউন্টার আপডেট করা
                if (context instanceof MainActivity) {
                    ((MainActivity) context).updateCheckedCount();
                }
            } else {
                holder.btnCheckUpdate.setChecked(true); 
                Toast.makeText(context, "Long press to uncheck", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemContainer.setOnLongClickListener(v -> {
            if (context instanceof MainActivity && ((MainActivity) context).isLoading()) {
                return true;
            }

            if ("Checked".equalsIgnoreCase(model.getStatus())) {
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle("Uncheck Item?")
                        .setMessage("Do you want to uncheck this item?")
                        .setPositiveButton("Yes", (dialogInterface, which) -> {
                            String status = "Unchecked";
                            model.setStatus(status);
                            holder.btnCheckUpdate.setChecked(false);
                            holder.itemContainer.setBackgroundColor(Color.WHITE);
                            sendData(model.getCode(), holder.etShortQty.getText().toString(),
                                    holder.etExcessQty.getText().toString(), holder.etRemark.getText().toString(), status);
                            
                            // কাউন্টার আপডেট করা
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).updateCheckedCount();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#4CAF50"));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.parseColor("#F44336"));
                
                return true;
            }
            return false;
        });
    }

    private void sendData(String code, String s, String e, String r, String status) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).setLoading(true);
        }

        try {
            String encodedCode = URLEncoder.encode(code, "UTF-8");
            String encodedS = URLEncoder.encode(s, "UTF-8");
            String encodedE = URLEncoder.encode(e, "UTF-8");
            String encodedR = URLEncoder.encode(r, "UTF-8");
            String encodedStatus = URLEncoder.encode(status, "UTF-8");

            String url = Config.SCRIPT_URL + "?action=updateStock"
                    + "&code=" + encodedCode + "&shortQty=" + encodedS + "&excessQty=" + encodedE + "&remark=" + encodedR + "&status=" + encodedStatus;

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    res -> {
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).setLoading(false);
                            ((MainActivity) context).showBigSuccessDialog();
                        }
                        dbHelper.deleteUpdate(code);
                    },
                    err -> {
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).setLoading(false);
                        }
                        dbHelper.addUpdate(code, s, e, r, status);
                        Toast.makeText(context, "Saved offline.", Toast.LENGTH_SHORT).show();
                    });

            request.setRetryPolicy(new DefaultRetryPolicy(20000, 1, 1f));
            Volley.newRequestQueue(context).add(request);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

    @Override public int getItemCount() { return list == null ? 0 : list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSl, tvCategory, tvPackSize, tvCode, tvProductName, tvTotalQty, tvLoose, tvCarton, tvCartonSize;
        EditText etShortQty, etExcessQty, etRemark;
        CheckBox btnCheckUpdate;
        LinearLayout itemContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSl = itemView.findViewById(R.id.tvSl);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPackSize = itemView.findViewById(R.id.tvPackSize);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvTotalQty = itemView.findViewById(R.id.tvTotalQty);
            tvLoose = itemView.findViewById(R.id.tvLoose);
            tvCarton = itemView.findViewById(R.id.tvCarton);
            tvCartonSize = itemView.findViewById(R.id.tvCartonSize);
            etShortQty = itemView.findViewById(R.id.etShortQty);
            etExcessQty = itemView.findViewById(R.id.etExcessQty);
            etRemark = itemView.findViewById(R.id.etRemark);
            btnCheckUpdate = itemView.findViewById(R.id.btnCheckUpdate);
            itemContainer = itemView.findViewById(R.id.itemContainer);
        }
    }
}