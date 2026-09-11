import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { createSalon } from "../services/salonService";
import type { Salon as SalonType } from "../types/salon";

export default function Salon() {
    const navigate = useNavigate();

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleCreateSalon = async () => {
        setLoading(true);
        setError("");

        try {
            const salon: SalonType = await createSalon();

            navigate(`/salon/${salon.code}`);
        } catch (error) {
            console.error(error);
            setError("Impossible de créer le salon.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <section>
            <h1>Créer un salon</h1>

            <p>
                Crée un salon et invite tes amis avec le code généré.
            </p>

            {error && <p>{error}</p>}

            <button
                onClick={handleCreateSalon}
                disabled={loading}
            >
                {loading ? "Création..." : "Créer le salon"}
            </button>
        </section>
    );
}