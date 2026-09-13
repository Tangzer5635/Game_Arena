import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import { register } from "../../services/authService";
import { useAuth } from "../../context/AuthContext";

export default function Register() {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleRegister = async (event: React.FormEvent) => {
        event.preventDefault();
        setError("");

        if (password !== confirmPassword) {
            setError("Les mots de passe ne correspondent pas.");
            return;
        }

        setLoading(true);

        try {
            await register({ username: username.trim(), password });

            const response = await api.post("/users/login/", {
                username: username.trim(),
                password,
            });

            const authorization = response.headers.authorization;
            if (!authorization) {
                setError("Compte créé, mais le token JWT est introuvable.");
                return;
            }

            login(authorization.replace("Bearer ", ""));
            navigate("/dashboard");
        } catch (err: any) {
            if (err.response?.status === 409) {
                setError("Ce nom d'utilisateur existe déjà.");
            } else if (err.response?.status === 400) {
                setError("Les informations saisies sont invalides.");
            } else {
                setError("Impossible de créer le compte.");
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h1>Créer un compte</h1>

            <form onSubmit={handleRegister}>
                <div>
                    <label htmlFor="username">Nom d'utilisateur</label>
                    <input
                        id="username"
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        disabled={loading}
                        placeholder="tanguy"
                    />
                </div>

                <div>
                    <label htmlFor="password">Mot de passe</label>
                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        disabled={loading}
                        placeholder="••••••"
                    />
                </div>

                <div>
                    <label htmlFor="confirmPassword">Confirmer le mot de passe</label>
                    <input
                        id="confirmPassword"
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        disabled={loading}
                        placeholder="••••••"
                    />
                </div>

                {error && <p className="error">{error}</p>}

                <button type="submit" disabled={loading}>
                    {loading ? "Création..." : "Créer mon compte"}
                </button>
            </form>

            <p className="auth-link">
                Déjà un compte ? <a href="/login">Se connecter</a>
            </p>
        </div>
    );
}
