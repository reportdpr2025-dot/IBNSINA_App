/**
 * IBN SINA Inventory Management - Google Apps Script
 * This script handles JSON data fetching, stock updates, and reset functions.
 * Compatible with Android App and Web View.
 */

function doGet(e) {
  var action = e.parameter.action;
  var ss = SpreadsheetApp.openById('1UKCJz6vT_N9L-uhhGDVtVVJOVE5CVurzZXJZo167vu8');
  var sheet = ss.getSheetByName('ALL STOCK');

  // 1. Fetch Data for Android App (JSON Format)
  if (action == "getJson") {
    var lastRow = sheet.getLastRow();
    if (lastRow < 3) return ContentService.createTextOutput(JSON.stringify([])).setMimeType(ContentService.MimeType.JSON);
    
    // Fetch data up to Column M (Index 13) for Serial Number
    var data = sheet.getRange(3, 1, lastRow - 2, 13).getValues(); 
    var list = [];
    
    for (var i = 0; i < data.length; i++) {
      if (data[i][1] != "") { // Ensure Code column is not empty
        list.push({
          "sl": data[i][12],         // Column M (Serial Number)
          "category": data[i][0],    // Column A
          "code": data[i][1],        // Column B
          "productName": data[i][2], // Column C
          "packSize": data[i][3],    // Column D
          "totalQty": data[i][4],    // Column E
          "cartonSize": data[i][7],  // Column H
          "shortQty": data[i][8],    // Column I
          "excessQty": data[i][9],   // Column J
          "remark": data[i][10],     // Column K
          "status": data[i][5] == "TRUE" ? "Checked" : "Unchecked" // Column F
        });
      }
    }
    return ContentService.createTextOutput(JSON.stringify(list)).setMimeType(ContentService.MimeType.JSON);
  }

  // 2. Reset All Data Function
  if (action == "resetAll") {
    var lastRow = sheet.getLastRow();
    if (lastRow >= 3) {
      sheet.getRange(3, 6, lastRow - 2, 1).clearContent(); // Clear Checkbox (F)
      sheet.getRange(3, 9, lastRow - 2, 3).clearContent(); // Clear Short, Excess, Remark (I, J, K)
    }
    return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
  }

  // 3. Update Stock from Android
  if (action == "updateStock") {
    var code = e.parameter.code;
    var sheetData = sheet.getDataRange().getValues();
    
    for (var i = 2; i < sheetData.length; i++) {
      if (sheetData[i][1] == code) {
        sheet.getRange(i + 1, 9).setValue(e.parameter.shortQty);
        sheet.getRange(i + 1, 10).setValue(e.parameter.excessQty);
        sheet.getRange(i + 1, 11).setValue(e.parameter.remark);
        sheet.getRange(i + 1, 6).setValue(e.parameter.status == "Checked" ? "TRUE" : "");
        return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
      }
    }
    return ContentService.createTextOutput("Not Found").setMimeType(ContentService.MimeType.TEXT);
  }

  // 4. Serve Web App Interface (Index.html)
  try {
    const template = HtmlService.createTemplateFromFile('Index');
    template.userId = e.parameter.userId || '';
    template.userName = e.parameter.username || '';
    
    return template.evaluate()
      .setTitle('Stock View Auto')
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL)
      .setFaviconUrl('https://images.seeklogo.com/logo-png/53/1/ibn-sina-medical-logo-png_seeklogo-536126.png');
  } catch (err) {
    return ContentService.createTextOutput("Error loading Web Interface: " + err.message);
  }
}
