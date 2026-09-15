import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createSalon } from "../services/salonService";

export default function Salon() {
    const navigate = useNavigate();
    const [maxPlayers, setMaxPlayers] = useState(4);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleCreateSalon = async () => {
        setLoading(true);
        setError("");
        try {
            const salon = await createSalon(maxPlayers);
            navigate(`/salon/${salon.code}`);
        } catch {
            setError("Impossible de créer le salon.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="create-page">
            <h1>🎮 Créer un salon</h1>
            <p>Crée un salon et partage le code à 4 chiffres avec tes amis.</p>

            <label htmlFor="maxPlayers">Nombre maximum de joueurs</label>
            <select id="maxPlayers" value={maxPlayers} onChange={(e) => setMaxPlayers(Number(e.target.value))}>
                <option value={2}>2 joueurs</option>
                <option value={4}>4 joueurs</option>
                <option value={8}>8 joueurs</option>
                <option value={16}>16 joueurs</option>
            </select>

            {error && <p className="error">{error}</p>}
            <button onClick={handleCreateSalon} disabled={loading}>
                {loading ? "Création..." : "Créer un salon"}
            </button>
        </div>
    );
}
