import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function Navbar() {
    const { isAuthenticated, logout } = useAuth();

    return (
        <nav>
            <Link to="/">
                Game Arena
            </Link>

            <div>
                <Link to="/">
                    Accueil
                </Link>

                {isAuthenticated ? (
                    <>
                        <Link to="/dashboard">
                            Dashboard
                        </Link>

                        <Link to="/quizzes">
                            Quiz
                        </Link>

                        <Link to="/salon">
                            Salon
                        </Link>

                        <button onClick={logout}>
                            Déconnexion
                        </button>
                    </>
                ) : (
                    <>
                        <Link to="/login">
                            Connexion
                        </Link>

                        <Link to="/register">
                            Inscription
                        </Link>
                    </>
                )}
            </div>
        </nav>
    );
}