import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Home() {
    const { isAuthenticated } = useAuth();

    return (
        <div className="hero">
            <div className="hero-badge">⚡ Multijoueur temps réel</div>

            <h1>
                Bienvenue dans<br />
                <span>Game Arena</span>
            </h1>

            <p className="hero-sub">
                Affronte tes amis sur des quiz en temps réel.
                Crée un salon, partage le code à 4 chiffres,
                et que le meilleur gagne.
            </p>

            <div className="hero-buttons">
                {isAuthenticated ? (
                    <Link to="/dashboard">
                        <button className="btn-gold">Accéder au dashboard</button>
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

            <div className="hero-stats">
                <div className="hero-stat">
                    <div className="hero-stat-value">12</div>
                    <div className="hero-stat-label">Quiz</div>
                </div>
                <div className="hero-stat">
                    <div className="hero-stat-value">144</div>
                    <div className="hero-stat-label">Questions</div>
                </div>
                <div className="hero-stat">
                    <div className="hero-stat-value">∞</div>
                    <div className="hero-stat-label">Joueurs</div>
                </div>
                <div className="hero-stat">
                    <div className="hero-stat-value">15s</div>
                    <div className="hero-stat-label">Par question</div>
                </div>
            </div>

            <div className="hero-features">
                <div className="hero-feature">
                    <div className="icon">⚡</div>
                    <h3>Temps réel</h3>
                    <p>Questions et scores synchronisés instantanément via WebSocket.</p>
                </div>
                <div className="hero-feature">
                    <div className="icon">🏆</div>
                    <h3>Score vitesse</h3>
                    <p>Plus tu réponds vite, plus tu marques de points — jusqu'à 1 000 par question.</p>
                </div>
                <div className="hero-feature">
                    <div className="icon">🔥</div>
                    <h3>Streaks</h3>
                    <p>3 bonnes réponses consécutives → multiplicateur ×2 sur la suivante.</p>
                </div>
                <div className="hero-feature">
                    <div className="icon">🔗</div>
                    <h3>Code simple</h3>
                    <p>Un code à 4 chiffres pour inviter tes amis — aucun compte requis pour rejoindre.</p>
                </div>
            </div>
        </div>
    );
}
