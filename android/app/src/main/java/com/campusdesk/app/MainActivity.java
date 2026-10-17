package com.campusdesk.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    EditText usernameField, passwordField;
    LinearLayout root, content;
    String token = "", role = "", selectedRole = "student";
    String apiOverride = BuildConfig.CAMPUSDESK_API_BASE_URL;
    final int ink = Color.rgb(24, 35, 58), blue = Color.rgb(48, 91, 220), pale = Color.rgb(245, 247, 252), muted = Color.rgb(112, 124, 146), border = Color.rgb(226, 231, 241), green = Color.rgb(20, 145, 105);

    @Override public void onCreate(Bundle b) { super.onCreate(b); showLogin(); }

    TextView text(String s, int size, boolean bold) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(ink);
        if (bold) v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        v.setPadding(0, 4, 0, 4); return v;
    }
    GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    LinearLayout.LayoutParams params(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }
    EditText field(String hint, boolean secret) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setHint(hint); e.setTextSize(15); e.setTextColor(ink);
        e.setHintTextColor(Color.rgb(151,160,176)); e.setPadding(dp(15), dp(12), dp(15), dp(12));
        e.setBackground(shape(Color.WHITE, 14, border));
        if (secret) e.setInputType(129); else e.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES );
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, dp(5), 0, dp(13)); e.setLayoutParams(lp); return e;
    }
    Button button(String label, Runnable action) { return button(label, action, true); }
    Button button(String label, Runnable action, boolean primary) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); b.setTextColor(primary ? Color.WHITE : ink);
        b.setBackground(shape(primary ? blue : Color.WHITE, 14, primary ? 0 : border)); b.setPadding(dp(12), dp(8), dp(12), dp(8)); b.setElevation(0);
        b.setMinHeight(dp(48)); b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(49)); lp.setMargins(0, dp(5), 0, dp(5)); b.setLayoutParams(lp); return b;
    }
    void shell(String title, String subtitle) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(pale); root.setPadding(dp(20), dp(16), dp(20), dp(10));
        LinearLayout brandRow = new LinearLayout(this); brandRow.setOrientation(LinearLayout.HORIZONTAL); brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("C", 22, true); mark.setTextColor(Color.WHITE); mark.setGravity(Gravity.CENTER); mark.setBackground(shape(blue, 15, 0));
        brandRow.addView(mark, params(46,46)); LinearLayout brandText = new LinearLayout(this); brandText.setOrientation(LinearLayout.VERTICAL); brandText.setPadding(dp(11),0,0,0);
        TextView brand = text("CampusConnect", 19, true); brand.setTextColor(ink); brandText.addView(brand);
        TextView sub = text("CAMPUS • ACADEMICS • COMMUNITY", 9, true); sub.setTextColor(muted); brandText.addView(sub); brandRow.addView(brandText); root.addView(brandRow);
        TextView heading = text(title, 26, true); heading.setPadding(0, dp(22), 0, dp(1)); root.addView(heading);
        TextView desc = text(subtitle, 13, false); desc.setTextColor(muted); desc.setPadding(0,0,0,dp(14)); root.addView(desc);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(false); scroll.setClipToPadding(false);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0, dp(2), 0, dp(22)); scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView footer = text("CAMPUSCONNECT  •  YOUR CAMPUS, CONNECTED", 9, true); footer.setTextColor(muted); footer.setGravity(Gravity.CENTER); footer.setPadding(0,dp(8),0,dp(4)); root.addView(footer);
        setContentView(root);
    }
    LinearLayout card() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(17),dp(16),dp(17),dp(16)); c.setBackground(shape(Color.WHITE,20,border)); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(5),0,dp(9)); c.setLayoutParams(lp); return c; }
    void sectionLabel(String label) { TextView t = text(label.toUpperCase(), 11, true); t.setTextColor(muted); t.setPadding(dp(2),dp(12),0,dp(5)); content.addView(t); }
    void serverSettings() {
        EditText e = field("https://your-server.example", false); e.setText(apiBase());
        new android.app.AlertDialog.Builder(this).setTitle("API server settings").setMessage("CampusConnect backend HTTPS URL").setView(e)
            .setPositiveButton("Save", (d,w) -> { String val=e.getText().toString().trim(); if(!val.isEmpty()){ if(!val.startsWith("http://")&&!val.startsWith("https://")) val="https://"+val; apiOverride=val.replaceAll("/+$", ""); Toast.makeText(this,"Server URL updated",Toast.LENGTH_SHORT).show(); } })
            .setNegativeButton("Cancel", null).show();
    }
    String apiBase() { return apiOverride; }
    void put(JSONObject j, String key, Object value) { try { j.put(key, value); } catch (Exception e) { throw new RuntimeException(e); } }
    JSONObject request(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(apiBase()+path).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(12000); c.setReadTimeout(15000);
        c.setRequestProperty("Accept","application/json"); c.setRequestProperty("Connection","close");
        if (!token.isEmpty()) c.setRequestProperty("Authorization","Bearer "+token);
        if (body != null) { c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json; charset=utf-8"); try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));} }
        int status = c.getResponseCode(); InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (in != null) { byte[] buf=new byte[2048]; int n; while((n=in.read(buf))!=-1)out.write(buf,0,n); in.close(); }
        c.disconnect(); JSONObject result;
        try { result = new JSONObject(out.toString("UTF-8")); } catch(Exception ex) { throw new Exception("Server returned an invalid response. Check backend status."); }
        if (status < 200 || status >= 300) throw new Exception(result.optString("error","Request failed: "+status)); return result;
    }
    @FunctionalInterface
    interface AsyncJob {
        void run() throws Exception;
    }
    void async(AsyncJob job) {
        new Thread(() -> { try { job.run(); } catch(Exception e) { runOnUiThread(() -> Toast.makeText(this, e.getMessage()==null?"Something went wrong":e.getMessage(),Toast.LENGTH_LONG).show()); }}).start();
    }
    void roleTabs(LinearLayout parent) {
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); tabs.setPadding(dp(4),dp(4),dp(4),dp(4)); tabs.setBackground(shape(Color.rgb(237,240,248),14,0));
        Button admin = tabButton("Admin", "admin".equals(selectedRole)); Button student = tabButton("Student", "student".equals(selectedRole));
        admin.setOnClickListener(v->{selectedRole="admin";showLogin();}); student.setOnClickListener(v->{selectedRole="student";showLogin();});
        tabs.addView(admin,new LinearLayout.LayoutParams(0,dp(44),1)); tabs.addView(student,new LinearLayout.LayoutParams(0,dp(44),1)); parent.addView(tabs);
    }
    Button tabButton(String label, boolean active) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setTextColor(active?Color.WHITE:muted); b.setBackground(shape(active?blue:Color.TRANSPARENT,11,0)); b.setElevation(0); return b; }
    void showLogin() {
        shell("Welcome to campus", "Choose your account type to sign in.");
        LinearLayout panel=card(); content.addView(panel);
        TextView welcome=text("Let’s get you connected",19,true); panel.addView(welcome);
        TextView caption=text("Select a role, then enter your login details.",12,false); caption.setTextColor(muted); caption.setPadding(0,0,0,dp(14)); panel.addView(caption);
        roleTabs(panel);
        TextView userLabel=text("USERNAME",10,true); userLabel.setTextColor(muted); userLabel.setPadding(0,dp(16),0,0); panel.addView(userLabel);
        usernameField=field("Enter your username",false); panel.addView(usernameField);
        TextView passLabel=text("PASSWORD",10,true); passLabel.setTextColor(muted); panel.addView(passLabel);
        passwordField=field("Enter your password",true); panel.addView(passwordField);
        panel.addView(button("Sign in as "+(selectedRole.equals("admin")?"Admin":"Student")+"  →", () -> doLogin()));
        TextView note=text("Your account access depends on the role assigned by your campus administrator.",11,false); note.setTextColor(muted); note.setPadding(0,dp(10),0,0); panel.addView(note);
        content.addView(button("⚙  Server settings",this::serverSettings,false));
    }
    void doLogin() {
        String u=usernameField.getText().toString().trim(), p=passwordField.getText().toString();
        if(u.isEmpty()||p.isEmpty()){Toast.makeText(this,"Enter username and password",Toast.LENGTH_SHORT).show();return;}
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(passwordField.getWindowToken(),0);
        async(()->{ JSONObject body=new JSONObject(); put(body,"username",u); put(body,"password",p); JSONObject result=request("POST","/api/login",body);
            String gotRole=result.getJSONObject("user").getString("role");
            if(!selectedRole.equals(gotRole)) throw new Exception("This account is not registered as "+selectedRole+". Choose the correct tab.");
            token=result.getString("token"); role=gotRole; runOnUiThread(()->{if(role.equals("admin"))showAdmin();else showStudent();}); });
    }
    void addStat(LinearLayout parent,String label,String value) { LinearLayout c=card(); c.setPadding(dp(14),dp(12),dp(14),dp(12)); TextView val=text(value,22,true); val.setTextColor(blue); c.addView(val); TextView lab=text(label,11,false);lab.setTextColor(muted);c.addView(lab);parent.addView(c); }
    void showAdmin() {
        shell("Admin dashboard", "Manage students, academics and campus information.");
        LinearLayout welcome=card(); TextView hi=text("Administrator workspace",17,true);welcome.addView(hi);TextView desc=text("Everything you need to manage your campus.",12,false);desc.setTextColor(muted);welcome.addView(desc);content.addView(welcome);
        sectionLabel("Student management"); content.addView(button("♙  View all students",this::loadStudents)); content.addView(button("＋  Add new student & set password",this::addStudentForm));
        sectionLabel("Academics & campus"); content.addView(button("✎  Update student CGPA",this::chooseStudentForCgpa,false)); content.addView(button("♧  Faculty directory",this::loadFaculty,false)); content.addView(button("★  Academic leaderboard",this::loadLeaderboard,false));
        sectionLabel("Account"); content.addView(button("⚙  API server settings",this::serverSettings,false)); content.addView(button("Sign out",this::logout,false));
        content.addView(text("New student accounts require a unique username, roll number and password of at least 8 characters.",11,false));
    }
    void logout(){token="";role="";passwordField=null;showLogin();}
    void loadStudents() {
        progressScreen("Loading students…","Fetching student records from CampusConnect.");
        async(()->{JSONArray arr=request("GET","/api/students",null).getJSONArray("students");runOnUiThread(()->{
            shell("Student directory",arr.length()+" student accounts");content.addView(button("＋  Add new student",this::addStudentForm));
            content.addView(button("↻  Refresh records",this::loadStudents,false));
            if(arr.length()==0)content.addView(text("No student accounts yet. Add your first student.",14,false));
            for(int i=0;i<arr.length();i++){JSONObject s=arr.optJSONObject(i);LinearLayout c=card();TextView name=text(s.optString("full_name"),16,true);c.addView(name);TextView meta=text("Roll no. "+s.optString("roll_no")+"  •  "+s.optString("username"),11,false);meta.setTextColor(muted);c.addView(meta);
                LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);TextView cg=text("CGPA  "+s.optDouble("cgpa",0),13,true);cg.setTextColor(blue);TextView at=text("Attendance  "+s.optDouble("attendance",0)+"%",13,true);at.setTextColor(green);stats.addView(cg,new LinearLayout.LayoutParams(0,-2,1));stats.addView(at,new LinearLayout.LayoutParams(0,-2,1));c.addView(stats);c.addView(button("Update CGPA",()->updateCgpa(s.optString("roll_no")),false));content.addView(c);}
            content.addView(button("←  Dashboard",this::showAdmin,false));});});
    }
    void progressScreen(String title,String subtitle){shell(title,subtitle);ProgressBar p=new ProgressBar(this);LinearLayout.LayoutParams lp=params(44,44);lp.gravity=Gravity.CENTER;lp.setMargins(0,dp(30),0,dp(30));content.addView(p,lp);}
    void addStudentForm() {
        shell("Add a student", "Create a student account and assign login credentials.");LinearLayout panel=card();content.addView(panel);
        EditText name=field("Student full name",false), roll=field("Roll number (unique)",false), username=field("Login username (unique)",false), pass=field("Temporary password (8+ characters)",true), sem=field("Semester (1–12)",false), cgpa=field("Initial CGPA (0–10)",false), att=field("Attendance % (0–100)",false);
        TextView[] labels={text("FULL NAME",10,true),text("ROLL NUMBER",10,true),text("USERNAME",10,true),text("TEMPORARY PASSWORD",10,true),text("SEMESTER",10,true),text("CGPA",10,true),text("ATTENDANCE",10,true)}; EditText[] fields={name,roll,username,pass,sem,cgpa,att};
        for(int i=0;i<fields.length;i++){labels[i].setTextColor(muted);panel.addView(labels[i]);panel.addView(fields[i]);}
        panel.addView(button("Create student account",()->{
            String n=name.getText().toString().trim(),r=roll.getText().toString().trim(),u=username.getText().toString().trim(),pw=pass.getText().toString();
            if(n.isEmpty()||r.isEmpty()||u.isEmpty()||pw.length()<8){Toast.makeText(this,"Fill name, roll, username and password (8+ chars)",Toast.LENGTH_LONG).show();return;}
            async(()->{JSONObject b=new JSONObject();put(b,"full_name",n);put(b,"roll_no",r);put(b,"username",u);put(b,"password",pw);put(b,"semester",integer(sem,1));put(b,"cgpa",decimal(cgpa,0));put(b,"attendance",decimal(att,0));request("POST","/api/students",b);runOnUiThread(()->new android.app.AlertDialog.Builder(this).setTitle("Student account created").setMessage("Username: "+u+"\nTemporary password: "+pw+"\n\nShare these credentials securely with the student.").setPositiveButton("Done",(d,w)->loadStudents()).setCancelable(false).show());});
        }));
        content.addView(button("←  Back to dashboard",this::showAdmin,false));
    }
    int integer(EditText e,int d){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return d;}}
    double decimal(EditText e,double d){try{return Double.parseDouble(e.getText().toString());}catch(Exception x){return d;}}
    void chooseStudentForCgpa(){loadStudents();}
    void updateCgpa(String roll) { EditText e=field("New CGPA (0–10)",false);new android.app.AlertDialog.Builder(this).setTitle("Update CGPA • Roll "+roll).setView(e).setPositiveButton("Save",(d,w)->async(()->{double val=Double.parseDouble(e.getText().toString());JSONObject b=new JSONObject();put(b,"cgpa",val);request("PATCH","/api/students/"+roll+"/cgpa",b);runOnUiThread(()->{Toast.makeText(this,"CGPA updated",Toast.LENGTH_SHORT).show();loadStudents();});})).setNegativeButton("Cancel",null).show(); }
    void loadFaculty() { progressScreen("Faculty directory","Loading campus faculty.");async(()->{JSONArray a=request("GET","/api/faculty",null).getJSONArray("faculty");runOnUiThread(()->{shell("Faculty directory",a.length()+" faculty members");for(int i=0;i<a.length();i++){JSONObject f=a.optJSONObject(i);LinearLayout c=card();c.addView(text(f.optString("name"),16,true));TextView dep=text(f.optString("department"),12,false);dep.setTextColor(muted);c.addView(dep);if(!f.optString("email").isEmpty())c.addView(text(f.optString("email"),12,false));content.addView(c);}sectionLabel("Add faculty member");EditText n=field("Faculty name",false),dep=field("Department",false),email=field("Email (optional)",false);content.addView(n);content.addView(dep);content.addView(email);content.addView(button("Add faculty",()->{if(n.getText().toString().trim().isEmpty()||dep.getText().toString().trim().isEmpty()){Toast.makeText(this,"Name and department required",Toast.LENGTH_SHORT).show();return;}async(()->{JSONObject b=new JSONObject();put(b,"name",n.getText().toString().trim());put(b,"department",dep.getText().toString().trim());put(b,"email",email.getText().toString().trim());request("POST","/api/faculty",b);runOnUiThread(this::loadFaculty);});}));content.addView(button("←  Dashboard",this::showAdmin,false));});}); }
    void loadLeaderboard() { progressScreen("Academic leaderboard","Loading student rankings.");async(()->{JSONArray a=request("GET","/api/leaderboard",null).getJSONArray("students");runOnUiThread(()->{shell("Leaderboard", "Academic performance across campus.");for(int i=0;i<a.length();i++){JSONObject s=a.optJSONObject(i);LinearLayout c=card();TextView rank=text("#"+(i+1)+"   "+s.optString("full_name"),15,true);c.addView(rank);TextView detail=text("CGPA "+s.optDouble("cgpa")+"  •  Attendance "+s.optDouble("attendance")+"%",12,false);detail.setTextColor(muted);c.addView(detail);content.addView(c);}content.addView(button("←  Back",()->{if(role.equals("admin"))showAdmin();else showStudent();},false));});}); }
    void showStudent() {
        shell("Student dashboard", "Your academic life, all in one place.");LinearLayout welcome=card();welcome.addView(text("Welcome back!",18,true));TextView desc=text("Track your progress and explore campus information.",12,false);desc.setTextColor(muted);welcome.addView(desc);content.addView(welcome);
        sectionLabel("My academics");content.addView(button("▣  My academic progress",this::loadProgress));content.addView(button("★  Campus leaderboard",this::loadLeaderboard,false));
        sectionLabel("Campus services");content.addView(button("♧  Faculty directory",this::loadFaculty,false));content.addView(button("◉  My account details",this::loadMyAccount,false));
        sectionLabel("Account");content.addView(button("⚙  Server settings",this::serverSettings,false));content.addView(button("Sign out",this::logout,false));
    }
    void loadProgress(){progressScreen("Academic progress","Fetching your latest academic records.");async(()->{JSONObject s=request("GET","/api/student/progress",null).getJSONObject("student");runOnUiThread(()->{shell("My progress", "Your latest academic snapshot.");LinearLayout hero=card();hero.addView(text(s.optString("full_name"),19,true));TextView roll=text("Roll no. "+s.optString("roll_no"),12,false);roll.setTextColor(muted);hero.addView(roll);content.addView(hero);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout cg=card();cg.addView(text(String.valueOf(s.optDouble("cgpa")),25,true));cg.addView(text("CGPA / 10",11,false));LinearLayout at=card();at.addView(text(String.format(java.util.Locale.US,"%.1f%%",s.optDouble("attendance")),25,true));at.addView(text("Attendance",11,false));row.addView(cg,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);rp.setMargins(dp(8),0,0,0);row.addView(at,rp);content.addView(row);
        LinearLayout details=card();details.addView(text("Course",11,false));details.addView(text(s.optString("course"),14,true));details.addView(text("Semester",11,false));details.addView(text(String.valueOf(s.optInt("semester")),14,true));content.addView(details);content.addView(button("←  Dashboard",this::showStudent,false));});});}
    void loadMyAccount(){async(()->{JSONObject u=request("GET","/api/me",null).getJSONObject("user");runOnUiThread(()->{shell("My account", "Account information linked to your login.");LinearLayout c=card();c.addView(text(u.optString("full_name"),19,true));c.addView(text("Username: "+u.optString("username"),13,false));c.addView(text("Role: "+u.optString("role"),13,false));if(!u.isNull("roll_no"))c.addView(text("Roll no.: "+u.optString("roll_no"),13,false));c.addView(text("Course: "+u.optString("course"),13,false));c.addView(text("Semester: "+u.optInt("semester"),13,false));content.addView(c);content.addView(button("←  Dashboard",this::showStudent,false));});});}
}
