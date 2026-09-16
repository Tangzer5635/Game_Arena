import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import { useAuth } from "../../context/AuthContext";

export default function Login() {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setError("");

        try {
            const response = await api.post("/users/login/", { username, password });
            const authorization = response.headers.authorization;

            if (!authorization) {
                setError("Token JWT introuvable.");
                return;
            }

            login(authorization.replace("Bearer ", ""));
            navigate("/dashboard");
        } catch {
            setError("Identifiant ou mot de passe incorrect.");
        }
    };

    return (
        <div>
            <h1>Connexion</h1>

            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="username">Nom d'utilisateur</label>
                    <input
                        id="username"
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        placeholder="tanguy"
                        required
                    />
                </div>

                <div>
                    <label htmlFor="password">Mot de passe</label>
                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="••••••"
                        required
                    />
                </div>

                {error && <p className="error">{error}</p>}

                <button type="submit">Se connecter</button>
            </form>

            <p className="auth-link">
                Pas de compte ? <a href="/register">Créer un compte</a>
            </p>
        </div>
    );
}
