import customtkinter as ctk
import build_web
import os
import sys

# Force the working directory to the script's directory so glob works if run from anywhere
if getattr(sys, 'frozen', False):
    application_path = os.path.dirname(sys.executable)
else:
    application_path = os.path.dirname(os.path.abspath(__file__))

# Update build_web.DIR to be the parent directory (docs/testing) since the tool is inside tools/
build_web.DIR = os.path.abspath(os.path.join(application_path, ".."))

ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

class DashboardApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        self.title("CustomBlocks Testing Dashboard")
        self.geometry("900x700")

        # Layout
        self.grid_columnconfigure(1, weight=1)
        self.grid_rowconfigure(0, weight=1)

        # Sidebar
        self.sidebar_frame = ctk.CTkFrame(self, width=200, corner_radius=0)
        self.sidebar_frame.grid(row=0, column=0, sticky="nsew")
        self.sidebar_frame.grid_rowconfigure(4, weight=1)

        self.logo_label = ctk.CTkLabel(self.sidebar_frame, text="CustomBlocks\nDashboard", font=ctk.CTkFont(size=20, weight="bold"))
        self.logo_label.grid(row=0, column=0, padx=20, pady=(20, 10))

        self.refresh_btn = ctk.CTkButton(self.sidebar_frame, text="Refresh Data", command=self.load_data)
        self.refresh_btn.grid(row=1, column=0, padx=20, pady=10)

        self.next_task_btn = ctk.CTkButton(self.sidebar_frame, text="Get Next Task", command=self.show_next_task, fg_color="#28a745", hover_color="#218838")
        self.next_task_btn.grid(row=2, column=0, padx=20, pady=10)

        self.stats_lbl = ctk.CTkLabel(self.sidebar_frame, text="", justify="left")
        self.stats_lbl.grid(row=3, column=0, padx=20, pady=20)

        # Main Content (Scrollable)
        self.main_frame = ctk.CTkScrollableFrame(self)
        self.main_frame.grid(row=0, column=1, sticky="nsew", padx=20, pady=20)
        self.main_frame.grid_columnconfigure(0, weight=1)

        # Editor Frame (Hidden by default)
        self.editor_frame = ctk.CTkFrame(self, corner_radius=0)
        self.editor_frame.grid_columnconfigure(0, weight=1)
        self.editor_frame.grid_rowconfigure(1, weight=1)
        
        self.editor_topbar = ctk.CTkFrame(self.editor_frame, height=50, corner_radius=0, fg_color="transparent")
        self.editor_topbar.grid(row=0, column=0, sticky="ew", padx=20, pady=(20, 10))
        
        self.editor_back_btn = ctk.CTkButton(self.editor_topbar, text="◀ Back to Dashboard", command=self.close_editor, width=120)
        self.editor_back_btn.pack(side="left")
        
        self.editor_save_btn = ctk.CTkButton(self.editor_topbar, text="💾 Save File", command=self.save_file, fg_color="#28a745", hover_color="#218838", width=120)
        self.editor_save_btn.pack(side="right")
        
        self.editor_title = ctk.CTkLabel(self.editor_topbar, text="Editing...", font=ctk.CTkFont(size=18, weight="bold"))
        self.editor_title.pack(side="left", padx=20)

        self.editor_textbox = ctk.CTkTextbox(self.editor_frame, font=ctk.CTkFont(family="Consolas", size=14))
        self.editor_textbox.grid(row=1, column=0, sticky="nsew", padx=20, pady=(0, 20))

        self.current_edit_file = None
        self.data = None
        self.load_data()

    def load_data(self):
        # clear main frame
        for widget in self.main_frame.winfo_children():
            widget.destroy()

        try:
            data = build_web.extract_data()
        except Exception as e:
            error_lbl = ctk.CTkLabel(self.main_frame, text=f"Error loading data: {e}", text_color="red")
            error_lbl.grid(row=0, column=0)
            return

        self.data = data

        total_tests = sum(g["total"] for g in data["groups"])
        passed_tests = sum(g["passed"] for g in data["groups"])
        pct = int((passed_tests / total_tests) * 100) if total_tests > 0 else 0

        self.stats_lbl.configure(text=f"Total Tests: {total_tests}\nPassed: {passed_tests}\nProgress: {pct}%")

        row = 0
        title_lbl = ctk.CTkLabel(self.main_frame, text=f"Project Groups (Last Updated: {data['lastUpdated']})", font=ctk.CTkFont(size=18, weight="bold"))
        title_lbl.grid(row=row, column=0, pady=(0, 20), sticky="w")
        row += 1

        for g in data["groups"]:
            card = ctk.CTkFrame(self.main_frame, corner_radius=8)
            card.grid(row=row, column=0, sticky="ew", pady=5)
            card.grid_columnconfigure(1, weight=1)

            lbl_title = ctk.CTkLabel(card, text=g["title"], font=ctk.CTkFont(weight="bold"))
            lbl_title.grid(row=0, column=0, padx=10, pady=(10, 0), sticky="w")

            lbl_verdict = ctk.CTkLabel(card, text=g["verdict"], text_color="gray")
            lbl_verdict.grid(row=1, column=0, padx=10, pady=(0, 10), sticky="w")

            prog = ctk.CTkProgressBar(card)
            prog.grid(row=0, column=1, rowspan=2, padx=20, sticky="e")
            prog.set(g["percent"] / 100)
            
            lbl_pct = ctk.CTkLabel(card, text=f"{g['percent']}% ({g['passed']}/{g['total']})")
            lbl_pct.grid(row=0, column=2, rowspan=2, padx=10, sticky="e")

            # Open file button inside integrated editor
            btn = ctk.CTkButton(card, text="Open", width=60, command=lambda f=g["filename"]: self.open_editor(f))
            btn.grid(row=0, column=3, rowspan=2, padx=10, sticky="e")

            row += 1

        # Bugs
        if data["bugs"]:
            row += 1
            bug_title = ctk.CTkLabel(self.main_frame, text="🐛 Active Bugs", font=ctk.CTkFont(size=18, weight="bold"))
            bug_title.grid(row=row, column=0, pady=(30, 10), sticky="w")
            row += 1

            for b in data["bugs"]:
                bc = ctk.CTkFrame(self.main_frame, corner_radius=8, fg_color="#4a2323")
                bc.grid(row=row, column=0, sticky="ew", pady=5)
                blbl = ctk.CTkLabel(bc, text=f"{b['bugId']} | {b['group']} (Row {b['testRow']})\nIssue: {b['issue']}")
                blbl.pack(padx=10, pady=10, anchor="w")
                row += 1

    def show_next_task(self):
        if not self.data: return
        task = self.data.get("nextTask")
        if task:
            msg = f"GROUP: {task['group']}\nID: {task['id']}\nACTION: {task['action']}\nEXPECT: {task['expect']}"
        else:
            msg = "No active tasks found! You are 100% complete."

        dialog = ctk.CTkToplevel(self)
        dialog.title("Next Task")
        dialog.geometry("450x250")
        dialog.attributes('-topmost', True)
        
        lbl = ctk.CTkLabel(dialog, text="🚀 YOUR NEXT TASK", font=ctk.CTkFont(weight="bold", size=16))
        lbl.pack(pady=(20,10))
        
        txt = ctk.CTkTextbox(dialog, width=400, height=120)
        txt.pack(pady=10)
        txt.insert("0.0", msg)
        txt.configure(state="disabled")
        
    def open_editor(self, filename):
        self.current_edit_file = os.path.join(build_web.DIR, filename)
        self.editor_title.configure(text=f"Editing: {filename}")
        
        with open(self.current_edit_file, "r", encoding="utf-8") as f:
            content = f.read()
            
        self.editor_textbox.delete("0.0", "end")
        self.editor_textbox.insert("0.0", content)
        
        self.main_frame.grid_forget()
        self.editor_frame.grid(row=0, column=1, sticky="nsew")
        
    def close_editor(self):
        self.editor_frame.grid_forget()
        self.main_frame.grid(row=0, column=1, sticky="nsew", padx=20, pady=20)
        self.load_data() # refresh in case changes were made
        
    def save_file(self):
        if self.current_edit_file:
            content = self.editor_textbox.get("0.0", "end")
            with open(self.current_edit_file, "w", encoding="utf-8") as f:
                f.write(content.strip() + "\n")
            
            # Show brief save confirmation on button
            orig_text = self.editor_save_btn.cget("text")
            self.editor_save_btn.configure(text="✅ Saved!")
            self.after(1500, lambda: self.editor_save_btn.configure(text=orig_text))

if __name__ == "__main__":
    app = DashboardApp()
    app.mainloop()
