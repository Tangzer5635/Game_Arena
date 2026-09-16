import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function Navbar() {
    const { isAuthenticated, currentUser, logout } = useAuth();

    return (
        <nav>
            <Link to="/" className="nav-brand"><img src="/favicon.ico" alt="Logo Application"/> | GAME ARENA</Link>
            <div>
                {isAuthenticated ? (
                    <>
                        <Link to="/dashboard">Dashboard</Link>
                        <Link to="/quizzes">Quiz</Link>
                        <Link to="/salon">Salon</Link>
                        <Link to="/profile">Mon profil</Link>
                        <button className="btn-ghost" onClick={logout}>
                            Déconnexion{currentUser ? ` (${currentUser.username})` : ""}
                        </button>
                    </>
                ) : (
                    <>
                        <Link to="/login">Connexion</Link>
                        <Link to="/register">Inscription</Link>
                    </>
                )}
            </div>
        </nav>
    );
}
