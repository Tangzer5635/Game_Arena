import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import QuizCard from "../components/quiz/QuizCard";
import SearchBar from "../components/ui/SearchBar";
import { getQuizzes } from "../services/quizService";
import type { Quiz } from "../types/quiz";

export default function Quizzes() {
    const [quizzes, setQuizzes] = useState<Quiz[]>([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        getQuizzes()
            .then(setQuizzes)
            .catch(() => setError("Impossible de charger les quiz."))
            .finally(() => setLoading(false));
    }, []);

    const filtered = quizzes.filter(q =>
        q.titre.toLowerCase().includes(search.toLowerCase()) ||
        q.description?.toLowerCase().includes(search.toLowerCase())
    );

    if (loading) return <p>Chargement...</p>;
    if (error) return <p className="error">{error}</p>;

    return (
        <section>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12, marginBottom: 16 }}>
                <h1>Les quiz</h1>
                <Link to="/quizzes/create"><button>Créer un quiz</button></Link>
            </div>

            <SearchBar value={search} onChange={setSearch} placeholder="Rechercher par titre ou description..." />

            {filtered.length === 0 ? (
                <p style={{ marginTop: 20, color: "var(--text-dim)" }}>Aucun quiz ne correspond à votre recherche.</p>
            ) : (
                <div className="quiz-grid" style={{ marginTop: 16 }}>
                    {filtered.map(quiz => <QuizCard key={quiz.id} quiz={quiz} />)}
                </div>
            )}
        </section>
    );
}
