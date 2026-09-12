import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { joinSalon } from "../services/salonService";

export default function JoinSalon() {
    const navigate = useNavigate();

    const [code, setCode] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (
        event: FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        setError("");

        const normalizedCode = code.trim();

        if (!/^\d{4}$/.test(normalizedCode)) {
            setError(
                "Le code doit contenir exactement 4 chiffres."
            );
            return;
        }

        setLoading(true);

        try {
            await joinSalon(normalizedCode);

            navigate(`/salon/${normalizedCode}`);
        } catch (error) {
            console.error(error);
            setError(
                "Impossible de rejoindre ce salon."
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <section>
            <h1>Rejoindre un salon</h1>

            <p>
                Entre le code à 4 chiffres du salon.
            </p>

            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="code">
                        Code du salon
                    </label>

                    <input
                        id="code"
                        type="text"
                        inputMode="numeric"
                        maxLength={4}
                        value={code}
                        onChange={(event) => {
                            const value = event.target.value;

                            if (/^\d*$/.test(value)) {
                                setCode(value);
                            }
                        }}
                        placeholder="1234"
                        required
                    />
                </div>

                {error && <p>{error}</p>}

                <button
                    type="submit"
                    disabled={loading}
                >
                    {loading ? "Connexion..." : "Rejoindre"}
                </button>
            </form>
        </section>
    );
}