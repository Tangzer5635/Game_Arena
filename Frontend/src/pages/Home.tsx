import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Home() {
    const { isAuthenticated } = useAuth();

    return (
        <div className="hero">
            <h1>🎮 Game Arena</h1>

            <p>
                Affronte tes amis en temps réel sur des quiz multijoueurs.
                Crée un salon, partage le code, et que le meilleur gagne !
            </p>

            <div className="hero-buttons">
                {isAuthenticated ? (
                    <Link to="/dashboard">
                        <button>Accéder au dashboard</button>
                    </Link>
                ) : (
                    <>
                        <Link to="/login">
                            <button>Se connecter</button>
                        </Link>
                        <Link to="/register">
                            <button className="btn-outline">Créer un compte</button>
                        </Link>
                    </>
                )}
            </div>

            <div className="hero-features">
                <div className="hero-feature">
                    <div className="icon">⚡</div>
                    <h3>Temps réel</h3>
                    <p>Les questions et scores se synchronisent instantanément entre tous les joueurs.</p>
                </div>

                <div className="hero-feature">
                    <div className="icon">🏆</div>
                    <h3>Classement live</h3>
                    <p>Suis ton score et celui de tes adversaires en direct pendant la partie.</p>
                </div>

                <div className="hero-feature">
                    <div className="icon">🔗</div>
                    <h3>Simple à rejoindre</h3>
                    <p>Un code à 4 chiffres suffit pour rejoindre un salon et commencer à jouer.</p>
                </div>
            </div>
        </div>
    );
}
