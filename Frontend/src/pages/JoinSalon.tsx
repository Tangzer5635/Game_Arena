import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { joinSalon } from "../services/salonService";

export default function JoinSalon() {
    const navigate = useNavigate();
    const [code, setCode] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setError("");

        const normalizedCode = code.trim();
        if (!/^\d{4}$/.test(normalizedCode)) {
            setError("Le code doit contenir exactement 4 chiffres.");
            return;
        }

        setLoading(true);
        try {
            await joinSalon(normalizedCode);
            navigate(`/salon/${normalizedCode}`);
        } catch {
            setError("Impossible de rejoindre ce salon.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="join-page">
            <h1>👥 Rejoindre un salon</h1>

            <p>Entre le code à 4 chiffres communiqué par l'hôte.</p>

            <form onSubmit={handleSubmit}>
                <input
                    className="code-input"
                    type="text"
                    inputMode="numeric"
                    maxLength={4}
                    value={code}
                    onChange={(e) => {
                        if (/^\d*$/.test(e.target.value)) setCode(e.target.value);
                    }}
                    placeholder="0000"
                    required
                />

                {error && <p className="error">{error}</p>}

                <button type="submit" disabled={loading}>
                    {loading ? "Connexion..." : "Rejoindre"}
                </button>
            </form>
        </div>
    );
}
