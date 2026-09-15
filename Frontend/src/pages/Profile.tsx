import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getUserStats, type UserStats } from "../services/userService";

export default function Profile() {
    const { currentUser } = useAuth();
    const navigate = useNavigate();

    const [stats, setStats] = useState<UserStats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!currentUser) return;
        getUserStats(currentUser.id)
            .then(setStats)
            .catch(() => setError("Impossible de charger le profil."))
            .finally(() => setLoading(false));
    }, [currentUser]);

    if (loading) return <p>Chargement...</p>;
    if (error) return <p className="error">{error}</p>;
    if (!stats) return null;

    const totalReponses = stats.bonnesReponses + stats.mauvaisesReponses;

    return (
        <div style={{ maxWidth: 560, margin: "0 auto" }}>
            <h1>👤 {stats.username}</h1>

            {stats.partiesJouees === 0 ? (
                <div className="card" style={{ textAlign: "center", padding: 32 }}>
                    <p style={{ fontSize: "2rem" }}>🎮</p>
                    <p>Tu n'as pas encore joué de partie.</p>
                    <button onClick={() => navigate("/salon")} style={{ marginTop: 12 }}>
                        Créer un salon
                    </button>
                </div>
            ) : (
                <>
                    {/* Stats principales */}
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginTop: 16 }}>
                        <StatCard icon="🏆" label="Meilleur score" value={stats.meilleurScore.toLocaleString()} />
                        <StatCard icon="📊" label="Score cumulé" value={stats.scoreCumule.toLocaleString()} />
                        <StatCard icon="🎮" label="Parties jouées" value={String(stats.partiesJouees)} />
                        <StatCard icon="🔥" label="Meilleure streak" value={String(stats.meilleureStreak)} />
                    </div>

                    {/* Taux de réussite */}
                    <div className="card" style={{ marginTop: 12 }}>
                        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
                            <span style={{ fontWeight: 600 }}>Taux de réussite</span>
                            <span style={{ color: "var(--accent)", fontWeight: 800 }}>
                                {stats.tauxReussitePct}%
                            </span>
                        </div>
                        <div className="timer-bar-track">
                            <div
                                className="timer-bar-fill"
                                style={{
                                    width: `${stats.tauxReussitePct}%`,
                                    transition: "width 1s ease",
                                    background: stats.tauxReussitePct >= 70
                                        ? "var(--green)"
                                        : stats.tauxReussitePct >= 40
                                        ? "var(--accent)"
                                        : "var(--red)"
                                }}
                            />
                        </div>
                        <p style={{ fontSize: 13, opacity: .7, marginTop: 6 }}>
                            {stats.bonnesReponses} bonnes · {stats.mauvaisesReponses} mauvaises
                            · {totalReponses} au total
                        </p>
                    </div>

                    {/* Score moyen par partie */}
                    {stats.partiesJouees > 0 && (
                        <div className="card" style={{ marginTop: 12 }}>
                            <p style={{ margin: 0, fontWeight: 600 }}>
                                Score moyen par partie
                            </p>
                            <p style={{ margin: "4px 0 0", fontSize: "1.6rem", fontWeight: 800, color: "var(--accent)" }}>
                                {Math.round(stats.scoreCumule / stats.partiesJouees).toLocaleString()} pts
                            </p>
                        </div>
                    )}
                </>
            )}

            <button
                className="btn-outline"
                style={{ marginTop: 24 }}
                onClick={() => navigate("/dashboard")}
            >
                ← Dashboard
            </button>
        </div>
    );
}

function StatCard({ icon, label, value }: { icon: string; label: string; value: string }) {
    return (
        <div className="card" style={{ textAlign: "center" }}>
            <div style={{ fontSize: "1.8rem" }}>{icon}</div>
            <div style={{ fontSize: "1.5rem", fontWeight: 800, color: "var(--accent)", margin: "4px 0" }}>
                {value}
            </div>
            <div style={{ fontSize: 12, opacity: .7, textTransform: "uppercase", letterSpacing: ".5px" }}>
                {label}
            </div>
        </div>
    );
}
