import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getSalon, leaveSalon } from "../services/salonService";
import { connectToSalon } from "../services/salonSocket";
import type { Salon } from "../types/salon";
import { useAuth } from "../context/AuthContext";

function getAvatar(username: string): string {
    return username.slice(0, 2).toUpperCase();
}

function getAvatarColor(username: string): string {
    const colors = [
        'linear-gradient(135deg,#55917F,#6BAB90)',
        'linear-gradient(135deg,#5E4C5A,#7a617a)',
        'linear-gradient(135deg,#7a5540,#b07850)',
        'linear-gradient(135deg,#3a5f6f,#4e7f90)',
    ];
    let h = 0;
    for (const ch of username) h = ch.charCodeAt(0) + ((h << 5) - h);
    return colors[Math.abs(h) % colors.length];
}

export default function SalonLobby() {
    const { code } = useParams<{ code: string }>();
    const { currentUser } = useAuth();
    const navigate = useNavigate();

    const [salon, setSalon] = useState<Salon | null>(null);
    const [loading, setLoading] = useState(true);
    const [leaving, setLeaving] = useState(false);
    const [error, setError] = useState("");

    const isCreator = salon !== null && currentUser !== null && salon.createurId === currentUser.id;

    useEffect(() => {
        if (!code) { setError("Code manquant."); setLoading(false); return; }

        const loadSalon = async () => {
            try {
                setSalon(await getSalon(code));
            } catch {
                setError("Impossible de récupérer le salon.");
            } finally {
                setLoading(false);
            }
        };
        loadSalon();
    }, [code]);

    useEffect(() => {
        if (!code) return;

        const client = connectToSalon(
            code,
            (updated) => {
                setSalon(updated);
                if (updated.etat === "EN_COURS") navigate(`/salon/${code}/game`);
            },
            () => { alert("Le salon a été supprimé par l'hôte."); navigate("/dashboard"); }
        );

        return () => { client.deactivate(); };
    }, [code, navigate]);

    const handleLeave = async () => {
        if (!code || !currentUser) return;

        const msg = isCreator
            ? "Vous êtes l'hôte. Quitter supprimera le salon pour tout le monde. Continuer ?"
            : "Quitter le salon ?";
        if (!window.confirm(msg)) return;

        setLeaving(true);
        try {
            await leaveSalon(code, currentUser.id);
            navigate("/dashboard");
        } catch {
            setError("Impossible de quitter le salon.");
        } finally {
            setLeaving(false);
        }
    };

    if (loading) return <p>Chargement...</p>;
    if (error && !salon) return <p className="error">{error}</p>;
    if (!salon) return <p>Salon introuvable.</p>;

    return (
        <div className="lobby">
            <h1>Salon de jeu</h1>

            <div
                className="lobby-code-wrapper"
                onClick={() => { navigator.clipboard.writeText(salon.code).then(() => alert("Code copié !")); }}
                title="Cliquer pour copier"
            >
                <div className="lobby-code">{salon.code}</div>
                <div className="lobby-code-hint">📋 Cliquer pour copier</div>
            </div>

            <span className={`lobby-badge ${salon.etat === "OUVERT" ? "open" : "playing"}`}>
                {salon.etat === "OUVERT" ? "En attente" : "En cours"}
            </span>

            {error && <p className="error">{error}</p>}

            <h2 style={{ marginTop: 24 }}>Joueurs ({salon.users.length}/{salon.maxPlayers})</h2>

            <ul className="player-list">
                {salon.users.map((user) => (
                    <li key={user.id} className="player-item">
                        <span className="player-avatar" style={{ background: getAvatarColor(user.username) }}>
                            {getAvatar(user.username)}
                        </span>
                        {user.id === salon.createurId && <span className="crown">👑</span>}
                        {user.username}
                    </li>
                ))}
            </ul>

            <div className="lobby-actions">
                {isCreator && salon.etat === "OUVERT" && (
                    <button onClick={() => navigate(`/salon/${salon.code}/quiz`)}>
                        🚀 Lancer la partie
                    </button>
                )}

                <button
                    className={isCreator ? "btn-danger" : "btn-outline"}
                    onClick={handleLeave}
                    disabled={leaving}
                >
                    {leaving ? "Départ..." : isCreator ? "Supprimer le salon" : "Quitter le salon"}
                </button>
            </div>
        </div>
    );
}
