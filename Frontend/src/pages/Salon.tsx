import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createSalon } from "../services/salonService";

export default function Salon() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleCreateSalon = async () => {
        setLoading(true);
        setError("");
        try {
            const salon = await createSalon();
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

            <p>
                Crée un salon de jeu et partage le code à 4 chiffres
                avec tes amis pour qu'ils te rejoignent.
            </p>

            {error && <p className="error">{error}</p>}

            <button onClick={handleCreateSalon} disabled={loading}>
                {loading ? "Création..." : "Créer un salon"}
            </button>
        </div>
    );
}
