import { useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import IssueModal from "@/components/issue/IssueModal";

export default function DashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <Topbar onMenuClick={() => setSidebarOpen(true)} />
      
      <main className="lg:ml-64 pt-14">
        <div className="h-[calc(100vh-3.5rem)] overflow-y-auto">
          <Outlet />
        </div>
      </main>
      <IssueModal />
    </div>
  );
}
