import { Routes, Route } from "react-router-dom";
import SalonLobby from "../pages/SalonLobby";
import MainLayout from "../layouts/MainLayout";
import AuthLayout from "../layouts/AuthLayout";
import ProtectedRoute from "./ProtectedRoute";

import Home from "../pages/Home";
import Login from "../pages/auth/Login";
import Register from "../pages/auth/Register";
import Dashboard from "../pages/Dashboard";
import Quizzes from "../pages/Quizzes";
import QuizDetails from "../pages/QuizDetails";
import Salon from "../pages/Salon";
import JoinSalon from "../pages/JoinSalon";
import NotFound from "../pages/NotFound";
import ChoixQuiz from "../pages/ChoixQuiz";
import CreateQuiz from "../pages/CreateQuiz";
import Game from "../pages/Game";
import EditQuiz from "../pages/EditQuiz.tsx";
import Profile from "../pages/Profile";

export default function AppRouter() {
    return (
        <Routes>

            {/* Routes publiques */}
            <Route element={<MainLayout />}>
                <Route path="/" element={<Home />} />
            </Route>

            {/* Auth */}
            <Route element={<AuthLayout />}>
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
            </Route>

            {/* Routes protégées */}
            <Route element={<ProtectedRoute />}>
                <Route element={<MainLayout />}>
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/quizzes" element={<Quizzes />} />
                    <Route path="/quizzes/create" element={<CreateQuiz />} />
                    <Route path="/quizzes/:quizId" element={<QuizDetails />} />
                    <Route path="/quizzes/:quizId/edit" element={<EditQuiz />}/>
                    <Route path="/salon" element={<Salon />} />
                    <Route path="/salon/join" element={<JoinSalon />} />
                    <Route path="/salon/:code" element={<SalonLobby />} />
                    <Route path="/salon/:code/quiz" element={<ChoixQuiz />}/>
                    <Route path="/salon/:code/game" element={<Game />} />
                    <Route path="/profile" element={<Profile />} />
                </Route>
            </Route>

            <Route path="*" element={<NotFound />} />

        </Routes>
    );
}