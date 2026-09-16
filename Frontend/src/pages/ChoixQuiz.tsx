import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getQuizzes } from "../services/quizService";
import { startSalon } from "../services/salonService";
import type { Quiz } from "../types/quiz";
import SearchBar from "../components/ui/SearchBar";
import QuizOptionCard from "../components/quiz/QuizOptionCard";

export default function ChoixQuiz() {
    const { code } = useParams<{ code: string }>();
    const navigate = useNavigate();

    const [quizzes, setQuizzes] = useState<Quiz[]>([]);
    const [search, setSearch] = useState("");
    const [selectedQuizId, setSelectedQuizId] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [starting, setStarting] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        getQuizzes()
            .then(setQuizzes)
            .catch(() => setError("Impossible de récupérer les quiz."))
            .finally(() => setLoading(false));
    }, []);

    const filtered = quizzes.filter(q =>
        q.titre.toLowerCase().includes(search.toLowerCase()) ||
        q.description?.toLowerCase().includes(search.toLowerCase())
    );

    const handleStart = async () => {
        if (!code || selectedQuizId === null) return;
        setStarting(true); setError("");
        try { await startSalon(code, selectedQuizId); navigate(`/salon/${code}/game`); }
        catch { setError("Impossible de lancer la partie."); }
        finally { setStarting(false); }
    };

    if (loading) return <p>Chargement des quiz...</p>;

    return (
        <div className="choix-quiz">
            <h1>Choisir un quiz</h1>
            <p style={{ color: "var(--text-dim)" }}>Salon : <strong style={{ color: "var(--text-h)" }}>{code}</strong></p>

            {error && <p className="error">{error}</p>}

            <div style={{ margin: "16px 0" }}>
                <SearchBar value={search} onChange={setSearch} placeholder="Rechercher un quiz..." />
            </div>

            {filtered.length === 0 ? (
                <p style={{ color: "var(--text-dim)" }}>Aucun quiz ne correspond à votre recherche.</p>
            ) : (
                filtered.map(quiz => (
                    <QuizOptionCard
                        key={quiz.id}
                        quiz={quiz}
                        selected={selectedQuizId === quiz.id}
                        onSelect={() => setSelectedQuizId(quiz.id)}
                    />
                ))
            )}

            <button onClick={handleStart} disabled={selectedQuizId === null || starting} style={{ width: "100%", marginTop: 16 }}>
                {starting ? "Lancement..." : "Lancer la partie"}
            </button>
        </div>
    );
}
