import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { ArrowRight, Layout, Users, MessageSquare, Video } from "lucide-react";

export default function Home() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 flex flex-col">
      {/* Navbar */}
      <nav className="p-6 flex items-center justify-between max-w-7xl mx-auto w-full">
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 bg-blue-600 rounded-lg flex items-center justify-center">
            <Layout className="h-5 w-5 text-white" />
          </div>
          <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600">
            SynergyHub
          </span>
        </div>
        <div className="flex items-center gap-4">
          <Link to="/login">
            <Button variant="ghost" className="font-medium">
              Sign In
            </Button>
          </Link>
          <Link to="/register">
            <Button className="bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-200">
              Get Started
            </Button>
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      <main className="flex-1 flex items-center justify-center p-6">
        <div className="max-w-4xl mx-auto text-center space-y-8">
          <div className="space-y-4">
            <h1 className="text-5xl md:text-7xl font-bold tracking-tight text-gray-900">
              Collaborate. <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-purple-600">
                Innovate. Ship.
              </span>
            </h1>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto leading-relaxed">
              The all-in-one platform for high-performance teams. Manage projects, track sprints, and collaborate in real-time.
            </p>
          </div>

          <div className="flex items-center justify-center gap-4 pt-4">
            <Link to="/register">
              <Button size="lg" className="h-12 px-8 text-lg bg-blue-600 hover:bg-blue-700 shadow-xl shadow-blue-200 hover:translate-y-[-2px] transition-all">
                Start for Free
                <ArrowRight className="ml-2 h-5 w-5" />
              </Button>
            </Link>
            <Link to="/login">
              <Button size="lg" variant="outline" className="h-12 px-8 text-lg border-2 hover:bg-white/50 backdrop-blur-sm">
                Login to Workspace
              </Button>
            </Link>
          </div>

          {/* Feature Grid */}
          <div className="grid md:grid-cols-3 gap-6 pt-16 text-left">
            <div className="p-6 rounded-2xl bg-white/60 backdrop-blur-md border border-white/20 shadow-lg hover:shadow-xl transition-shadow">
              <div className="h-12 w-12 bg-blue-100 rounded-xl flex items-center justify-center mb-4 text-blue-600">
                <Layout className="h-6 w-6" />
              </div>
              <h3 className="text-lg font-bold mb-2">Project Management</h3>
              <p className="text-gray-600">
                Powerful kanban boards, backlogs, and timelines to keep your team on track.
              </p>
            </div>
            <div className="p-6 rounded-2xl bg-white/60 backdrop-blur-md border border-white/20 shadow-lg hover:shadow-xl transition-shadow">
              <div className="h-12 w-12 bg-purple-100 rounded-xl flex items-center justify-center mb-4 text-purple-600">
                <MessageSquare className="h-6 w-6" />
              </div>
              <h3 className="text-lg font-bold mb-2">Real-time Chat</h3>
              <p className="text-gray-600">
                Seamless team communication with channels, threads, and direct messages.
              </p>
            </div>
            <div className="p-6 rounded-2xl bg-white/60 backdrop-blur-md border border-white/20 shadow-lg hover:shadow-xl transition-shadow">
              <div className="h-12 w-12 bg-indigo-100 rounded-xl flex items-center justify-center mb-4 text-indigo-600">
                <Video className="h-6 w-6" />
              </div>
              <h3 className="text-lg font-bold mb-2">Video Meetings</h3>
              <p className="text-gray-600">
                Integrated video conferencing so you can connect faces to names instantly.
              </p>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="p-6 text-center text-sm text-gray-500">
        &copy; {new Date().getFullYear()} SynergyHub. All rights reserved.
      </footer>
    </div>
  );
}
