import { Outlet } from "react-router-dom";
import Topbar from "./Topbar";
import IssueModal from "@/components/issue/IssueModal";

export default function DashboardLayout() {
  return (
    <div className="min-h-screen bg-background">
      <Topbar />
      
      <main className="pt-14">
        <div className="h-[calc(100vh-3.5rem)] overflow-y-auto">
          <Outlet />
        </div>
      </main>
      <IssueModal />
    </div>
  );
}
