import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getSalon, leaveSalon } from "../services/salonService";
import { connectToSalon } from "../services/salonSocket";
import type { Salon } from "../types/salon";
import { useAuth } from "../context/AuthContext";
import LobbyCode from "../components/lobby/LobbyCode";
import PlayerList from "../components/lobby/PlayerList";

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
        getSalon(code).then(setSalon).catch(() => setError("Impossible de récupérer le salon.")).finally(() => setLoading(false));
    }, [code]);

    useEffect(() => {
        if (!code) return;
        const client = connectToSalon(
            code,
            updated => { setSalon(updated); if (updated.etat === "EN_COURS") navigate(`/salon/${code}/game`); },
            () => { alert("Le salon a été supprimé par l'hôte."); navigate("/dashboard"); }
        );
        return () => { client.deactivate(); };
    }, [code, navigate]);

    const handleLeave = async () => {
        if (!code || !currentUser) return;
        const msg = isCreator ? "Vous êtes l'hôte. Quitter supprimera le salon. Continuer ?" : "Quitter le salon ?";
        if (!window.confirm(msg)) return;
        setLeaving(true);
        try { await leaveSalon(code); navigate("/dashboard"); }
        catch { setError("Impossible de quitter le salon."); }
        finally { setLeaving(false); }
    };

    if (loading) return <p>Chargement...</p>;
    if (error && !salon) return <p className="error">{error}</p>;
    if (!salon) return <p>Salon introuvable.</p>;

    return (
        <div className="lobby">
            <h1>Salon de jeu</h1>

            <LobbyCode code={salon.code} />

            <span className={`lobby-badge ${salon.etat === "OUVERT" ? "open" : "playing"}`}>
                {salon.etat === "OUVERT" ? "En attente" : "En cours"}
            </span>

            {error && <p className="error">{error}</p>}

            <PlayerList users={salon.users} createurId={salon.createurId} maxPlayers={salon.maxPlayers} />

            <div className="lobby-actions">
                {isCreator && salon.etat === "OUVERT" && (
                    <button onClick={() => navigate(`/salon/${salon.code}/quiz`)}>
                        Lancer la partie
                    </button>
                )}
                <button className={isCreator ? "btn-danger" : "btn-outline"} onClick={handleLeave} disabled={leaving}>
                    {leaving ? "Départ..." : isCreator ? "Supprimer le salon" : "Quitter le salon"}
                </button>
            </div>
        </div>
    );
}
